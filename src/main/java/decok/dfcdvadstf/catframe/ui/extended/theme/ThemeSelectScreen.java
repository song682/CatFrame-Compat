package decok.dfcdvadstf.catframe.ui.extended.theme;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import decok.dfcdvadstf.catframe.CatFrameCompat;
import decok.dfcdvadstf.catframe.ui.GuiDrawing;
import decok.dfcdvadstf.catframe.ui.Text;
import decok.dfcdvadstf.catframe.ui.extended.ScreenExtended;
import decok.dfcdvadstf.catframe.ui.util.TextureStretching;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 主题选择界面 —— 左侧列出所有已注册主题，右侧实时预览选中主题下的典型界面组件外观，
 * 底部提供 Apply（应用并持久化选中主题）与 Done（关闭界面）。
 * 界面骨架（面板、按钮、列表高亮）始终跟随当前活动主题渲染；
 * 预览区内容则完全按“选中主题”解析纹理与颜色（预览主题 ≠ 活动主题时即可直接对比差异）。
 * </p>
 * <p>
 * Theme selection screen — lists every registered theme on the left, renders a live
 * preview of a sample screen under the selected theme on the right, and offers
 * Apply (activate + persist the selection) and Done (close) at the bottom.
 * The screen chrome (panel, buttons, list highlight) always follows the active
 * theme, while the preview resolves every texture and colour against the
 * <em>selected</em> theme, so differences are visible before applying.
 * </p>
 *
 * <h3>Controls / 操作</h3>
 * <ul>
 * <li>Mouse wheel / Up / Down — move through the theme list / 鼠标滚轮、上下方向键浏览列表</li>
 * <li>Click / Enter — select the highlighted row / 单击或回车选中高亮行</li>
 * <li>Apply — activate and persist the selected theme / 应用并持久化选中主题</li>
 * <li>Done / Esc — close the screen / 关闭界面</li>
 * </ul>
 */
@SideOnly(Side.CLIENT)
public class ThemeSelectScreen extends ScreenExtended {

    // ──── Layout constants ────

    private static final int MARGIN = 10;
    private static final int TITLE_BAR_H = 28;
    private static final int BOTTOM_BAR_H = 28;
    private static final int ROW_H = 20;
    private static final int BTN_W = 100;
    private static final int BTN_H = 20;
    private static final int LIST_W_MAX = 150;
    private static final int SCROLLBAR_W = 6;
    /** Selection texture tile size (8x8 source). / 选择纹理平铺尺寸（源图 8x8）。 */
    private static final int SELECTION_TILE = 8;

    // ──── State ────

    /** All registered theme ids, in registration order. / 所有已注册主题 id（注册顺序）。 */
    private final List<String> themeIds = new ArrayList<>();

    /** Previewed (not yet applied) theme index. / 当前预览（尚未应用）的主题索引。 */
    private int selectedIndex;

    /** Index of the first visible list row. / 列表首个可见行的索引。 */
    private int scrollOffset;

    // ──── Cached layout (recomputed in init()) ────
    private int contentTop;
    private int contentBottom;
    private int listX;
    private int listY;
    private int listW;
    private int listH;
    private int previewX;
    private int previewY;
    private int previewW;
    private int previewH;
    private int applyX;
    private int applyY;
    private int doneX;

