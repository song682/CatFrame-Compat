package decok.dfcdvadstf.catframe.ui.extended.notifications;

import decok.dfcdvadstf.catframe.ui.GuiDrawing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;

import java.util.List;

/**
 * <p>
 * Renders active notifications as stacked cards in the top-right corner of the screen.<br>
 * Each card contains:
 * </p>
 * <ul>
 *   <li>A colored left accent border.</li>
 *   <li>The notification title and a countdown timer (top row).</li>
 *   <li>The description text — wraps to a second line if needed, with a {@code -} continuation marker.</li>
 *   <li>A progress bar at the bottom that drains as time runs out.</li>
 * </ul>
 * <p>
 * Cards slide in and out from the right edge using an ease-out cubic curve.
 * The background color is fully customizable per notification; the default is pure black.
 * </p>
 * <p>
 * 将活跃通知渲染为右上角堆叠的卡片。<br>
 * 每张卡片包含：
 * </p>
 * <ul>
 *   <li>彩色左侧强调边框。</li>
 *   <li>通知标题与倒计时（顶行）。</li>
 *   <li>描述文本 —— 超出宽度时自动换行至第二行，带 {@code -} 续行标记。</li>
 *   <li>底部进度条，随时间流逝而缩短。</li>
 * </ul>
 * <p>
 * 卡片以 ease-out cubic 曲线从右侧滑入/滑出。背景颜色可按通知自定义；默认为纯黑色。
 * </p>
 */
public final class NotificationRenderer {

    // ──── Layout constants ────

    /** Card width in pixels. / 卡片宽度（像素）。 */
    public static final int W = 210;

    /** Screen margin. / 屏幕边距。 */
    public static final int MARGIN = 6;

    /** Gap between stacked cards. / 堆叠卡片之间的间距。 */
    public static final int GAP = 4;

    /** Left accent border width. / 左侧强调边框宽度。 */
    public static final int BORDER_W = 3;

    /** Horizontal padding inside the card. / 卡片内部水平内边距。 */
    public static final int PAD_H = 8;

    /** Vertical padding inside the card. / 卡片内部垂直内边距。 */
    public static final int PAD_V = 8;

    /** Progress bar height. / 进度条高度。 */
    public static final int BAR_H = 3;

    /** Card height when the description fits on one line. / 描述为一行时的卡片高度。 */
    public static final int H_ONE_LINE = 50;

    /** Card height when the description wraps to two lines. / 描述换行为两行时的卡片高度。 */
    public static final int H_TWO_LINE = 62;

    private NotificationRenderer() {
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Public entry point
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Render all active notifications. Called from the overlay render path.
     * <p>渲染所有活跃通知。由 Overlay 渲染路径调用。</p>
     *
     * @param notifications list of active notifications / 活跃通知列表
     */
    public static void render(List<ActiveNotification> notifications) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.gameSettings.hideGUI) {
            return;
        }

        FontRenderer font = mc.fontRenderer;
        if (notifications.isEmpty()) {
            return;
        }

        ScaledResolution sr = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        int screenW = sr.getScaledWidth();
        int topY = MARGIN;

