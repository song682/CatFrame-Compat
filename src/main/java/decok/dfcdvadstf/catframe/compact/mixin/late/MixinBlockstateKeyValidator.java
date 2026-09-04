package decok.dfcdvadstf.catframe.compact.mixin.late;

import decok.dfcdvadstf.catframe.model.state.BlockstateJson;
import decok.dfcdvadstf.catframe.model.state.BlockstateKeyValidator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Bypasses CatFrame core's blockstate rotation angle restriction, allowing
 * mod developers to use arbitrary integer degree values (not just 0/90/180/270)
 * in blockstate JSON {@code x} and {@code y} fields.
 *
 * <p><b>Background</b>: CatFrame core's {@link BlockstateKeyValidator#validateRotations}
 * enforces vanilla 1.8+ semantics — only multiples of 90° are accepted; any other
 * angle causes the variant to be replaced with {@code builtin/missing} (purple-black
 * MissingNo). However, the underlying baking pipeline
 * ({@code JsonModelBake.applyXRotation} / {@code applyYRotation}) uses
 * {@code Math.toRadians()} + matrix transforms that already support arbitrary
 * angles correctly. The validator is an artificial restriction, not a technical
 * limitation.</p>
 *
 * <p>This Mixin cancels {@code validateRotations} at HEAD, turning it into a
 * no-op so that non-90° rotations pass through without being replaced by
 * MissingNo. The property-key validation in {@link BlockstateKeyValidator#validate}
 * is unaffected (it runs in a separate method).</p>
 *
 * <p>The injection target is a CatFrame class (not obfuscated), hence
 * {@code remap = false}.</p>
 */
@Mixin(value = BlockstateKeyValidator.class, remap = false)
public abstract class MixinBlockstateKeyValidator {

    /**
     * Skip the rotation angle validation entirely.
     *
     * <p>Without this, a blockstate variant with e.g. {@code "y": 45} would be
     * silently replaced by {@code builtin/missing}, showing the purple-black
     * MissingNo cube instead of the intended rotated model.</p>
     *
     * @param bs    the loaded blockstate JSON (unused — cancel before any work)
     * @param owner human-readable owner for log context (unused)
     * @param ci    callback — cancelled to skip validation
     */
    @Inject(method = "validateRotations", at = @At("HEAD"), cancellable = true,
            remap = false)
    private static void catframecompact$skipRotationValidation(
            BlockstateJson bs, String owner, CallbackInfo ci) {
        ci.cancel();
    }
}
