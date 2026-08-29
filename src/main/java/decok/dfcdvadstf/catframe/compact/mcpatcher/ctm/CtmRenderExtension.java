package decok.dfcdvadstf.catframe.compact.mcpatcher.ctm;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import decok.dfcdvadstf.catframe.model.render.IModelRenderExtension;
import decok.dfcdvadstf.catframe.model.render.api.RenderContext;
import decok.dfcdvadstf.catframe.model.render.api.RenderPhase;

/**
 * Render-extension entry point of the CTM compatibility layer.
 * <p>
 * Registered at {@link decok.dfcdvadstf.catframe.model.render.ModelRenderRegistry#DEFAULT_PRIORITY},
 * i.e. after all built-in extensions, so it sees the results of FaceCull/Tint.
 * P0 is a deliberate no-op: with an empty rule set the extension returns
 * immediately (zero overhead without a CTM pack). P2 adds tile selection here
 * and writes {@link RenderContext#iconOverride}.
 * <p>
 * Thread safety: this instance is stateless; all state lives in the immutable
 * {@link CtmRuleSet} snapshot swapped by {@link CtmManager}.
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
        // P2: resolve a rule for (block/tile, metadata, face, neighbors) and
        // write ctx.iconOverride. Intentionally a no-op until the selector lands.
    }
}
