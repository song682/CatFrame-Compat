package decok.dfcdvadstf.catframe.compact.mcpatcher;

import com.falsepattern.mcpatcher.internal.config.ModuleConfig;
import com.falsepattern.mcpatcher.internal.modules.common.Side;
import com.falsepattern.mcpatcher.internal.modules.ctm.CTMEngine;
import decok.dfcdvadstf.catframe.core.Direction;
import decok.dfcdvadstf.catframe.model.render.IModelRenderExtension;
import decok.dfcdvadstf.catframe.model.render.api.RenderContext;
import decok.dfcdvadstf.catframe.model.render.api.RenderPhase;
import net.minecraft.block.BlockPane;
import net.minecraft.util.IIcon;

/**
 * Render extension bridging RPMCP (Right Proper MCPatcher) CTM into the
 * CatFrame pipeline: for every world quad it asks RPMCP's {@link CTMEngine}
 * for a replacement icon and writes it into {@link RenderContext#iconOverride}.
 * <p>
 * Kept as a separate class from {@link CtmRenderExtension} for class-loading
 * isolation: this one references {@code com.falsepattern.mcpatcher} classes,
 * so it must never be loaded when RPMCP is absent. The extension is only
 * registered when {@code mcpatcher} is detected, and the per-quad check of
 * RPMCP's own {@link ModuleConfig#isConnectedTexturesEnabled()} mirrors the
 * guard used by RPMCP's RenderBlocks mixins.
 * <p>
 * Stateless: safe for parallel chunk compilation.
 */
public class RpmcpRenderExtension implements IModelRenderExtension {

    public static final RpmcpRenderExtension INSTANCE = new RpmcpRenderExtension();

    @Override
    public void apply(RenderContext ctx) {
        if (ctx.phase != RenderPhase.BLOCK_WORLD) {
            return;
        }
        // Respect RPMCP's own connected-textures toggle.
        if (!ModuleConfig.isConnectedTexturesEnabled()) {
            return;
        }
        // Direction-less quads (cross etc.) have no face to query.
        if (ctx.world == null || ctx.block == null || ctx.quad.face == null) {
            return;
        }
        IIcon base = ctx.quad.icon;
        if (base == null) {
            return;
        }
        // Pane: use PaneRenderHelper's connection logic (BlockPane.canPaneConnectToBlock)
        // instead of CTMEngine's generic neighbor query. Pane connection state is based
        // on blockstate (north/east/south/west properties), not world neighbor identity.
        if (ctx.block instanceof BlockPane) {
            Side paneSide = getPaneSide(ctx);
            if (paneSide == null) {
                return;
            }
            IIcon ctm = CTMEngine.getCTMIconMultiPass(ctx.world, ctx.block,
                    ctx.x, ctx.y, ctx.z, paneSide, base);
            if (ctm != null && ctm != base) {
                ctx.iconOverride = ctm;
            }
            return;
        }

        // Non-pane blocks: use generic CTMEngine neighbor query
        Side side = Side.fromMCDirection(ctx.quad.face.ordinal());
        if (side == null) {
            return;
        }
        IIcon ctm = CTMEngine.getCTMIconMultiPass(ctx.world, ctx.block,
                ctx.x, ctx.y, ctx.z, side, base);
        if (ctm != null && ctm != base) {
            ctx.iconOverride = ctm;
        }
    }

    /**
     * Get the CTM side for a pane quad based on its face and connection state.
     * Mirrors PaneRenderHelper's logic: pane's north/south faces use ZNeg/ZPos,
     * east/west faces use XNeg/XPos. Returns null if the face should not be
     * CTM-processed (UP/DOWN faces, or unconnected faces).
     */
    private static Side getPaneSide(RenderContext ctx) {
        BlockPane pane = (BlockPane) ctx.block;
        Direction face = ctx.quad.face;

        // Map quad face to connection check direction
        boolean connected;
        Side side;
        switch (face) {
            case NORTH:
                connected = pane.canPaneConnectToBlock(ctx.world.getBlock(ctx.x, ctx.y, ctx.z - 1));
                side = Side.ZNeg;
                break;
            case SOUTH:
                connected = pane.canPaneConnectToBlock(ctx.world.getBlock(ctx.x, ctx.y, ctx.z + 1));
                side = Side.ZPos;
                break;
            case WEST:
                connected = pane.canPaneConnectToBlock(ctx.world.getBlock(ctx.x - 1, ctx.y, ctx.z));
                side = Side.XNeg;
                break;
            case EAST:
                connected = pane.canPaneConnectToBlock(ctx.world.getBlock(ctx.x + 1, ctx.y, ctx.z));
                side = Side.XPos;
                break;
            default:
                // UP/DOWN: pane top/bottom faces are not CTM-processed
                return null;
        }

        // Only query CTM if the pane is connected in this direction
        // (unconnected faces show edge texture, not CTM variant)
        return connected ? side : null;
    }
}
