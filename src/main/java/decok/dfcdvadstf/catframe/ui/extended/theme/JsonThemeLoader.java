package decok.dfcdvadstf.catframe.ui.extended.theme;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * <p>
 * Loads {@link Theme} definitions from JSON resource files located at
 * {@code assets/<namespace>/themes/<id>.json} on the classpath.<br>
 * Supports both exploded directories (development) and JAR entries (production).
 * </p>
 * <p>
 * 从 classpath 上 {@code assets/<namespace>/themes/<id>.json} 位置的 JSON 资源文件
 * 加载 {@link Theme} 定义。同时支持展开目录（开发环境）和 JAR 条目（生产环境）。
 * </p>
 *
 * <h3>Discovery / 发现</h3>
 * <p>
 * The loader scans all classpath entries for {@code assets/} directories, then
 * looks for {@code <namespace>/themes/*.json} within them. In a dev environment
 * this picks up {@code build/resources/main/}; in production it reads from JARs.
 * </p>
 */
@SideOnly(Side.CLIENT)
public final class JsonThemeLoader {

    private static final Logger LOGGER = LogManager.getLogger("CatFrameCompact|ThemeLoader");
    private static final Gson GSON = new GsonBuilder().create();
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private JsonThemeLoader() {
    }

    /**
     * Scan the classpath for theme JSON files, parse them, and register the
     * discovered themes with {@link ThemeManager}.
     * <p>扫描 classpath 以查找主题 JSON 文件，解析它们，并将发现的主题注册到
     * {@link ThemeManager}。</p>
     */
    public static void loadThemes() {
        ThemeManager manager = ThemeManager.getInstance();
        List<ThemeDefinition> definitions = scanAndParse();

        if (definitions.isEmpty()) {
            LOGGER.info("No JSON themes found on classpath");
            return;
        }

        // Phase 1: create JsonTheme instances (without fallback links)
        // 阶段 1：创建 JsonTheme 实例（不含回退链接）
        Map<String, ThemeDefinition.JsonTheme> themeMap = new LinkedHashMap<>();
        for (ThemeDefinition def : definitions) {
            if (def.id == null || def.id.isEmpty()) {
                LOGGER.warn("Skipping theme with missing id");
                continue;
            }
            // Create with null fallback for now; resolve in phase 2
            ThemeDefinition.JsonTheme theme = new ThemeDefinition.JsonTheme(def, null);
            themeMap.put(def.id, theme);
        }

        // Phase 2: resolve fallback links
        // 阶段 2：解析回退链接
        for (Map.Entry<String, ThemeDefinition.JsonTheme> entry : themeMap.entrySet()) {
            ThemeDefinition def = findDefinition(definitions, entry.getKey());
            if (def != null && def.fallback != null) {
                Theme fallback = themeMap.get(def.fallback);
                if (fallback == null) {
                    fallback = DefaultTheme.INSTANCE;
                    LOGGER.warn("Theme '{}': fallback '{}' not found, using default",
                            def.id, def.fallback);
                }
                // Re-create with the resolved fallback
                ThemeDefinition.JsonTheme withFallback =
                        new ThemeDefinition.JsonTheme(def, fallback);
                themeMap.put(entry.getKey(), withFallback);
            }
        }

        // Phase 3: register with ThemeManager
        // 阶段 3：注册到 ThemeManager
        for (Map.Entry<String, ThemeDefinition.JsonTheme> entry : themeMap.entrySet()) {
            manager.register(entry.getKey(), entry.getValue());
        }

        LOGGER.info("Loaded {} JSON theme(s)", themeMap.size());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Classpath scanning
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Scan all classpath entries for theme JSON files and parse them.
     * <p>扫描所有 classpath 条目以查找主题 JSON 文件并解析。</p>
     */
    private static List<ThemeDefinition> scanAndParse() {
        List<ThemeDefinition> results = new ArrayList<>();
        ClassLoader cl = JsonThemeLoader.class.getClassLoader();

        try {
            // Find all 'assets/' directories on the classpath
            Enumeration<URL> assetsUrls = cl.getResources("assets/");
            while (assetsUrls.hasMoreElements()) {
                URL assetsUrl = assetsUrls.nextElement();
                scanAssetsDirectory(assetsUrl, results);
            }
        } catch (Exception e) {
            LOGGER.error("Error scanning classpath for themes", e);
        }

        return results;
    }

    /**
     * Scan an {@code assets/} directory for {@code <namespace>/themes/*.json} files.
     * <p>扫描 {@code assets/} 目录以查找 {@code <namespace>/themes/*.json} 文件。</p>
     */
    private static void scanAssetsDirectory(URL assetsUrl, List<ThemeDefinition> results) {
        try {
            URLConnection conn = assetsUrl.openConnection();

            if (conn instanceof JarURLConnection) {
                // JAR entry: iterate JAR entries looking for themes/*.json
                // JAR 条目：遍历 JAR 条目查找 themes/*.json
                scanJar(((JarURLConnection) conn).getJarFile(), results);
            } else {
                // File system directory (dev environment)
                // 文件系统目录（开发环境）
                File assetsDir = new File(assetsUrl.toURI());
                if (assetsDir.isDirectory()) {
                    scanFilesystem(assetsDir, results);
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Could not open assets URL: {}", assetsUrl, e);
        }
    }

    /**
     * Scan a filesystem {@code assets/} directory.
     * <p>扫描文件系统中的 {@code assets/} 目录。</p>
     */
    private static void scanFilesystem(File assetsDir, List<ThemeDefinition> results) {
        File[] namespaces = assetsDir.listFiles();
        if (namespaces == null) {
            return;
        }
        for (File nsDir : namespaces) {
            if (!nsDir.isDirectory()) {
                continue;
            }
            File themesDir = new File(nsDir, "themes");
            if (!themesDir.isDirectory()) {
                continue;
            }
            File[] jsonFiles = themesDir.listFiles(
                    (dir, name) -> name.endsWith(".json"));
            if (jsonFiles == null) {
                continue;
            }
            for (File jsonFile : jsonFiles) {
                try {
                    String content = readFileUtf8(jsonFile);
                    ThemeDefinition def = GSON.fromJson(content, ThemeDefinition.class);
                    if (def != null && def.id != null) {
                        results.add(def);
                        LOGGER.debug("Found theme JSON: {}", jsonFile.getAbsolutePath());
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to parse theme JSON: {}", jsonFile.getAbsolutePath(), e);
                }
            }
        }
    }

    /**
     * Scan a JAR file for theme JSON entries.
     * <p>扫描 JAR 文件以查找主题 JSON 条目。</p>
     */
    private static void scanJar(JarFile jarFile, List<ThemeDefinition> results) {
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();
            // Match pattern: assets/<namespace>/themes/<id>.json
            // 匹配模式：assets/<namespace>/themes/<id>.json
            if (name.startsWith("assets/") && name.endsWith(".json")
                    && name.contains("/themes/")) {
                try (InputStream is = jarFile.getInputStream(entry)) {
                    String content = readStreamUtf8(is);
                    ThemeDefinition def = GSON.fromJson(content, ThemeDefinition.class);
                    if (def != null && def.id != null) {
                        results.add(def);
                        LOGGER.debug("Found theme JSON in JAR: {}", name);
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to parse theme JSON from JAR: {}", name, e);
                }
            }
        }
    }

    // ── I/O helpers ──

    private static String readFileUtf8(File file) throws Exception {
        byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
        return new String(bytes, UTF_8);
    }

    private static String readStreamUtf8(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append('\n');
        }
        reader.close();
        return sb.toString();
    }

    /**
     * Find the {@link ThemeDefinition} with the given id.
     * <p>查找具有给定 id 的 {@link ThemeDefinition}。</p>
     */
    private static ThemeDefinition findDefinition(List<ThemeDefinition> defs, String id) {
        for (ThemeDefinition def : defs) {
            if (id.equals(def.id)) {
                return def;
            }
        }
        return null;
    }
}
