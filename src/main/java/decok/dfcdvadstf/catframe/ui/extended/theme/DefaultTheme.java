package decok.dfcdvadstf.catframe.ui.extended.theme;

import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Built-in default theme — maps every known CatFrame semantic key to its shipped
 * texture, colour, or sound. Acts as the ultimate fallback in the theme resolution
 * chain (after the active theme and its optional parent have been consulted).
 * </p>
 * <p>
 * 内置默认主题 —— 将每个已知的 CatFrame 语义键映射到其自带的纹理、颜色或音效。
 * 在主题解析链中充当最终回退（在活动主题及其可选父主题均被查询之后）。
 * </p>
 */
public final class DefaultTheme implements Theme {

    /** Singleton instance. */
    public static final DefaultTheme INSTANCE = new DefaultTheme();

    /** Theme id constant. */
    public static final String ID = "catframe:default";

    // ── Texture map ──
    private static final Map<String, ResourceLocation> TEXTURES = new HashMap<>();
    // ── Colour map ──
    private static final Map<String, Integer> COLORS = new HashMap<>();
    // ── Sound map ──
    private static final Map<String, ResourceLocation> SOUNDS = new HashMap<>();

    static {
        initTextures();
        initColors();
        initSounds();
    }

    private DefaultTheme() {
    }

    @Override
    public String getName() {
        return "CatFrame Default";
    }

    @Nullable
    @Override
    public ResourceLocation getTexture(String key) {
        return TEXTURES.get(key);
    }

    @Nullable
    @Override
    public Integer getColor(String key) {
        return COLORS.get(key);
    }

    @Nullable
    @Override
    public ResourceLocation getSound(String key) {
        return SOUNDS.get(key);
    }

