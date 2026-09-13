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
        // Pane: fully mirror PaneRenderHelper's CTM logic.
        // PaneRenderHelper uses block.getIcon(0, meta) as base icon for all 4 horizontal faces,
        // then queries CTMEngine for each side (XNeg/XPos/ZNeg/ZPos).
        // We replicate this: get the correct base icon, map quad.face to Side, query CTMEngine.
        if (ctx.block instanceof BlockPane) {
            Side paneSide = getPaneSide(ctx.quad.face);
            if (paneSide == null) {
                return; // UP/DOWN: skip CTM
            }
            // Use block's side-0 icon as base (matches PaneRenderHelper.updateTextures L150)
            IIcon paneBase = ctx.block.getIcon(0, ctx.metadata);
            if (paneBase == null) {
                return;
            }
            IIcon ctm = CTMEngine.getCTMIconMultiPass(ctx.world, ctx.block,
                    ctx.x, ctx.y, ctx.z, paneSide, paneBase);
            if (ctm != null) {
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
     * Map pane quad face to CTMEngine Side.
     * Mirrors PaneRenderHelper: NORTH->ZNeg, SOUTH->ZPos, WEST->XNeg, EAST->XPos.
     * Returns null for UP/DOWN (pane top/bottom faces are not CTM-processed).
     */
    private static Side getPaneSide(Direction face) {
        switch (face) {
            case NORTH: return Side.ZNeg;
            case SOUTH: return Side.ZPos;
            case WEST:  return Side.XNeg;
            case EAST:  return Side.XPos;
            default:    return null; // UP/DOWN
        }
    }
}
