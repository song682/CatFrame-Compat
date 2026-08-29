package decok.dfcdvadstf.catframe.mixin;

import decok.dfcdvadstf.catframe.compact.mcpatcher.ctm.CtmTileRegistry;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Redirects atlas resource resolution for CTM placeholder icons.
 * <p>
 * {@link CtmTileRegistry} registers every CTM tile on the block atlas under a
 * synthetic key ({@code catframe_ctm/r<rule>/<tile>}). During
 * {@code TextureMap.loadTextureAtlas} the key would be resolved to a
 * nonexistent {@code blocks/catframe_ctm/...} texture; this redirect
 * intercepts the {@code completeResourceLocation} call (both the main and the
 * mipmap call sites) and substitutes the real pack texture path when the key
 * is a registered placeholder. Non-placeholder keys fall through to the
 * original resolution.
 * <p>
 * The redirect handler is a mixin on the vanilla {@link TextureMap} (an
 * obfuscated class in releases), hence {@code remap} defaults to true and the
 * reference map maps the MCP names to their SRG counterparts.
 */
@Mixin(TextureMap.class)
public abstract class MixinTextureMap {

    /** Original resource-path resolution, used as the fallback for non-placeholder keys. */
    @Shadow
    private ResourceLocation completeResourceLocation(ResourceLocation loc, int level) {
        return null;
    }

    /**
     * @param map   the TextureMap instance being loaded (the receiver of the redirected call)
     * @param loc   the registered icon key, e.g. {@code catframe_ctm/r0/3}
     * @param level mipmap level ({@code 0} = main texture, &gt;0 = mipmap)
     * @return the real CTM texture location for placeholders, otherwise the original resolution
     */
    @Redirect(
            method = "loadTextureAtlas",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/texture/TextureMap;completeResourceLocation(Lnet/minecraft/util/ResourceLocation;I)Lnet/minecraft/util/ResourceLocation;"))
    private ResourceLocation catframecompact$ctmCompleteResourceLocation(TextureMap map, ResourceLocation loc, int level) {
        if (level == 0) {
            String packPath = CtmTileRegistry.resolvePackPath(loc.getResourcePath());
            if (packPath != null) {
                return new ResourceLocation("minecraft", packPath);
            }
        }
        return this.completeResourceLocation(loc, level);
    }
}
