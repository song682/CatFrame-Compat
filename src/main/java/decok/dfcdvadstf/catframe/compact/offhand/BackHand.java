package decok.dfcdvadstf.catframe.compact.offhand;

import decok.dfcdvadstf.catframe.compact.CompactBase;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import xonin.backhand.api.core.BackhandUtils;

import javax.annotation.Nullable;

/**
 * Backhand compatibility utility.
 *
 * <p>Backhand (GTNH edition, 1.8.x line) renders the offhand item by replaying the
 * vanilla first/third-person hand pipeline under a mirrored GL state
 * ({@code glScalef(-1,1,1)} plus a cull-face flip), so every Forge
 * {@code IItemRenderer} — CatFrame's {@code RenderJsonItemModel} included — is
 * invoked exactly as for the main hand. The display-transform side of that
 * takeover is rebuilt by {@link BackhandDisplayExtension} (see its class comment
 * for the geometry); this utility only carries the layer switch and the
 * render-pass detection that does not need client-only classes.</p>
 *
 * <p><b>Linking, not vendoring</b> (same policy as the ItemPhysic layer): the
 * Backhand API is a {@code compileOnly} dependency, linked directly —
 * {@link BackhandUtils#getOffhandItem} / {@link BackhandUtils#getOffhandSlot} —
 * so an upstream signature change fails the build instead of degrading
 * silently. Every caller gates on {@link #isEnabled()} first, so the linked
 * classes are guaranteed to be on the classpath whenever the calls run; the
 * extension itself is only registered when Backhand is installed, so the whole
 * chain stays dark without it.</p>
 */
public final class BackHand {

    /**
     * Layer master switch: config switch AND installation status. Set from the
     * common proxy preInit after the config is read; the initial value follows
     * the installation status (off without Backhand).
     */
    private static boolean enabled = CompactBase.isBackhandInstalled();

    private BackHand() {}

    /**
     * Enables or disables the whole Backhand compatibility layer (config switch).
     * When disabled, the left-hand display bridge is never registered and every
     * detection below returns {@code false}: the offhand then renders with the
     * right-hand display transforms, exactly the pre-bridge behavior.
     *
     * @param configEnabled {@code true} to enable (default); {@code false} to bypass entirely
     */
    public static void setEnabled(boolean configEnabled) {
        enabled = configEnabled && CompactBase.isBackhandInstalled();
    }

    /**
     * Whether the Backhand compatibility layer is active
     * (installed AND enabled by config).
     */
    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Whether the given third-person hand render is Backhand's offhand pass.
     *
     * <p>Backhand's third-person pass ({@code BackhandRenderHelper
     * .renderOffhandItemIn3rdPerson}) anchors on the left arm and hands the
     * <b>exact offhand stack reference</b> straight to
     * {@code ItemRenderer.renderItem(entity, stack, pass)}, with no slot swap
     * around it — so identity against the holder's offhand stack is the
     * reliable marker. Works for the local player (F5) and for other players
     * in multiplayer alike, including while the offhand item is actively being
     * used (the left-arm pass is unconditional).</p>
     *
     * <p>调用方必须先确认 {@link #isEnabled()}；未安装 Backhand 时本方法不会被调用
     * （本层整体未注册）。</p>
     *
     * @param stack  the stack being rendered (from {@code RenderContext.stack})
     * @param holder the attached entity (from {@code RenderJsonItemModel.getCurrentHolderEntity()})
     * @return {@code true} when this render is the offhand item on its holder's left arm
     */
    public static boolean isOffhandThirdPersonPass(@Nullable ItemStack stack, @Nullable EntityLivingBase holder) {
        if (!enabled || stack == null) return false;
        if (!(holder instanceof EntityPlayer)) return false;
        return stack == BackhandUtils.getOffhandItem((EntityPlayer) holder);
    }
}
