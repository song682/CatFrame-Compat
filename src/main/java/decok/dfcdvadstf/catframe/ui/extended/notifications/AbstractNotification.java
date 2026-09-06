package decok.dfcdvadstf.catframe.ui.extended.notifications;

import decok.dfcdvadstf.catframe.ui.components.AbstractComponent;
import decok.dfcdvadstf.catframe.ui.overlay.Overlay;
import decok.dfcdvadstf.catframe.ui.overlay.OverlayContext;
import decok.dfcdvadstf.catframe.ui.overlay.ScreenAnchor;

/**
 * <p>
 * Concrete, mutable {@link Notification} implementation with a fluent {@link Builder}.<br>
 * Also implements {@link Overlay} for type compatibility with the CatFrame overlay system;
 * however, notifications are not individually registered with the {@code OverlayManager}.
 * Instead, the {@link NotificationManager} (itself a single Overlay) manages their lifecycle
 * and delegates rendering to {@link NotificationRenderer}.
 * </p>
 * <p>
 * 具体、可变的 {@link Notification} 实现，附带流利的 {@link Builder}。<br>
 * 同时实现 {@link Overlay} 以保持与 CatFrame Overlay 系统的类型兼容；但通知不会逐个注册到
 * {@code OverlayManager}。{@link NotificationManager}（自身为单个 Overlay）统一管理其生命周期，
 * 并将渲染委托给 {@link NotificationRenderer}。
 * </p>
 *
 * <h3>Usage / 用法</h3>
 * <pre>{@code
 * AbstractNotification n = AbstractNotification.builder("Title", "Body text")
 *         .duration(100)
 *         .borderColor(0xFFFF4444)
 *         .backgroundColor(0xFF000000)
 *         .build();
 *
 * NotificationManager.show(n);
 * }</pre>
 */
public class AbstractNotification extends AbstractComponent implements Notification, Overlay {

    // ──── Default colour constants (mirrors Notification interface) ────

    /** Default title colour: opaque white. / 默认标题颜色：不透明白色。 */
    public static final int DEF_TITLE_COLOR = Notification.DEFAULT_TITLE_COLOR;

    /** Default message colour: opaque light gray. / 默认消息颜色：不透明浅灰色。 */
    public static final int DEF_MESSAGE_COLOR = Notification.DEFAULT_MESSAGE_COLOR;

    /** Default background colour: pure black. / 默认背景颜色：纯黑色。 */
    public static final int DEF_BACKGROUND_COLOR = Notification.DEFAULT_BACKGROUND_COLOR;

    /** Default border / accent colour: dark gray. / 默认边框/强调颜色：深灰色。 */
    public static final int DEF_BORDER_COLOR = Notification.DEFAULT_BORDER_COLOR;

    /** Default display duration in ticks (5 s). / 默认显示时长（5 秒）。 */
    public static final int DEF_DURATION = Notification.DEFAULT_DURATION;

    // ──── Fields ────

    private String title = "";
    private String message = "";
    private int titleColor = DEF_TITLE_COLOR;
    private int messageColor = DEF_MESSAGE_COLOR;
    private int backgroundColor = DEF_BACKGROUND_COLOR;
    private int borderColor = DEF_BORDER_COLOR;
    private int duration = DEF_DURATION;
    private String id;

    // ──── Constructors ────

    /** Creates a notification with all defaults. Use setters or the {@link Builder} to configure.
     *  <p>使用默认值创建通知。通过 setter 或 {@link Builder} 配置。</p> */
    public AbstractNotification() {
    }

    /** Creates a notification with the given title and message.
     *  <p>使用给定标题和消息创建通知。</p> */
    public AbstractNotification(String title, String message) {
        this.title = title != null ? title : "";
        this.message = message != null ? message : "";
    }

    // ──── Builder entry point ────

    /**
     * Create a new {@link Builder} with the required title and message.
     * <p>以必需的标题和消息创建新的 {@link Builder}。</p>
     */
    public static Builder builder(String title, String message) {
        return new Builder(title, message);
    }

