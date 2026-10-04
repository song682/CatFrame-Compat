package decok.dfcdvadstf.catframe.language;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import decok.dfcdvadstf.catframe.CatFrameCompat;
import decok.dfcdvadstf.catframe.Tags;
import decok.dfcdvadstf.catframe.adapter.forge.language.LanguageRegister;
import io.qzz.dfdvdsf.jarfile.JarContents;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Language-file scanner for loaded mod containers: finds the JSON lang files
 * ({@code assets/<namespace>/lang/xx_xx.json}) inside every loaded mod's
 * container (jar in production, classes directory in dev) and forwards each of
 * them to the core CatFrame
 * {@link LanguageRegister#injectExternal(String, String, String, InputStream)}.
 * <p>
 * The core's {@code LanguageRegister.load()} only scans CatFrame's own
 * container, so every other mod's lang files — including this mod's, shipped
 * under the shared {@code assets/catframe} namespace — would never reach
 * Forge's {@code LanguageRegistry} on their own. Injection and its judgment
 * (lang-code conversion {@code en_us} → {@code en_US}, JSON parsing,
 * empty-file skipping) stay entirely in the core; this scanner only discovers
 * and forwards raw streams, which also makes the core re-inject them on later
 * resource-manager reloads with resource pack overrides on top.
 * <p>
 * CatFrame's own container is skipped — {@code LanguageRegister.load()} already
 * registers it, and re-forwarding would only duplicate its reload bookkeeping.
 * Containers are deduplicated by canonical path, since several mod IDs can
 * share one jar.
 */
public final class LanguageScanner {

    private static final String ASSETS_PREFIX = "assets/";
    private static final String JSON_SUFFIX = ".json";
    private static final String LANG_DIR = "lang";

    /** modid of the core CatFrame library: its container self-loads and is skipped. */
    private static final String CORE_MODID = Tags.MODID;

    private LanguageScanner() {
    }

    /**
     * Scans every loaded mod's container for {@code assets/<ns>/lang/*.json}
     * entries and forwards each one to
     * {@link LanguageRegister#injectExternal}. Intended to run once, from
     * pre-init, after the core library's own language pass.
     */
    public static void scan() {
        ModContainer coreContainer = Loader.instance().getIndexedModList().get(CORE_MODID);
        File coreSource = (coreContainer != null) ? coreContainer.getSource() : null;
        String coreKey = (coreSource != null) ? canonicalPath(coreSource) : null;

        List<File> containers = new ArrayList<File>();
        Set<String> seen = new HashSet<String>();
        for (ModContainer container : Loader.instance().getActiveModList()) {
            File source = container.getSource();
            if (source == null) continue;
            String key = canonicalPath(source);
            // The core library already loads its own container via LanguageRegister.load().
            if (key.equals(coreKey)) continue;
            // Several mod IDs may share one jar; scan each container once.
            if (!seen.add(key)) continue;
            containers.add(source);
        }

        int forwarded = 0;
        for (File container : containers) {
            List<String> entries = JarContents.listEntryNames(container, ASSETS_PREFIX, JSON_SUFFIX);
            for (String entry : entries) {
                // Accept only the flat assets/<namespace>/lang/<file>.json layout.
                String[] parts = entry.split("/");
                if (parts.length != 4 || !LANG_DIR.equals(parts[2]) || parts[1].isEmpty()) continue;

                try (InputStream in = JarContents.openEntry(container, entry)) {
                    if (in == null) continue;
                    LanguageRegister.injectExternal(parts[1], LANG_DIR, parts[3], in);
                    forwarded++;
                } catch (IOException e) {
                    CatFrameCompat.logger.error("LanguageScanner: failed to read '{}' in '{}'", entry, container, e);
                }
            }
        }

        CatFrameCompat.logger.info("LanguageScanner: forwarded {} lang JSON file(s) from {} mod container(s)",
                forwarded, containers.size());
    }

    /**
     * Canonical path used to deduplicate containers; falls back to the absolute
     * path when the canonical form cannot be resolved.
     */
    private static String canonicalPath(File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            return file.getAbsolutePath();
        }
    }
}
