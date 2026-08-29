package decok.dfcdvadstf.catframe.compact.mcpatcher.ctm;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import decok.dfcdvadstf.catframe.core.Direction;
import decok.dfcdvadstf.catframe.model.render.IModelRenderExtension;
import decok.dfcdvadstf.catframe.model.render.api.RenderContext;
import decok.dfcdvadstf.catframe.model.render.api.RenderPhase;
import net.minecraft.util.IIcon;

/**
 * Render-extension entry point of the CTM compatibility layer.
 * <p>
 * Registered at {@link decok.dfcdvadstf.catframe.model.render.ModelRenderRegistry#DEFAULT_PRIORITY},
 * i.e. after all built-in extensions, so it sees the results of FaceCull/Tint.
 * For each block-world quad it resolves a rule from the current
 * {@link CtmRuleSet} snapshot and writes {@link RenderContext#iconOverride}
 * with the selected tile icon; with an empty rule set the extension returns
 * immediately (zero overhead without a CTM pack).
 * <p>
 * Thread safety: this instance is stateless; all state lives in the immutable
 * {@link CtmRuleSet} snapshot swapped by {@link CtmManager} and the
 * per-stitch {@link CtmTileRegistry} icon table, both read-only during
 * rendering.
 */
@SideOnly(Side.CLIENT)
public final class CtmRenderExtension implements IModelRenderExtension {

    @Override
    public void apply(RenderContext ctx) {
        // World rendering only; item/GUI/destroy phases keep vanilla textures.
        if (ctx.phase != RenderPhase.BLOCK_WORLD || ctx.world == null) {
            return;
        }
        if (CtmManager.getRuleSet().isEmpty()) {
            return;
        }
        Direction face = ctx.quad != null ? ctx.quad.face : null;
        if (face == null || ctx.block == null) {
            return;
        }
        String baseName = ctx.quad.icon != null ? ctx.quad.icon.getIconName() : null;
        IIcon tile = CtmTileSelector.select(ctx.world, ctx.x, ctx.y, ctx.z,
                ctx.block, ctx.metadata, face, baseName, ctx.quad);
        if (tile != null) {
            ctx.iconOverride = tile;
        }
    }
}
