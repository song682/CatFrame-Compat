package decok.dfcdvadstf.catframe.compact;

import decok.dfcdvadstf.catframe.mixin.late.MixinRenderJsonItemModel;
import io.qzz.dfdvdsf.jarfile.JarContents;
import io.qzz.dfdvdsf.jarfile.JarNames;
import io.qzz.dfdvdsf.jarfile.JarVersionGuesser;
import io.qzz.dfdvdsf.jarfile.ModVersions;
import net.minecraft.entity.item.EntityItem;

import java.io.File;
import java.lang.reflect.Method;

/**
 * ItemPhysic compatibility utility.
 * <p>
 * ItemPhysic has two mutually incompatible distributions that share the modid
 * {@code itemphysic}, so the modid cannot tell them apart — the installed
 * variant must be decided by jar content:
 * <ul>
 *   <li><b>Official edition</b> (CreativeMD, ASM coremod): wholesale-replaces
 *       {@code RenderItem.doRender} and is mutually exclusive with CatFrame's
 *       Forge IItemRenderer takeover; CatFrame refuses to start once detected.</li>
 *   <li><b>Mixin edition</b> (kotmatross, depends on UniMixins): injects vanilla
 *       methods at fine granularity and actively yields to third-party
 *       IItemRenderer, so it can coexist; its drop-flip animation is driven by
 *       {@code ClientPhysic.applyRotations}, invoked here via reflection
 *       (zero compile-time dependency).</li>
 * </ul>
 *
 * <p>Variant detection reuses jar-utils' jar content scanning
 * ({@link JarContents#findClassEntries}): a pure filesystem operation that
 * does not depend on classloader state; the official-only class
 * {@code ItemPatchingLoader} and the Mixin-only class {@code ClientPhysic}
 * serve as distinguishing anchors. Compatibility policy: the official edition
 * crashes with a removal hint; the Mixin edition is allowed and its rotation
 * physics are recreated in the CatFrame renderer
 * ({@link MixinRenderJsonItemModel}).</p>
 */
public class ItemPhysic {

    /** Official edition (CreativeMD ASM coremod) only class: the doRender wholesale-replacement patch entry. */
    private static final String OFFICIAL_CORE_LOADER = "com.creativemd.itemphysic.ItemPatchingLoader";

    /** Mixin edition only class: rotation/fluid/web-slowdown logic (1.3.1 kotmatross edition). */
    private static final String MIXIN_CLIENT_PHYSIC = "com.creativemd.itemphysic.physics.ClientPhysic";

    /**
     * Minimum version of the Mixin edition: the official edition never shipped
     * a release ≥ this version (its version line stops lower), so a Forge-level
     * version ≥ 1.2.6 confirms the Mixin edition without scanning jar content.
     */
    private static final String MIXIN_MIN_VERSION = "1.2.6";

    /** Cached reflection handle, avoiding a reflection lookup every frame. */
    private static Method applyRotationsMethod;

    // === === === Scan results (scanned once, all verdicts cached) === === ===

    private static boolean scanned;
    private static boolean official;
    private static boolean mixin;
    /** File name of the hit itemphysic jar (no extension; for logging and version hints). */
    private static String detectedJarName;

    /**
     * Compatibility layer master switch (set via {@link #setEnabled} after the
     * main class reads the config): turning it off disables all detection and
     * injection, effectively ignoring ItemPhysic entirely. The initial value
     * follows the actual installation status of itemphysic (off by default).
     */
    private static boolean enabled = CompactBase.isItemPhysicInstalled();

    private ItemPhysic() {}

    /**
     * Enables or disables the whole ItemPhysic compatibility layer (config switch).
     * It takes effect only when both the config switch and the itemphysic
     * installation status are satisfied: even with the config allowing it, the
     * layer stays disabled when itemphysic is not installed. When disabled,
     * {@link #isInstalled()} / {@link #isOfficialInstalled()} /
     * {@link #isMixinInstalled()} all return {@code false}, and neither the
     * crash rejection nor the rotation injection takes effect.
     *
     * @param configEnabled {@code true} to enable (default); {@code false} to bypass entirely
     */
    public static void setEnabled(boolean configEnabled) {
        enabled = configEnabled && CompactBase.isItemPhysicInstalled();
    }

