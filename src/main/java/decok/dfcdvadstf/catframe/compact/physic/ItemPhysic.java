package decok.dfcdvadstf.catframe.compact.physic;

import com.creativemd.itemphysic.physics.ClientPhysic;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.versioning.DefaultArtifactVersion;
import decok.dfcdvadstf.catframe.compact.CompactBase;
import decok.dfcdvadstf.catframe.compact.mixin.late.MixinItemPhysics;
import io.qzz.dfdvdsf.jarfile.JarContents;
import io.qzz.dfdvdsf.jarfile.JarNames;
import io.qzz.dfdvdsf.jarfile.JarVersionGuesser;
import net.minecraft.entity.item.EntityItem;

import java.io.File;

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
 *       {@code ClientPhysic.applyRotations}, linked and called directly here.</li>
 * </ul>
 *
 * <p><b>Linking, not vendoring</b>: the Mixin edition is GPLv3, so this layer
 * copies none of its source — it only calls the class the installed jar puts on
 * the classpath (a {@code compileOnly} dependency), which leaves this mod's own
 * sources and distribution under MIT. The upstream API is effectively frozen,
 * so the call is compile-time verified instead of reflective: an upstream
 * signature change fails the build rather than degrading silently at runtime.</p>
 *
 * <p>Variant detection reuses jar-utils' jar content scanning
 * ({@link JarContents#findClassEntries}): a pure filesystem operation that
 * does not depend on classloader state; the official-only class
 * {@code ItemPatchingLoader} and the Mixin-only class {@code ClientPhysic}
 * serve as distinguishing anchors. Compatibility policy: the official edition
 * crashes with a removal hint; the Mixin edition is allowed — its physics are
 * called through {@link #applyRotations} and its render-side rotation chain is
 * recreated in the CatFrame renderer ({@link MixinItemPhysics}).</p>
 */
public class ItemPhysic {

    /** Official edition (CreativeMD ASM coremod) only class: the doRender wholesale-replacement patch entry. */
    private static final String OFFICIAL_CORE_LOADER = "com.creativemd.itemphysic.ItemPatchingLoader";

    /**
     * Mixin edition only class: rotation/fluid/web-slowdown logic (1.3.1
     * kotmatross edition). Serves twice — as the artifact {@link #scan(File)}
     * keys on, and as the class {@link #applyRotations} links against.
     */
    private static final String MIXIN_CLIENT_PHYSIC = "com.creativemd.itemphysic.physics.ClientPhysic";

    /**
     * Minimum version of the Mixin edition: the official edition never shipped
     * a release ≥ this version (its version line stops lower), so a Forge-level
     * version ≥ 1.2.6 confirms the Mixin edition without scanning jar content.
     */
    private static final String MIXIN_MIN_VERSION = "1.2.6";

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
     * {@link #isOfficialInstalled()} /
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
     * <p>Detection is layered in two tiers; when they disagree, the Forge-level
     * verdict (Tier 1) prevails — a jar scan must never override it:</p>
     * <ul>
     *   <li><b>Tier 1 (Forge level, authoritative on conflict)</b>: compares the
     *       loaded mod's version with Forge's own versioning machinery —
     *       {@code Loader#getIndexedModList().get(id).getProcessedVersion()}
     *       against a {@link DefaultArtifactVersion}; ≥ {@value #MIXIN_MIN_VERSION}
     *       confirms the Mixin edition — the official edition never released
     *       that high, no jar scan needed. A verdict reached here is final:
     *       Tier 2 is skipped entirely.</li>
     *   <li><b>Tier 2 (jar content, fallback only)</b>: runs only when Tier 1
     *       yields no verdict (itemphysic not loaded, or its version below
     *       {@value #MIXIN_MIN_VERSION}); falls back to class-entry scanning:
     *       the official edition contains {@code ItemPatchingLoader}, the Mixin
     *       edition contains {@code physics/ClientPhysic}.</li>
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

        // Tier 1: Forge-level version check — a version at or above the Mixin
        // floor can only be the Mixin rewrite, since the official line never
        // reached it. Uses Forge's own ArtifactVersion ordering, no re-implemented
        // version parsing.
        // Authoritative on conflict: a verdict reached here is final — return
        // immediately, so the jar scan below can never override it.
        ModContainer container = Loader.instance().getIndexedModList().get("itemphysic");
        if (container != null
                && container.getProcessedVersion().compareTo(new DefaultArtifactVersion(MIXIN_MIN_VERSION)) >= 0) {
            mixin = true;
            return;
        }

        // Tier 2: jar content scan — fallback only, reached when Tier 1 yields
        // no verdict (itemphysic not loaded, or its version below the Mixin floor).
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
     * Applies the Mixin edition's {@code ClientPhysic.applyRotations(EntityItem)}
     * to update the drop item's {@code rotationPitch} (falling flip / landing
     * reset / fluid and web slowdown).
     *
     * <p>Called directly against the {@code compileOnly} 1.3.1 API — no
     * reflection — so the call is verified by the compiler. The linkage
     * invariant that makes this safe: {@value #MIXIN_CLIENT_PHYSIC} is both the
     * artifact scanned in {@link #scan(File)} and the class linked here, and
     * every caller gates on {@link #isMixinInstalled()} first, so the class is
     * guaranteed to be on the classpath whenever this method runs. Should that
     * invariant ever break, it now surfaces as a {@code NoClassDefFoundError}
     * instead of being swallowed — fail loudly, never degrade silently.</p>
     *
     * <p>调用方必须先确认 {@link #isMixinInstalled()}：本方法直接链接上游类，
     * 未安装 Mixin 版就调用会抛出 {@code NoClassDefFoundError}，不再静默降级。</p>
     *
     * @param item the drop-item entity being rendered
     */
    public static void applyRotations(EntityItem item) {
        ClientPhysic.applyRotations(item);
    }
}
