package decok.dfcdvadstf.catframe.ui.extended.theme;

import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;

/**
 * <p>
 * Gson data class for deserialising theme JSON files
 * ({@code assets/<namespace>/themes/<id>.json}).<br>
 * Also contains the inner {@link JsonTheme} implementation that wraps the
 * parsed data into a live {@link Theme} instance.
 * </p>
 * <p>
 * 用于反序列化主题 JSON 文件（{@code assets/<namespace>/themes/<id>.json}）
 * 的 Gson 数据类。内含 {@link JsonTheme} 实现，将解析后的数据包装为可运行的
 * {@link Theme} 实例。
 * </p>
 *
 * <h3>JSON schema / JSON 模式</h3>
 * <pre>{@code
 * {
 *   "id": "mymod:dark",
 *   "name": "Dark Theme",
 *   "fallback": "catframe:vanilla",
 *   "textures": { "catframe:widget/button/enabled": "mymod:textures/gui/dark/button.png" },
 *   "colors":   { "catframe:color/button/text_enabled": "#FFFFFF" },
 *   "sounds":   { "catframe:sound/button_press": "minecraft:gui.button.press" }
 * }
 * }</pre>
 */
public class ThemeDefinition {

    /** Unique theme id (e.g. {@code "catframe:vanilla"}). / 主题唯一 id。 */
    public String id;

    /** Human-readable display name. / 可读显示名称。 */
    public String name;

    /**
     * Optional parent theme id for inheritance. Keys not present in this theme's
     * maps are delegated to the parent before reaching {@link DefaultTheme}.
     * <p>可选的父主题 id，用于继承。此主题的映射中不存在的键将委托给父主题，
     * 最终才到达 {@link DefaultTheme}。</p>
     */
    @Nullable
    public String fallback;

    /** Texture key → texture path (e.g. {@code "catframe:textures/gui/widgets/button.png"}). */
    public Map<String, String> textures = Collections.emptyMap();

    /** Colour key → hex string (e.g. {@code "#E0E0E0"} or {@code "#FFE0E0E0"}). */
    public Map<String, String> colors = Collections.emptyMap();

    /** Sound key → sound event name (e.g. {@code "minecraft:gui.button.press"}). */
    public Map<String, String> sounds = Collections.emptyMap();

    // ══════════════════════════════════════════════════════════════════════
    //  Inner Theme implementation
    // ══════════════════════════════════════════════════════════════════════

    /**
     * <p>
     * A {@link Theme} backed by parsed {@link ThemeDefinition} data.<br>
     * Texture paths are lazily converted to {@link ResourceLocation} on first access.
     * </p>
     * <p>
     * 由解析后的 {@link ThemeDefinition} 数据支撑的 {@link Theme} 实现。<br>
     * 纹理路径在首次访问时惰性转换为 {@link ResourceLocation}。
     * </p>
     */
    public static class JsonTheme implements Theme {

        private final String id;
        private final String name;
        private final Map<String, String> rawTextures;
        private final Map<String, Integer> parsedColors;
        private final Map<String, ResourceLocation> parsedSounds;
        @Nullable
        private final Theme fallbackTheme;

        public JsonTheme(ThemeDefinition def, @Nullable Theme fallbackTheme) {
            this.id = def.id;
            this.name = def.name != null ? def.name : def.id;
            this.rawTextures = def.textures != null ? def.textures : Collections.<String, String>emptyMap();
            this.parsedColors = parseColors(def.colors);
            this.parsedSounds = parseSounds(def.sounds);
            this.fallbackTheme = fallbackTheme;
        }

        @Override
        public String getName() {
            return name;
        }

        @Nullable
        @Override
        public ResourceLocation getTexture(String key) {
            String path = rawTextures.get(key);
            if (path == null) {
                return null;
            }
            return new ResourceLocation(path);
        }

        @Nullable
        @Override
        public Integer getColor(String key) {
            return parsedColors.get(key);
        }

        @Nullable
        @Override
        public ResourceLocation getSound(String key) {
            return parsedSounds.get(key);
        }

        @Nullable
        @Override
        public Theme getFallback() {
            return fallbackTheme;
        }

        /** @return the raw theme id from JSON / JSON 中的原始主题 id */
        public String getId() {
            return id;
        }

        /** @return the raw fallback id from JSON, or {@code null} / JSON 中的原始回退 id */
        @Nullable
        public String getFallbackId() {
            return fallbackTheme != null ? id : null;
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Parsing helpers
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Parse colour hex strings ({@code "#RRGGBB"} or {@code "#AARRGGBB"}) into
     * ARGB integers.
     * <p>将颜色十六进制字符串解析为 ARGB 整数。</p>
     */
    private static Map<String, Integer> parseColors(@Nullable Map<String, String> raw) {
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyMap();
        }
        java.util.HashMap<String, Integer> result = new java.util.HashMap<>();
        for (Map.Entry<String, String> entry : raw.entrySet()) {
            Integer color = parseColorString(entry.getValue());
            if (color != null) {
                result.put(entry.getKey(), color);
            }
        }
        return result;
    }

    /**
     * Parse a single colour string.
     *
     * @return ARGB int, or {@code null} if unparseable
     */
    @Nullable
    static Integer parseColorString(@Nullable String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            String hex = value.startsWith("#") ? value.substring(1) : value;
            long parsed = Long.parseLong(hex, 16);
            if (hex.length() <= 6) {
                // RGB → ARGB (fully opaque) / RGB → ARGB（完全不透明）
                return (int) (0xFF000000L | parsed);
            }
            return (int) parsed;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Parse sound event strings into {@link ResourceLocation}.
     * <p>将音效事件字符串解析为 {@link ResourceLocation}。</p>
     */
    private static Map<String, ResourceLocation> parseSounds(@Nullable Map<String, String> raw) {
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyMap();
        }
        java.util.HashMap<String, ResourceLocation> result = new java.util.HashMap<>();
        for (Map.Entry<String, String> entry : raw.entrySet()) {
            String value = entry.getValue();
            if (value != null && !value.isEmpty()) {
                result.put(entry.getKey(), new ResourceLocation(value));
            }
        }
        return result;
    }
}