        for (ActiveNotification active : notifications) {
            float slide = easeOutCubic(active.getSlideProgress());
            float visible = active.getVisibleProgress();

            int xOffset = (int) ((1.0F - slide) * (W + MARGIN));
            int x = screenW - W - MARGIN + xOffset;

            Notification n = active.getNotification();
            int cardH = renderCard(font, n, x, topY, visible);
            topY += cardH + GAP;
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Card rendering
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Render a single notification card and return the height it occupied.
     * <p>渲染单条通知卡片并返回其占用的高度。</p>
     */
    private static int renderCard(FontRenderer font, Notification n,
                                  int x, int y, float visibleProgress) {
        int descMaxWidth = W - BORDER_W - PAD_H * 2;
        String[] descLines = wrapDescription(font, n.getMessage(), descMaxWidth);
        int cardH = descLines.length > 1 ? H_TWO_LINE : H_ONE_LINE;

        // ── Background and left accent border ──
        GuiDrawing.drawRect(x, y, x + W, y + cardH, n.getBackgroundColor());
        GuiDrawing.drawRect(x, y, x + BORDER_W, y + cardH, n.getBorderColor());

        // ── Title + countdown timer on the same row ──
        int textX = x + BORDER_W + PAD_H;

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        String titleStr = truncate(font, n.getTitle(), W - BORDER_W - PAD_H * 2 - 30);
        font.drawStringWithShadow(titleStr, textX, y + PAD_V, n.titleColor());

        int remainSecs = Math.max(0, Math.round((n.getDuration() * visibleProgress) / 20.0F));
        String timerText = remainSecs + "s";
        font.drawStringWithShadow(timerText,
                x + W - PAD_H - font.getStringWidth(timerText),
                y + PAD_V,
                n.titleColor());

        // ── Description — one or two lines ──
        int descY = y + PAD_V + font.FONT_HEIGHT + 3;
        font.drawStringWithShadow(descLines[0], textX, descY, n.messageColor());

        if (descLines.length > 1) {
            font.drawStringWithShadow(descLines[1],
                    textX, descY + font.FONT_HEIGHT + 2, n.messageColor());
        }

        // ── Progress bar ──
        int barY = y + cardH - BAR_H;
        GuiDrawing.drawRect(x, barY, x + W, y + cardH, darken(n.getBorderColor(), 0.35F));

        int barFill = (int) (W * visibleProgress);
        if (barFill > 0) {
            GuiDrawing.drawRect(x, barY, x + barFill, y + cardH, n.getBorderColor());
        }

        return cardH;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Text utilities
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Split {@code text} into at most two display lines for the given {@code maxWidth}.
     * <p>将 {@code text} 拆分为最多两行显示文本，适配给定 {@code maxWidth}。</p>
     *
     * @return a 1- or 2-element array of ready-to-render strings
     */
    private static String[] wrapDescription(FontRenderer font, String text, int maxWidth) {
        if (text == null || text.isEmpty()) {
            return new String[]{""};
        }
        if (font.getStringWidth(text) <= maxWidth) {
            return new String[]{text};
        }

        // Find how much fits on line 1 with the "-" marker reserved
        int hyphenWidth = font.getStringWidth("-");
        String fitted = font.trimStringToWidth(text, maxWidth - hyphenWidth);

        // Prefer breaking at a word boundary
        int lastSpace = fitted.lastIndexOf(' ');
        String line1 = lastSpace > 0 ? fitted.substring(0, lastSpace) : fitted;
        String remaining = text.substring(line1.length()).trim();

        return new String[]{
                line1 + "-",
                truncate(font, remaining, maxWidth)
        };
    }

    /**
     * Truncate {@code text} with {@code ...} if it exceeds {@code maxWidth}.
     * <p>若 {@code text} 超出 {@code maxWidth} 则以 {@code ...} 截断。</p>
     */
    private static String truncate(FontRenderer font, String text, int maxWidth) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (font.getStringWidth(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        int limit = maxWidth - font.getStringWidth(ellipsis);
        return font.trimStringToWidth(text, limit) + ellipsis;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Math utilities
    // ══════════════════════════════════════════════════════════════════════

    /** Ease-out cubic curve: {@code 1 - (1-t)^3}. / ease-out cubic 曲线。 */
    private static float easeOutCubic(float t) {
        return 1.0F - (float) Math.pow(1.0 - t, 3.0);
    }

    /**
     * Darken an ARGB color by the given factor (0 = black, 1 = unchanged).
     * <p>以给定因子加深 ARGB 颜色（0 = 黑色，1 = 不变）。</p>
     */
    private static int darken(int argb, float factor) {
        int a = (argb >> 24) & 0xFF;
        int r = (int) (((argb >> 16) & 0xFF) * factor);
        int g = (int) (((argb >> 8) & 0xFF) * factor);
        int b = (int) ((argb & 0xFF) * factor);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
