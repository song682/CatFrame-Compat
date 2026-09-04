package decok.dfcdvadstf.catframe.ui.extended.theme;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * Theme manager — client-side singleton that drives theme registration, selection,
 * and semantic-key resolution with a fallback chain.<br>
 * Resolution order: active theme → active theme's fallback (recursive) → {@link DefaultTheme} → null.
 * </p>
 * <p>
 * 主题管理器 —— 客户端单例，驱动主题注册、选择与语义键解析（带回退链）。<br>
 * 解析顺序：活动主题 → 活动主题的回退（递归）→ {@link DefaultTheme} → null。
 * </p>
 */
@SideOnly(Side.CLIENT)
public final class ThemeManager {

    private static final Logger LOGGER = LogManager.getLogger("CatFrameCompact|Theme");

    private static final ThemeManager INSTANCE = new ThemeManager();

    /** Registered themes by id. / 已注册主题（按 id 索引）。 */
    private final Map<String, Theme> themes = new LinkedHashMap<>();

    /** Currently active theme. / 当前活动主题。 */
    private Theme activeTheme = DefaultTheme.INSTANCE;

    private ThemeManager() {
    }

    /** Singleton accessor. / 单例访问入口。 */
    public static ThemeManager getInstance() {
        return INSTANCE;
    }

    // ──── Registration ────

    /**
     * Register a theme under the given id.
     * <p>以给定 id 注册一个主题。</p>
     */
    public void register(String id, Theme theme) {
        if (id == null || theme == null) {
            return;
        }
        themes.put(id, theme);
        LOGGER.info("Registered theme '{}' ({})", id, theme.getName());
    }

    /**
     * Set the active theme by id. Falls back to {@link DefaultTheme} if the id
     * is not registered.
     * <p>按 id 设置活动主题。若 id 未注册则回退到 {@link DefaultTheme}。</p>
     */
    public void setActive(String themeId) {
        Theme theme = themes.get(themeId);
        if (theme == null) {
            LOGGER.warn("Theme '{}' not found, falling back to default", themeId);
            activeTheme = DefaultTheme.INSTANCE;
        } else {
            activeTheme = theme;
            LOGGER.info("Active theme set to '{}' ({})", themeId, theme.getName());
        }
    }

    /**
     * @return the currently active theme (never null) / 当前活动主题（永不为 null）
     */
    public Theme getActive() {
        return activeTheme;
    }

    /**
     * @return unmodifiable view of all registered theme ids / 所有已注册主题 id 的只读视图
     */
    public Set<String> getAvailableThemes() {
        return Collections.unmodifiableSet(themes.keySet());
    }

    // ──── Texture resolution ────

    /**
     * Resolve a texture key through the active theme with fallback chain.
     * <p>通过活动主题及回退链解析纹理键。</p>
     *
     * @return resolved texture, or {@code null} if no theme provides it
     */
    @Nullable
    public ResourceLocation resolveTexture(String key) {
        ResourceLocation result = resolveTextureInChain(activeTheme, key);
        if (result != null) {
            return result;
        }
        return DefaultTheme.INSTANCE.getTexture(key);
    }

    /**
     * Resolve a texture key with an explicit fallback value.
     * <p>解析纹理键，若无法解析则使用显式回退值。</p>
     */
    public ResourceLocation resolveTextureOrDefault(String key, ResourceLocation fallback) {
        ResourceLocation result = resolveTexture(key);
        return result != null ? result : fallback;
    }

    // ──── Colour resolution ────

    /**
     * Resolve a colour key through the active theme with fallback chain.
     * <p>通过活动主题及回退链解析颜色键。</p>
     *
     * @return resolved ARGB colour, or {@code null} if no theme provides it
     */
    @Nullable
    public Integer resolveColor(String key) {
        Integer result = resolveColorInChain(activeTheme, key);
        if (result != null) {
            return result;
        }
        return DefaultTheme.INSTANCE.getColor(key);
    }

    /**
     * Resolve a colour key with an explicit fallback value.
     * <p>解析颜色键，若无法解析则使用显式回退值。</p>
     */
    public int resolveColorOrDefault(String key, int fallback) {
        Integer result = resolveColor(key);
        return result != null ? result : fallback;
    }

    // ──── Sound resolution ────

    /**
     * Resolve a sound key through the active theme with fallback chain.
     * <p>通过活动主题及回退链解析音效键。</p>
     *
     * @return resolved sound event location, or {@code null} for silent
     */
    @Nullable
    public ResourceLocation resolveSound(String key) {
        ResourceLocation result = resolveSoundInChain(activeTheme, key);
        if (result != null) {
            return result;
        }
        return DefaultTheme.INSTANCE.getSound(key);
    }

    /**
     * Resolve a sound key with an explicit fallback value.
     * <p>解析音效键，若无法解析则使用显式回退值。</p>
     */
    @Nullable
    public ResourceLocation resolveSoundOrDefault(String key, @Nullable ResourceLocation fallback) {
        ResourceLocation result = resolveSound(key);
        return result != null ? result : fallback;
    }

    // ──── Reload ────

    /**
     * Clear all registered themes (except the built-in default) and reset to
     * {@link DefaultTheme}. Callers should re-load themes afterwards.
     * <p>清空所有已注册主题（内置默认主题除外）并重置为 {@link DefaultTheme}。
     * 调用方应在之后重新加载主题。</p>
     */
    public void reload() {
        themes.clear();
        activeTheme = DefaultTheme.INSTANCE;
        LOGGER.info("Theme manager reloaded");
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Internal: walk the fallback chain
    // ══════════════════════════════════════════════════════════════════════

    @Nullable
    private static ResourceLocation resolveTextureInChain(@Nullable Theme theme, String key) {
        if (theme == null) {
            return null;
        }
        ResourceLocation result = theme.getTexture(key);
        if (result != null) {
            return result;
        }
        return resolveTextureInChain(theme.getFallback(), key);
    }

    @Nullable
    private static Integer resolveColorInChain(@Nullable Theme theme, String key) {
        if (theme == null) {
            return null;
        }
        Integer result = theme.getColor(key);
        if (result != null) {
            return result;
        }
        return resolveColorInChain(theme.getFallback(), key);
    }

    @Nullable
    private static ResourceLocation resolveSoundInChain(@Nullable Theme theme, String key) {
        if (theme == null) {
            return null;
        }
        ResourceLocation result = theme.getSound(key);
        if (result != null) {
            return result;
        }
        return resolveSoundInChain(theme.getFallback(), key);
    }
}