    /** Root theme — no fallback. / 根主题，无回退。 */
    @Nullable
    @Override
    public Theme getFallback() {
        return null;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Texture defaults
    // ══════════════════════════════════════════════════════════════════════

    private static void initTextures() {
        String w = "catframe:textures/gui/widgets/";
        String t = "catframe:textures/gui/tabs/";
        String tt = "catframe:textures/gui/tooltips/";
        String ts = "catframe:textures/gui/toast/";
        String s = "catframe:textures/gui/seperator/";

        // Widget / Button
        TEXTURES.put(ThemeKeys.Textures.BUTTON_ENABLED, rl(w + "button.png"));
        TEXTURES.put(ThemeKeys.Textures.BUTTON_DISABLED, rl(w + "button_disabled.png"));
        TEXTURES.put(ThemeKeys.Textures.BUTTON_HIGHLIGHTED, rl(w + "button_highlighted.png"));

        // Widget / Checkbox
        TEXTURES.put(ThemeKeys.Textures.CHECKBOX_NORMAL, rl(w + "checkbox.png"));
        TEXTURES.put(ThemeKeys.Textures.CHECKBOX_HIGHLIGHTED, rl(w + "checkbox_highlighted.png"));
        TEXTURES.put(ThemeKeys.Textures.CHECKBOX_CHECKED, rl(w + "checkbox_checked.png"));
        TEXTURES.put(ThemeKeys.Textures.CHECKBOX_CHECKED_HIGHLIGHTED, rl(w + "checkbox_checked_highlighted.png"));

        // Widget / Scroll
        TEXTURES.put(ThemeKeys.Textures.SCROLL_SCROLLER, rl(w + "scroll.png"));
        TEXTURES.put(ThemeKeys.Textures.SCROLL_TRACK, rl(w + "scroll_background.png"));

        // Widget / Selection
        TEXTURES.put(ThemeKeys.Textures.SELECTION_NORMAL, rl(w + "selection.png"));
        TEXTURES.put(ThemeKeys.Textures.SELECTION_HIGHLIGHTED, rl(w + "selection_highlighted.png"));
        TEXTURES.put(ThemeKeys.Textures.SELECTION_SELECTED, rl(w + "selection_selected.png"));
        TEXTURES.put(ThemeKeys.Textures.SELECTION_SELECTED_HIGHLIGHTED, rl(w + "selection_selected_highlighted.png"));

        // Widget / EditBox
        TEXTURES.put(ThemeKeys.Textures.EDITBOX_NORMAL, rl(w + "text_field.png"));
        TEXTURES.put(ThemeKeys.Textures.EDITBOX_FOCUSED, rl(w + "text_field_highlighted.png"));

        // Widget / Toggle
        TEXTURES.put(ThemeKeys.Textures.TOGGLE_NORMAL, rl(w + "toggle_swticher.png"));
        TEXTURES.put(ThemeKeys.Textures.TOGGLE_HIGHLIGHTED, rl(w + "toggle_swticher_highlighted.png"));
        TEXTURES.put(ThemeKeys.Textures.TOGGLE_TOGGLED, rl(w + "toggle_swticher_toggled.png"));
        TEXTURES.put(ThemeKeys.Textures.TOGGLE_TOGGLED_HIGHLIGHTED, rl(w + "toggle_swticher_toggled_highlighted.png"));

        // Tab
        TEXTURES.put(ThemeKeys.Textures.TAB_NORMAL, rl(t + "tab.png"));
        TEXTURES.put(ThemeKeys.Textures.TAB_HIGHLIGHTED, rl(t + "tab_highlighted.png"));
        TEXTURES.put(ThemeKeys.Textures.TAB_SELECTED, rl(t + "tab_selected.png"));
        TEXTURES.put(ThemeKeys.Textures.TAB_SELECTED_HIGHLIGHTED, rl(t + "tab_selected_highlighted.png"));

        // Tooltip
        TEXTURES.put(ThemeKeys.Textures.TOOLTIP_BACKGROUND, rl(tt + "background.png"));
        TEXTURES.put(ThemeKeys.Textures.TOOLTIP_FRAME, rl(tt + "frame.png"));

        // Toast
        TEXTURES.put(ThemeKeys.Textures.TOAST_DEFAULT, rl(ts + "default.png"));
        TEXTURES.put(ThemeKeys.Textures.TOAST_ADVANCEMENT, rl(ts + "advancement.png"));
        TEXTURES.put(ThemeKeys.Textures.TOAST_RECIPE, rl(ts + "recipe.png"));
        TEXTURES.put(ThemeKeys.Textures.TOAST_SYSTEM, rl(ts + "system.png"));
        TEXTURES.put(ThemeKeys.Textures.TOAST_TUTORIAL, rl(ts + "tutorial.png"));
        TEXTURES.put(ThemeKeys.Textures.TOAST_NOW_PLAYING, rl(ts + "now_playing.png"));

        // Panel
        TEXTURES.put(ThemeKeys.Textures.PANEL_HEADER_SEPARATOR, rl(s + "header_separator.png"));
        TEXTURES.put(ThemeKeys.Textures.PANEL_FOOTER_SEPARATOR, rl(s + "footer_separator.png"));
        TEXTURES.put(ThemeKeys.Textures.PANEL_BACKGROUND, rl(s + "panel_background.png"));
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Colour defaults
    // ══════════════════════════════════════════════════════════════════════

    private static void initColors() {
        // Button text
        COLORS.put(ThemeKeys.Colors.BUTTON_TEXT_ENABLED, 0xE0E0E0);
        COLORS.put(ThemeKeys.Colors.BUTTON_TEXT_DISABLED, 0xA0A0A0);
        COLORS.put(ThemeKeys.Colors.BUTTON_TEXT_HOVER, 0xFFFFA0);

        // Tab text
        COLORS.put(ThemeKeys.Colors.TAB_TEXT_SELECTED, 0xFFFFFF);
        COLORS.put(ThemeKeys.Colors.TAB_TEXT_HOVERED, 0xFFFF55);
        COLORS.put(ThemeKeys.Colors.TAB_TEXT_NORMAL, 0xA0A0A0);

        // Toast
        COLORS.put(ThemeKeys.Colors.TOAST_BACKGROUND, 0xCC000000);
        COLORS.put(ThemeKeys.Colors.TOAST_BORDER, 0xFF555555);

        // EditBox
        COLORS.put(ThemeKeys.Colors.EDITBOX_BACKGROUND, 0xFF333333);
        COLORS.put(ThemeKeys.Colors.EDITBOX_BACKGROUND_FOCUSED, 0xFF333366);
        COLORS.put(ThemeKeys.Colors.EDITBOX_BORDER, 0xFF888888);
        COLORS.put(ThemeKeys.Colors.EDITBOX_BORDER_FOCUSED, 0xFFFFFFFF);

        // TabBar
        COLORS.put(ThemeKeys.Colors.TABBAR_BACKGROUND, 0xFF000000);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Sound defaults
    // ══════════════════════════════════════════════════════════════════════

    private static void initSounds() {
        SOUNDS.put(ThemeKeys.Sounds.BUTTON_PRESS,
                new ResourceLocation("minecraft", "sounds/gui/button.press.ogg"));
        // toast_show / toast_hide default to silent (absent from map → null)
    }

    // ── Helper ──

    private static ResourceLocation rl(String path) {
        return new ResourceLocation(path);
    }
}