    public ThemeSelectScreen() {
        super(Text.translatableWithFallback("catframe.theme.title", "Theme Selection"));
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Lifecycle
    // ══════════════════════════════════════════════════════════════════════

    @Override
    protected void init() {
        ThemeManager manager = ThemeManager.getInstance();
        themeIds.clear();
        themeIds.addAll(manager.getAvailableThemes());

        selectedIndex = themeIds.indexOf(manager.getActiveId());
        if (selectedIndex < 0) {
            selectedIndex = 0;
        }
        scrollOffset = 0;

        contentTop = TITLE_BAR_H + 2;
        contentBottom = height - BOTTOM_BAR_H - 2;

        listX = MARGIN;
        listW = Math.min(LIST_W_MAX, Math.max(80, width / 4));
        listY = contentTop + 16;
        listH = Math.max(ROW_H, contentBottom - listY - 4);

        previewX = listX + listW + 12;
        previewY = listY;
        previewW = Math.max(0, width - previewX - MARGIN);
        previewH = listH;

        applyY = contentBottom + 4;
        doneX = width - MARGIN - BTN_W;
        applyX = doneX - BTN_W - 8;

        ensureVisible();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Rendering
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Draws the whole screen — the base class routes this to
     * {@link #renderBackground(int, int, float)} before any child widgets.
     * <p>绘制整个界面 —— 基类会先调用本方法，再由其驱动背景渲染。</p>
     */
    @Override
    protected void renderBackground(int mouseX, int mouseY, float partialTicks) {
        FontRenderer font = getFont();

        // ── Screen chrome (active theme) ──
        drawThemedPanel(0, 0, width, height - 2);
        drawSeparator(0, TITLE_BAR_H, width);
        drawSeparator(0, contentBottom, width);

        String title = getTitle().getString();
        font.drawStringWithShadow(title, (width - font.getStringWidth(title)) / 2, 8,
                getButtonTextColor(true, false));

        // ── Theme list & preview ──
        renderList(mouseX, mouseY);
        renderPreview(mouseX, mouseY);

        // ── Bottom bar (Apply / Done / active status) ──
        renderBottomBar(mouseX, mouseY);
    }

    // ──── List ────

    private void renderList(int mouseX, int mouseY) {
        FontRenderer font = getFont();
        font.drawStringWithShadow(tr("catframe.theme.themes", "Themes"), listX, contentTop + 4, 0xA0A0A0);

        if (themeIds.isEmpty()) {
            font.drawStringWithShadow(tr("catframe.theme.empty", "No themes registered"),
                    listX, listY, 0xA0A0A0);
            return;
        }

        int visible = visibleRows();
        int maxOffset = Math.max(0, themeIds.size() - visible);
        if (scrollOffset > maxOffset) {
            scrollOffset = maxOffset;
        }

        for (int i = 0; i < visible; i++) {
            int index = scrollOffset + i;
            if (index >= themeIds.size()) {
                break;
            }
            String id = themeIds.get(index);
            int rowY = listY + i * ROW_H;
            boolean isSelected = index == selectedIndex;
            boolean isHovered = isInList(mouseX, mouseY) && mouseY >= rowY && mouseY < rowY + ROW_H;

            // Row background — themed selection texture (selected / hovered).
            ResourceLocation rowTex = null;
            if (isSelected) {
                rowTex = resolveTexture(isHovered
                        ? ThemeKeys.Textures.SELECTION_SELECTED_HIGHLIGHTED
                        : ThemeKeys.Textures.SELECTION_SELECTED);
            } else if (isHovered) {
                rowTex = resolveTexture(ThemeKeys.Textures.SELECTION_HIGHLIGHTED);
            }
            if (rowTex != null) {
                TextureStretching.drawTiled(rowTex, listX, rowY, listW, ROW_H - 1,
                        SELECTION_TILE, SELECTION_TILE);
            }

            Theme theme = ThemeManager.getInstance().getTheme(id);
            String name = theme != null ? theme.getName() : id;
            String label = font.trimStringToWidth(name, listW - 8);
            int textColor = isSelected
                    ? resolveColor(ThemeKeys.Colors.TAB_TEXT_SELECTED, 0xFFFFFF)
                    : getButtonTextColor(true, isHovered);
            font.drawStringWithShadow(label, listX + 4, rowY + 6, textColor);

            // "in use" marker on the currently active theme's row.
            if (id.equals(ThemeManager.getInstance().getActiveId())) {
                String mark = tr("catframe.theme.in_use", "in use");
                int markW = font.getStringWidth(mark);
                font.drawStringWithShadow(mark, listX + listW - markW - 8, rowY + 6, 0x808080);
            }
        }

        renderScrollbar(visible, maxOffset);
    }

    private void renderScrollbar(int visible, int maxOffset) {
        if (maxOffset <= 0) {
            return;
        }
        int trackX = listX + listW - SCROLLBAR_W;
        ResourceLocation trackTex = resolveTexture(ThemeKeys.Textures.SCROLL_TRACK);
        if (trackTex != null) {
            TextureStretching.drawAutoNinePatch(trackTex, trackX, listY, SCROLLBAR_W, listH, 6, 32, 1);
        }
        int thumbH = Math.max(12, listH * visible / themeIds.size());
        int thumbY = listY + (listH - thumbH) * scrollOffset / maxOffset;
        ResourceLocation thumbTex = resolveTexture(ThemeKeys.Textures.SCROLL_SCROLLER);
        if (thumbTex != null) {
            TextureStretching.drawAutoNinePatch(thumbTex, trackX, thumbY, SCROLLBAR_W, thumbH, 6, 32, 1);
        }
    }

    // ──── Preview ────

    private void renderPreview(int mouseX, int mouseY) {
        FontRenderer font = getFont();
        font.drawStringWithShadow(tr("catframe.theme.preview", "Preview"), previewX, contentTop + 4, 0xA0A0A0);

        if (previewW < 40 || previewH < 40) {
            return;
        }

        Theme theme = selectedTheme();

        // 1px frame marking the preview as a standalone "screen".
        GuiDrawing.drawRect(previewX - 1, previewY - 1,
                previewX + previewW + 1, previewY + previewH + 1, 0x60FFFFFF);

        // Clip preview content to the frame so nothing bleeds out on small window sizes.
        Minecraft mc = getMinecraft();
        int guiScale = Math.max(1, mc.displayWidth / Math.max(1, width));
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(previewX * guiScale,
                mc.displayHeight - (previewY + previewH) * guiScale,
                previewW * guiScale, previewH * guiScale);
        try {
            drawPanelFor(theme, previewX, previewY, previewW, previewH - 2);
            drawPreviewContent(theme, mouseX, mouseY);
        } finally {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }

    /**
     * Draws a mock "sample screen" inside the preview, resolving every texture
     * and colour against the given theme (never the active one).
     * <p>在预览区绘制模拟“示例界面”，所有纹理与颜色均按给定主题解析（而非活动主题）。</p>
     */
    private void drawPreviewContent(@Nullable Theme theme, int mouseX, int mouseY) {
        FontRenderer font = getFont();
        int pad = 6;
        int left = previewX + pad;
        int right = previewX + previewW - pad;
        int innerW = right - left;

        // ── Mini title: the selected theme's display name ──
        String title = font.trimStringToWidth(
                theme != null ? theme.getName() : DefaultTheme.INSTANCE.getName(), innerW);
        font.drawStringWithShadow(title, previewX + (previewW - font.getStringWidth(title)) / 2,
                previewY + 5, colorFor(theme, ThemeKeys.Colors.BUTTON_TEXT_ENABLED, 0xE0E0E0));

        int y = previewY + 17;
        drawSeparatorFor(theme, previewX + 1, y, previewW - 2);
        y += 6;

        // ── Tab bar: background strip + three tabs (selected / hovered / normal) ──
        int tabH = 20;
        int tabW = Math.min(64, Math.max(20, (innerW - 4) / 3));
        GuiDrawing.drawRect(left, y, left + innerW, y + tabH + 3,
                colorFor(theme, ThemeKeys.Colors.TABBAR_BACKGROUND, 0xFF000000));

        String[] tabLabels = {
                tr("catframe.theme.preview.tab1", "General"),
                tr("catframe.theme.preview.tab2", "Display"),
                tr("catframe.theme.preview.tab3", "Audio")
        };
        ResourceLocation[] tabTextures = {
                texFor(theme, ThemeKeys.Textures.TAB_SELECTED),
                texFor(theme, ThemeKeys.Textures.TAB_HIGHLIGHTED),
                texFor(theme, ThemeKeys.Textures.TAB_NORMAL)
        };
        int[] tabColors = {
                colorFor(theme, ThemeKeys.Colors.TAB_TEXT_SELECTED, 0xFFFFFF),
                colorFor(theme, ThemeKeys.Colors.TAB_TEXT_HOVERED, 0xFFFF55),
                colorFor(theme, ThemeKeys.Colors.TAB_TEXT_NORMAL, 0xA0A0A0)
        };
        for (int i = 0; i < 3; i++) {
            int tabX = left + i * (tabW + 2);
            ResourceLocation tabTex = tabTextures[i];
            if (tabTex != null) {
                TextureStretching.drawNinePatch(tabTex, tabX, y, tabW, tabH, 2, 2, 2, 2, 130, 24);
                if (i == 0) {
                    // Selected tab covers the bar with the panel background (mirrors TabButton).
                    ResourceLocation panelBg = texFor(theme, ThemeKeys.Textures.PANEL_BACKGROUND);
                    if (panelBg != null) {
                        TextureStretching.drawTiled(panelBg, tabX + 2, y + 2, tabW - 4, tabH - 4, 16, 16);
                    }
                }
            }
            String tabLabel = font.trimStringToWidth(tabLabels[i], tabW - 4);
            font.drawStringWithShadow(tabLabel, tabX + (tabW - font.getStringWidth(tabLabel)) / 2,
                    y + (i == 0 ? 5 : 8), tabColors[i]);
        }
        y += tabH + 9;

        // ── Buttons: enabled (with live hover) + disabled ──
        int btnW = Math.max(50, (innerW - 6) / 2);
        boolean hoveredEnabled = isInside(mouseX, mouseY, left, y, btnW, BTN_H);
        drawButtonFor(theme, left, y, btnW, BTN_H, true, hoveredEnabled,
                tr("catframe.theme.preview.button", "Button"));
        drawButtonFor(theme, left + btnW + 6, y, btnW, BTN_H, false, false,
                tr("catframe.theme.preview.button_disabled", "Disabled"));
        y += BTN_H + 9;

        // ── Checkboxes: checked + unchecked ──
        drawCheckboxFor(theme, left, y, true, tr("catframe.theme.preview.checkbox_on", "Checked"));
        drawCheckboxFor(theme, left + btnW + 6, y, false,
                tr("catframe.theme.preview.checkbox_off", "Unchecked"));
        y += 22;

        // ── Toggles: on + off ──
        drawToggleFor(theme, left, y, btnW, true, tr("catframe.theme.preview.toggle_on", "On"));
        drawToggleFor(theme, left + btnW + 6, y, btnW, false, tr("catframe.theme.preview.toggle_off", "Off"));
        y += 22;

        // ── EditBox (focused state, hint text) ──
        ResourceLocation editTex = texFor(theme, ThemeKeys.Textures.EDITBOX_FOCUSED);
        if (editTex != null) {
            TextureStretching.drawAutoNinePatch(editTex, left, y, innerW, 20, 200, 20, 1);
        } else {
            GuiDrawing.drawRect(left, y, left + innerW, y + 20,
                    colorFor(theme, ThemeKeys.Colors.EDITBOX_BACKGROUND_FOCUSED, 0xFF333366));
        }
        font.drawStringWithShadow(tr("catframe.theme.preview.editbox", "Sample text"),
                left + 4, y + 6, 0xFFFFFF);

        // ── Toast — pinned to the preview's bottom-right corner ──
        int toastW = Math.min(150, innerW);
        int toastH = 32;
        int toastX = right - toastW;
        int toastY = previewY + previewH - 10 - toastH;
        ResourceLocation toastTex = texFor(theme, ThemeKeys.Textures.TOAST_DEFAULT);
        if (toastTex != null) {
            TextureStretching.drawAutoNinePatch(toastTex, toastX, toastY, toastW, toastH, 160, 32, 4);
        } else {
            GuiDrawing.drawRect(toastX, toastY, toastX + toastW, toastY + toastH,
                    colorFor(theme, ThemeKeys.Colors.TOAST_BACKGROUND, 0xCC000000));
        }
        font.drawString(tr("catframe.theme.preview.toast_title", "Toast"),
                toastX + 10, toastY + 5, 0xFFFFFF);
        font.drawString(tr("catframe.theme.preview.toast_text", "Notification preview"),
                toastX + 10, toastY + 5 + font.FONT_HEIGHT + 4, 0xAAAAAA);
    }

    // ──── Bottom bar ────

    private void renderBottomBar(int mouseX, int mouseY) {
        FontRenderer font = getFont();

        // Apply — disabled while the selection already matches the active theme.
        boolean canApply = canApply();
        boolean applyHovered = canApply && isInside(mouseX, mouseY, applyX, applyY, BTN_W, BTN_H);
        drawThemedButton(applyX, applyY, BTN_W, BTN_H, canApply, applyHovered);
        String applyLabel = tr("catframe.theme.apply", "Apply");
        font.drawStringWithShadow(applyLabel, applyX + (BTN_W - font.getStringWidth(applyLabel)) / 2,
                applyY + (BTN_H - font.FONT_HEIGHT) / 2, getButtonTextColor(canApply, applyHovered));

        // Done
        boolean doneHovered = isInside(mouseX, mouseY, doneX, applyY, BTN_W, BTN_H);
        drawThemedButton(doneX, applyY, BTN_W, BTN_H, true, doneHovered);
        String doneLabel = tr("catframe.theme.done", "Done");
        font.drawStringWithShadow(doneLabel, doneX + (BTN_W - font.getStringWidth(doneLabel)) / 2,
                applyY + (BTN_H - font.FONT_HEIGHT) / 2, getButtonTextColor(true, doneHovered));

        // Active theme status — bottom-left.
        Theme active = ThemeManager.getInstance().getActive();
        String status = tr("catframe.theme.active", "Active: %s",
                active != null ? active.getName() : "-");
        font.drawStringWithShadow(status, MARGIN, applyY + (BTN_H - font.FONT_HEIGHT) / 2, 0xA0A0A0);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Input
    // ══════════════════════════════════════════════════════════════════════

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0) {
            // Theme list rows
            if (isInList(mouseX, mouseY) && !themeIds.isEmpty()) {
                int index = scrollOffset + (mouseY - listY) / ROW_H;
                if (index >= 0 && index < themeIds.size() && index != selectedIndex) {
                    selectedIndex = index;
                    playThemeSound(ThemeKeys.Sounds.BUTTON_PRESS);
                }
            }
            // Apply
            if (canApply() && isInside(mouseX, mouseY, applyX, applyY, BTN_W, BTN_H)) {
                applySelected();
            }
            // Done
            if (isInside(mouseX, mouseY, doneX, applyY, BTN_W, BTN_H)) {
                playThemeSound(ThemeKeys.Sounds.BUTTON_PRESS);
                onClose();
                return;
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0 || themeIds.isEmpty()) {
            return;
        }
        Minecraft mc = getMinecraft();
        int mouseX = Mouse.getEventX() * width / Math.max(1, mc.displayWidth);
        int mouseY = height - Mouse.getEventY() * height / Math.max(1, mc.displayHeight) - 1;
        if (!isInList(mouseX, mouseY)) {
            return;
        }
        int visible = visibleRows();
        int maxOffset = Math.max(0, themeIds.size() - visible);
        scrollOffset = clamp(scrollOffset - (wheel > 0 ? 1 : -1), 0, maxOffset);
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (!themeIds.isEmpty()) {
            if (keyCode == Keyboard.KEY_UP) {
                if (selectedIndex > 0) {
                    selectedIndex--;
                    ensureVisible();
                    playThemeSound(ThemeKeys.Sounds.BUTTON_PRESS);
                }
                return;
            }
            if (keyCode == Keyboard.KEY_DOWN) {
                if (selectedIndex < themeIds.size() - 1) {
                    selectedIndex++;
                    ensureVisible();
                    playThemeSound(ThemeKeys.Sounds.BUTTON_PRESS);
                }
                return;
            }
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
                applySelected();
                return;
            }
        }
        super.keyTyped(typedChar, keyCode);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════════════════════════════

    /** @return the currently previewed theme, or {@code null} when none is available */
    @Nullable
    private Theme selectedTheme() {
        if (selectedIndex < 0 || selectedIndex >= themeIds.size()) {
            return null;
        }
        return ThemeManager.getInstance().getTheme(themeIds.get(selectedIndex));
    }

    private boolean canApply() {
        return selectedIndex >= 0 && selectedIndex < themeIds.size()
                && !themeIds.get(selectedIndex).equals(ThemeManager.getInstance().getActiveId());
    }

    /** Activate + persist the previewed theme, then play the press sound. */
    private void applySelected() {
        if (!canApply()) {
            return;
        }
        String id = themeIds.get(selectedIndex);
        ThemeManager.getInstance().setActive(id);
        CatFrameCompat.config.saveActiveTheme(id);
        // Played after activation so the sound resolves against the new theme.
        playThemeSound(ThemeKeys.Sounds.BUTTON_PRESS);
    }

    /** Keep the selected row inside the visible window. */
    private void ensureVisible() {
        int visible = visibleRows();
        if (selectedIndex < scrollOffset) {
            scrollOffset = selectedIndex;
        } else if (selectedIndex >= scrollOffset + visible) {
            scrollOffset = selectedIndex - visible + 1;
        }
        scrollOffset = clamp(scrollOffset, 0, Math.max(0, themeIds.size() - visible));
    }

    private int visibleRows() {
        return Math.max(1, listH / ROW_H);
    }

    private boolean isInList(int mouseX, int mouseY) {
        return isInside(mouseX, mouseY, listX, listY, listW, listH);
    }

    private static boolean isInside(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private static int clamp(int value, int min, int max) {
        return value < min ? min : (value > max ? max : value);
    }

    // ──── Themed drawing helpers ────

    /** Horizontal separator using the ACTIVE theme's footer separator texture. */
    private void drawSeparator(int x, int y, int w) {
        drawSeparatorFor(null, x, y, w);
    }

    /** Horizontal separator using an explicit theme's footer separator texture. */
    private void drawSeparatorFor(@Nullable Theme theme, int x, int y, int w) {
        ResourceLocation tex = texFor(theme, ThemeKeys.Textures.PANEL_FOOTER_SEPARATOR);
        if (tex != null) {
            TextureStretching.drawTiled(tex, x, y, w, 2, 32, 2);
        }
    }

    /** Panel background + header/footer separators resolved against an explicit theme. */
    private void drawPanelFor(@Nullable Theme theme, int x, int top, int width, int bottom) {
        ResourceLocation bg = texFor(theme, ThemeKeys.Textures.PANEL_BACKGROUND);
        ResourceLocation header = texFor(theme, ThemeKeys.Textures.PANEL_HEADER_SEPARATOR);
        ResourceLocation footer = texFor(theme, ThemeKeys.Textures.PANEL_FOOTER_SEPARATOR);

        int bgTop = top + 2;
        if (bottom > bgTop && bg != null) {
            TextureStretching.drawTiled(bg, x, bgTop, width, bottom - bgTop, 16, 16);
        }
        if (header != null) {
            TextureStretching.drawTiled(header, x, top, width, 2, 32, 2);
        }
        if (footer != null) {
            TextureStretching.drawTiled(footer, x, bottom, width, 2, 32, 2);
        }
    }

    private void drawButtonFor(@Nullable Theme theme, int x, int y, int w, int h,
                               boolean active, boolean hovered, String label) {
        ResourceLocation tex = texFor(theme, !active ? ThemeKeys.Textures.BUTTON_DISABLED
                : hovered ? ThemeKeys.Textures.BUTTON_HIGHLIGHTED : ThemeKeys.Textures.BUTTON_ENABLED);
        if (tex != null) {
            TextureStretching.drawAutoThreePatch(tex, x, y, w, h, 200, 20, 2);
        }
        FontRenderer font = getFont();
        int color = colorFor(theme, !active ? ThemeKeys.Colors.BUTTON_TEXT_DISABLED
                : hovered ? ThemeKeys.Colors.BUTTON_TEXT_HOVER : ThemeKeys.Colors.BUTTON_TEXT_ENABLED,
                0xE0E0E0);
        String text = font.trimStringToWidth(label, w - 6);
        font.drawStringWithShadow(text, x + (w - font.getStringWidth(text)) / 2,
                y + (h - font.FONT_HEIGHT) / 2, color);
    }

    private void drawCheckboxFor(@Nullable Theme theme, int x, int y, boolean checked, String label) {
        ResourceLocation tex = texFor(theme, checked
                ? ThemeKeys.Textures.CHECKBOX_CHECKED
                : ThemeKeys.Textures.CHECKBOX_NORMAL);
        if (tex != null) {
            TextureStretching.drawStatic(tex, x, y, 16, 16, 16, 16, 1.0F);
        }
        getFont().drawStringWithShadow(label, x + 20, y + 4,
                colorFor(theme, ThemeKeys.Colors.BUTTON_TEXT_ENABLED, 0xE0E0E0));
    }

    private void drawToggleFor(@Nullable Theme theme, int x, int y, int w, boolean on, String label) {
        FontRenderer font = getFont();
        font.drawStringWithShadow(label, x + 8, y + 3,
                colorFor(theme, ThemeKeys.Colors.BUTTON_TEXT_ENABLED, 0xE0E0E0));
        ResourceLocation tex = texFor(theme, on ? ThemeKeys.Textures.TOGGLE_TOGGLED
                : ThemeKeys.Textures.TOGGLE_NORMAL);
        if (tex != null) {
            TextureStretching.drawStatic(tex, x + w - 8 - 32, y, 32, 16, 16, 8, 1.0F);
        }
    }

    @Nullable
    private static ResourceLocation texFor(@Nullable Theme theme, String key) {
        return ThemeManager.getInstance().resolveTextureFor(theme, key);
    }

    private static int colorFor(@Nullable Theme theme, String key, int fallback) {
        Integer color = ThemeManager.getInstance().resolveColorFor(theme, key);
        return color != null ? color : fallback;
    }

    private static String tr(String key, String fallback, Object... args) {
        return Text.translatableWithFallback(key, fallback, args).getString();
    }
}