    /**
     * Scans the jars in the mods directory and decides the itemphysic variant
     * by content.
     * <p>Detection is layered in two tiers:</p>
     * <ul>
     *   <li><b>Tier 1 (Forge level)</b>: reads the loaded mod's version via
     *       {@link ModVersions#versionMatches}; ≥ {@value #MIXIN_MIN_VERSION}
     *       confirms the Mixin edition — the official edition never released
     *       that high, no jar scan needed.</li>
     *   <li><b>Tier 2 (jar content)</b>: when the version is below
     *       {@value #MIXIN_MIN_VERSION} or unreadable, falls back to class-entry
     *       scanning: the official edition contains {@code ItemPatchingLoader},
     *       the Mixin edition contains {@code physics/ClientPhysic}.</li>
     * </ul>
     * Scanned only once; repeated calls have no side effects. A missing or
     * unreadable directory is silently skipped (treated as not installed).
     *
     * @param modsDir the mods directory (derivable from the preInit event's config directory)
     */
    public static void scan(File modsDir) {
        if (scanned) return;
        scanned = true;
        if (!enabled) return;

        // Tier 1: Forge-level version check — a version this high can only be
        // the Mixin rewrite, since the official line never reached it.
        if (ModVersions.versionMatches("ItemPhysic", "itemphysic", ">=" + MIXIN_MIN_VERSION)) {
            mixin = true;
            return;
        }

        // Tier 2: jar content scan, only when the version is unknown or below the Mixin floor.
        if (modsDir == null || !modsDir.isDirectory()) return;

        File[] files = modsDir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (!JarNames.isJarFile(file)) continue;
            boolean hasOfficial = !JarContents.findClassEntries(file, OFFICIAL_CORE_LOADER).isEmpty();
            boolean hasMixin = !JarContents.findClassEntries(file, MIXIN_CLIENT_PHYSIC).isEmpty();
            if (hasOfficial || hasMixin) {
                official |= hasOfficial;
                mixin |= hasMixin;
                if (detectedJarName == null) {
                    // Record the first hit jar (file-name version guess is for logging only)
                    JarVersionGuesser.Guess guess = JarVersionGuesser.guess(file);
                    detectedJarName = guess.name() + (guess.hasVersion() ? "-" + guess.version() : "");
                }
            }
        }
    }

    /**
     * Whether any ItemPhysic variant was detected (jar-content verdict from {@link #scan(File)}).
     */
    public static boolean isInstalled() {
        return enabled && (official || mixin);
    }

    /**
     * Whether the official edition (ASM coremod) is installed.
     * <p>The official edition is mutually exclusive with CatFrame's drop-item
     * takeover (it wholesale-replaces doRender and unconditionally
     * short-circuits ForgeHooksClient), so startup must be rejected.</p>
     */
    public static boolean isOfficialInstalled() {
        return enabled && official;
    }

    /**
     * Whether the Mixin edition (kotmatross) is installed.
     * <p>This edition injects vanilla rendering at fine granularity and
     * actively yields to third-party IItemRenderer, so it is structurally
     * compatible with CatFrame.</p>
     */
    public static boolean isMixinInstalled() {
        return enabled && mixin;
    }

    /**
     * Name of the hit itemphysic jar (file-name version guess; for logging only).
     */
    public static String detectedJarName() {
        return detectedJarName;
    }

    /**
     * Invokes the Mixin edition's {@code ClientPhysic.applyRotations(EntityItem)}
     * via reflection to update the drop item's {@code rotationPitch}
     * (falling flip / landing reset / fluid and web slowdown).
     * <p>Zero compile-time dependency on ItemPhysic: when the Mixin edition is
     * not installed, the class is missing, or the method signature changed,
     * it degrades silently (exceptions dropped) without affecting CatFrame's
     * own rendering.</p>
     *
     * @param item the drop-item entity being rendered
     */
    public static void applyRotations(EntityItem item) {
        try {
            if (applyRotationsMethod == null) {
                Class<?> clazz = Class.forName(MIXIN_CLIENT_PHYSIC);
                applyRotationsMethod = clazz.getMethod("applyRotations", EntityItem.class);
            }
            applyRotationsMethod.invoke(null, item);
        } catch (Throwable ignored) {
            // Mixin edition not installed or API changed: degrade silently.
        }
    }
}
