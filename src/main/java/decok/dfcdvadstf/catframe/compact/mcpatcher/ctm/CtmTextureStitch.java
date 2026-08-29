package decok.dfcdvadstf.catframe.compact.mcpatcher.ctm;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraftforge.client.event.TextureStitchEvent;

/**
 * Atlas stitch hook of the CTM compatibility layer.
 * <p>
 * Registered on {@code MinecraftForge.EVENT_BUS} by {@link CtmManager#init()}.
 * Only the block atlas (textureType 0) carries CTM tiles; the item atlas
 * (textureType 1) is left untouched.
 * <p>
 * Timing: the resource reload listener registered in {@link CtmManager}
 * (preInit) runs before {@code TextureMap} on every reload, so the rule set
 * is already populated when this Pre handler registers placeholders. On the
 * very first stitch of a session no CTM pack has been scanned yet (the first
 * reload happens before preInit); the rule set stays empty and the next
 * stitch (world entry / F3+T) picks everything up.
 */
@SideOnly(Side.CLIENT)
public final class CtmTextureStitch {

    @SubscribeEvent
    public void onTextureStitchPre(TextureStitchEvent.Pre event) {
        if (event.map.getTextureType() != 0) {
            return;
        }
        CtmTileRegistry.buildPlaceholders(CtmManager.getRuleSet());
    }

    @SubscribeEvent
    public void onTextureStitchPost(TextureStitchEvent.Post event) {
        if (event.map.getTextureType() != 0) {
            return;
        }
        CtmTileRegistry.collectIcons(event.map);
    }
}
