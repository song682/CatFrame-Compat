package decok.dfcdvadstf.catframe.compact.mcpatcher;

import com.falsepattern.mcpatcher.internal.config.ModuleConfig;
import com.falsepattern.mcpatcher.internal.modules.common.Side;
import com.falsepattern.mcpatcher.internal.modules.ctm.CTMEngine;
import decok.dfcdvadstf.catframe.model.render.IModelRenderExtension;
import decok.dfcdvadstf.catframe.model.render.api.RenderContext;
import decok.dfcdvadstf.catframe.model.render.api.RenderPhase;
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
}
