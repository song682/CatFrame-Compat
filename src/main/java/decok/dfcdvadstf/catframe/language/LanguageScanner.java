package decok.dfcdvadstf.catframe.language;

import decok.dfcdvadstf.catframe.CatFrameCompat;
import decok.dfcdvadstf.catframe.adapter.forge.language.LanguageRegister;
import io.qzz.dfdvdsf.jarinjar.JarLocator;
import io.qzz.dfdvdsf.jarfile.JarContents;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Language-file scanner for this mod's own container: finds the JSON lang
 * files ({@code assets/<namespace>/lang/xx_xx.json}) inside the jar (or class
 * output directory in dev) this class was loaded from, and forwards each of
 * them to the core CatFrame
 * {@link LanguageRegister#injectExternal(String, String, String, InputStream)}.
 * <p>
 * The core's {@code LanguageRegister.load()} only scans CatFrame's own
 * container, so this mod's lang files — shipped under the shared
 * {@code assets/catframe} namespace — would never reach Forge's
 * {@code LanguageRegistry} on their own. Injection and its judgment (lang-code
 * conversion {@code en_us} → {@code en_US}, JSON parsing, empty-file skipping)
 * stay entirely in the core; this scanner only discovers and forwards raw
 * streams, which also makes the core re-inject them on later resource-manager
 * reloads with resource pack overrides on top.
 */
public final class LanguageScanner {

    private static final String ASSETS_PREFIX = "assets/";
    private static final String JSON_SUFFIX = ".json";
    private static final String LANG_DIR = "lang";

    private LanguageScanner() {
    }

    /**
     * Scans this mod's own container for {@code assets/<ns>/lang/*.json}
     * entries and forwards each one to
     * {@link LanguageRegister#injectExternal}.
     */
    public static void scan() {
        File container = JarLocator.getContainingFile(LanguageScanner.class);
        if (container == null) {
            CatFrameCompat.logger.warn("LanguageScanner: cannot locate the mod container, skipping lang scan");
            return;
        }

        int forwarded = 0;
        List<String> entries = JarContents.listEntryNames(container, ASSETS_PREFIX, JSON_SUFFIX);
        for (String entry : entries) {
            // Accept only the flat assets/<namespace>/lang/<file>.json layout.
            String[] parts = entry.split("/");
            if (parts.length != 4 || !LANG_DIR.equals(parts[2]) || parts[1].isEmpty()) continue;

            String namespace = parts[1];
            String fileName = parts[3];
            try (InputStream in = JarContents.openEntry(container, entry)) {
                if (in == null) continue;
                LanguageRegister.injectExternal(namespace, LANG_DIR, fileName, in);
                forwarded++;
            } catch (IOException e) {
                CatFrameCompat.logger.error("LanguageScanner: failed to read '{}'", entry, e);
            }
        }

        CatFrameCompat.logger.info("LanguageScanner: forwarded {} lang JSON file(s) from '{}'",
                forwarded, container);
    }
}