    // ──── Notification implementation ────

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void setTitle(String title) {
        this.title = title != null ? title : "";
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public void setMessage(String message) {
        this.message = message != null ? message : "";
    }

    @Override
    public int titleColor() {
        return titleColor;
    }

    @Override
    public void setTitleColor(int titleColor) {
        this.titleColor = titleColor;
    }

    @Override
    public int messageColor() {
        return messageColor;
    }

    @Override
    public void setMessageColor(int messageColor) {
        this.messageColor = messageColor;
    }

    @Override
    public String getID() {
        return id;
    }

    @Override
    public void setID(String id) {
        this.id = id;
    }

    @Override
    public int getBackgroundColor() {
        return backgroundColor;
    }

    @Override
    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    @Override
    public int getBorderColor() {
        return borderColor;
    }

    @Override
    public void setBorderColor(int borderColor) {
        this.borderColor = borderColor;
    }

    @Override
    public int getDuration() {
        return duration;
    }

    @Override
    public void setDuration(int duration) {
        this.duration = duration;
    }

    // ──── Overlay implementation ────
    // AbstractNotification implements Overlay for type compatibility.
    // It is NOT individually registered with OverlayManager; NotificationManager
    // is the single Overlay that manages all active notifications.

    @Override
    public ScreenAnchor getAnchor() {
        return ScreenAnchor.TOP_RIGHT;
    }

    @Override
    public int getOffsetX() {
        return 0;
    }

    @Override
    public int getOffsetY() {
        return 0;
    }

    @Override
    public OverlayContext getContext() {
        return OverlayContext.BOTH;
    }

    @Override
    public void update() {
        // Lifecycle managed by NotificationManager / 生命周期由 NotificationManager 管理
    }

    // ──── AbstractComponent overrides ────

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Builder
    // ══════════════════════════════════════════════════════════════════════

    /**
     * <p>
     * Fluent builder for {@link AbstractNotification}. All colour and duration fields
     * are optional and fall back to the {@link Notification} interface defaults.
     * </p>
     * <p>
     * {@link AbstractNotification} 的流利构建器。所有颜色和时长字段均为可选，
     * 回退到 {@link Notification} 接口的默认值。
     * </p>
     */
    public static final class Builder {

        private final String title;
        private final String message;
        private int titleColor = DEF_TITLE_COLOR;
        private int messageColor = DEF_MESSAGE_COLOR;
        private int backgroundColor = DEF_BACKGROUND_COLOR;
        private int borderColor = DEF_BORDER_COLOR;
        private int duration = DEF_DURATION;
        private String id;

        private Builder(String title, String message) {
            this.title = title != null ? title : "";
            this.message = message != null ? message : "";
        }

        /** Set the title text colour (ARGB). / 设置标题文本颜色（ARGB）。 */
        public Builder titleColor(int color) {
            this.titleColor = color;
            return this;
        }

        /** Set the message text colour (ARGB). / 设置消息文本颜色（ARGB）。 */
        public Builder messageColor(int color) {
            this.messageColor = color;
            return this;
        }

        /** Set the card background colour (ARGB). Default: pure black.
         *  <p>设置卡片背景颜色（ARGB）。默认：纯黑色。</p> */
        public Builder backgroundColor(int color) {
            this.backgroundColor = color;
            return this;
        }

        /** Set the left accent border and progress bar colour (ARGB).
         *  <p>设置左侧强调边框与进度条颜色（ARGB）。</p> */
        public Builder borderColor(int color) {
            this.borderColor = color;
            return this;
        }

        /** Set the display duration in ticks (20 ticks = 1 s).
         *  <p>设置显示时长（tick 数，20 tick = 1 秒）。</p> */
        public Builder duration(int ticks) {
            this.duration = ticks;
            return this;
        }

        /** Set the display duration in seconds (converted to ticks automatically).
         *  <p>设置显示时长（秒数，自动转换为 tick）。</p> */
        public Builder durationSeconds(float seconds) {
            this.duration = Math.max(1, (int) (seconds * 20));
            return this;
        }

        /** Set the unique identifier. / 设置唯一标识符。 */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /** Build and return the immutable-configured {@link AbstractNotification}.
         *  <p>构建并返回配置完成的 {@link AbstractNotification}。</p> */
        public AbstractNotification build() {
            AbstractNotification n = new AbstractNotification(title, message);
            n.titleColor = this.titleColor;
            n.messageColor = this.messageColor;
            n.backgroundColor = this.backgroundColor;
            n.borderColor = this.borderColor;
            n.duration = this.duration;
            n.id = this.id;
            return n;
        }
    }
}
