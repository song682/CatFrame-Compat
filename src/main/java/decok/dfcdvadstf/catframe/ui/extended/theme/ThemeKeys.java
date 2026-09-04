package decok.dfcdvadstf.catframe.ui.extended.theme;

/**
 * <p>
 * Semantic key constants for the Theme system, covering textures, colours, and sounds.<br>
 * All keys follow the pattern {@code catframe:<category>/<element>/<state>}.
 * </p>
 * <p>
 * 主题系统的语义键常量，涵盖纹理、颜色与音效。<br>
 * 所有键遵循 {@code catframe:<类别>/<元素>/<状态>} 模式。
 * </p>
 */
public final class ThemeKeys {

    private ThemeKeys() {
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Texture keys
    // ════════════════════════════════════════════════════════════════════════

    /** Semantic texture keys — UI widget, tab, toast, tooltip, and panel textures. */
    public static final class Textures {

        private Textures() {
        }

        // ── Widget / Button ──

        public static final String BUTTON_ENABLED = "catframe:widget/button/enabled";
        public static final String BUTTON_DISABLED = "catframe:widget/button/disabled";
        public static final String BUTTON_HIGHLIGHTED = "catframe:widget/button/highlighted";

        // ── Widget / Checkbox ──

        public static final String CHECKBOX_NORMAL = "catframe:widget/checkbox/normal";
        public static final String CHECKBOX_HIGHLIGHTED = "catframe:widget/checkbox/highlighted";
        public static final String CHECKBOX_CHECKED = "catframe:widget/checkbox/checked";
        public static final String CHECKBOX_CHECKED_HIGHLIGHTED = "catframe:widget/checkbox/checked_highlighted";

        // ── Widget / Scroll ──

        public static final String SCROLL_SCROLLER = "catframe:widget/scroll/scroller";
        public static final String SCROLL_TRACK = "catframe:widget/scroll/track";

        // ── Widget / Selection ──

        public static final String SELECTION_NORMAL = "catframe:widget/selection/normal";
        public static final String SELECTION_HIGHLIGHTED = "catframe:widget/selection/highlighted";
        public static final String SELECTION_SELECTED = "catframe:widget/selection/selected";
        public static final String SELECTION_SELECTED_HIGHLIGHTED = "catframe:widget/selection/selected_highlighted";

        // ── Widget / EditBox ──

        public static final String EDITBOX_NORMAL = "catframe:widget/editbox/normal";
        public static final String EDITBOX_FOCUSED = "catframe:widget/editbox/focused";

        // ── Widget / Toggle ──

        public static final String TOGGLE_NORMAL = "catframe:widget/toggle/normal";
        public static final String TOGGLE_HIGHLIGHTED = "catframe:widget/toggle/highlighted";
        public static final String TOGGLE_TOGGLED = "catframe:widget/toggle/toggled";
        public static final String TOGGLE_TOGGLED_HIGHLIGHTED = "catframe:widget/toggle/toggled_highlighted";

        // ── Tab ──

        public static final String TAB_NORMAL = "catframe:tab/normal";
        public static final String TAB_HIGHLIGHTED = "catframe:tab/highlighted";
        public static final String TAB_SELECTED = "catframe:tab/selected";
        public static final String TAB_SELECTED_HIGHLIGHTED = "catframe:tab/selected_highlighted";

        // ── Tooltip ──

        public static final String TOOLTIP_BACKGROUND = "catframe:tooltip/background";
        public static final String TOOLTIP_FRAME = "catframe:tooltip/frame";

        // ── Toast ──

        public static final String TOAST_DEFAULT = "catframe:toast/default";
        public static final String TOAST_ADVANCEMENT = "catframe:toast/advancement";
        public static final String TOAST_RECIPE = "catframe:toast/recipe";
        public static final String TOAST_SYSTEM = "catframe:toast/system";
        public static final String TOAST_TUTORIAL = "catframe:toast/tutorial";
        public static final String TOAST_NOW_PLAYING = "catframe:toast/now_playing";

        // ── Panel ──

        public static final String PANEL_HEADER_SEPARATOR = "catframe:panel/header_separator";
        public static final String PANEL_FOOTER_SEPARATOR = "catframe:panel/footer_separator";
        public static final String PANEL_BACKGROUND = "catframe:panel/background";
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Colour keys
    // ════════════════════════════════════════════════════════════════════════

    /** Semantic colour keys — text colours, background fills, border colours. */
    public static final class Colors {

        private Colors() {
        }

        // ── Button text ──

        public static final String BUTTON_TEXT_ENABLED = "catframe:color/button/text_enabled";
        public static final String BUTTON_TEXT_DISABLED = "catframe:color/button/text_disabled";
        public static final String BUTTON_TEXT_HOVER = "catframe:color/button/text_hover";

        // ── Tab text ──

        public static final String TAB_TEXT_SELECTED = "catframe:color/tab/text_selected";
        public static final String TAB_TEXT_HOVERED = "catframe:color/tab/text_hovered";
        public static final String TAB_TEXT_NORMAL = "catframe:color/tab/text_normal";

        // ── Toast ──

        public static final String TOAST_BACKGROUND = "catframe:color/toast/background";
        public static final String TOAST_BORDER = "catframe:color/toast/border";

        // ── EditBox ──

        public static final String EDITBOX_BACKGROUND = "catframe:color/editbox/background";
        public static final String EDITBOX_BACKGROUND_FOCUSED = "catframe:color/editbox/background_focused";
        public static final String EDITBOX_BORDER = "catframe:color/editbox/border";
        public static final String EDITBOX_BORDER_FOCUSED = "catframe:color/editbox/border_focused";

        // ── TabBar ──

        public static final String TABBAR_BACKGROUND = "catframe:color/tabbar/background";
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Sound keys
    // ════════════════════════════════════════════════════════════════════════

    /** Semantic sound keys — UI interaction sound effects. */
    public static final class Sounds {

        private Sounds() {
        }

        public static final String BUTTON_PRESS = "catframe:sound/button_press";
        public static final String TOAST_SHOW = "catframe:sound/toast_show";
        public static final String TOAST_HIDE = "catframe:sound/toast_hide";
    }
}
