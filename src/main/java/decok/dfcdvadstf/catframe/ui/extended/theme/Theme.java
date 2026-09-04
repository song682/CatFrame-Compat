package decok.dfcdvadstf.catframe.ui.extended.theme;

import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

/**
 * <p>
 * Theme interface — semantic indirection layer for UI textures, colours, and sounds.<br>
 * A Theme maps semantic keys (e.g. {@code "catframe:widget/button/enabled"}) to concrete
 * {@link ResourceLocation} textures, ARGB colour integers, or sound event locations.
 * </p>
 * <p>
 * 主题接口 —— UI 纹理、颜色与音效的语义间接层。<br>
 * 主题将语义键（如 {@code "catframe:widget/button/enabled"}）映射到具体的
 * {@link ResourceLocation} 纹理、ARGB 颜色整数或音效事件位置。
 * </p>
 *
 * <h3>Fallback chain / 回退链</h3>
 * <p>
 * When a lookup returns {@code null}, the caller (typically {@link ThemeManager})
 * falls back to {@link #getFallback()} and, ultimately, to {@link DefaultTheme}.
 * </p>
 */
public interface Theme {

    /**
     * Display name of this theme.
     * <p>此主题的显示名称。</p>
     */
    String getName();

    // ──── Textures ────

    /**
     * Resolve a semantic texture key to a {@link ResourceLocation}.
     *
     * @param key semantic key (e.g. {@code "catframe:widget/button/enabled"})
     * @return resolved texture, or {@code null} to fall back to {@link #getFallback()} / {@link DefaultTheme}
     */
    @Nullable
    ResourceLocation getTexture(String key);

    // ──── Colours ────

    /**
     * Resolve a semantic colour key to an ARGB integer.
     *
     * @param key semantic key (e.g. {@code "catframe:color/button/text_enabled"})
     * @return resolved ARGB colour, or {@code null} to fall back
     */
    @Nullable
    Integer getColor(String key);

    // ──── Sounds ────

    /**
     * Resolve a semantic sound key to a sound-event {@link ResourceLocation}.
     *
     * @param key semantic key (e.g. {@code "catframe:sound/button_press"})
     * @return resolved sound event location, or {@code null} for silent / fall back
     */
    @Nullable
    ResourceLocation getSound(String key);

    // ──── Fallback ────

    /**
     * Optional parent theme for inheritance. Keys not found in this theme are
     * delegated to the fallback before reaching {@link DefaultTheme}.
     * <p>
     * 可选的父主题，用于继承。此主题中未找到的键将委托给回退主题，
     * 最终才到达 {@link DefaultTheme}。
     *
     * @return parent theme, or {@code null} if this is a root theme
     */
    @Nullable
    default Theme getFallback() {
        return null;
    }
}
