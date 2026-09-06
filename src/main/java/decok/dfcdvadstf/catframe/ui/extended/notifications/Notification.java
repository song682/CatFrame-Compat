package decok.dfcdvadstf.catframe.ui.extended.notifications;

/**
 * <p>
 * Notification data interface — describes a HUD notification card's mutable properties:
 * title, message, colours, background, border, and display duration.<br>
 * The canonical implementation is {@link AbstractNotification}, which also provides a
 * fluent {@link AbstractNotification.Builder}.
 * </p>
 * <p>
 * 通知数据接口 —— 描述 HUD 通知卡片的可变属性：标题、消息、颜色、背景、边框与显示时长。<br>
 * 规范实现为 {@link AbstractNotification}，同时提供流利的 {@link AbstractNotification.Builder}。
 * </p>
 *
 * <h3>Usage / 用法</h3>
 * <pre>{@code
 * AbstractNotification n = AbstractNotification.builder("Title", "Message body")
 *         .duration(100)
 *         .borderColor(0xFFFF4444)
 *         .build();
 *
 * NotificationManager.show(n);
 * }</pre>
 */
public interface Notification {

    // ──── Default colour constants ────

    /** Default title colour: opaque white ({@code #FFFFFF}). / 默认标题颜色：不透明白色。 */
    int DEFAULT_TITLE_COLOR = 0xFFFFFFFF;

    /** Default message colour: opaque light gray ({@code #E0E0E0}). / 默认消息颜色：不透明浅灰色。 */
    int DEFAULT_MESSAGE_COLOR = 0xFFE0E0E0;

    /** Default background colour: pure black ({@code #FF000000}). / 默认背景颜色：纯黑色。 */
    int DEFAULT_BACKGROUND_COLOR = 0xFF000000;

    /** Default border / accent colour: dark gray ({@code #FF555555}). / 默认边框/强调颜色：深灰色。 */
    int DEFAULT_BORDER_COLOR = 0xFF555555;

    /** Default display duration in ticks (100 ticks = 5 s). / 默认显示时长（100 tick = 5 秒）。 */
    int DEFAULT_DURATION = 100;

    // ──── Accessors ────

    /** @return the notification title / 通知标题 */
    String getTitle();

    /** Set the notification title. / 设置通知标题。 */
    void setTitle(String title);

    /** @return the notification body text / 通知正文 */
    String getMessage();

    /** Set the notification body text. / 设置通知正文。 */
    void setMessage(String message);

    /** @return title text colour as ARGB / 标题文本颜色（ARGB） */
    int titleColor();

    /** Set the title text colour. / 设置标题文本颜色。 */
    void setTitleColor(int titleColor);

    /** @return message text colour as ARGB / 消息文本颜色（ARGB） */
    int messageColor();

    /** Set the message text colour. / 设置消息文本颜色。 */
    void setMessageColor(int messageColor);

    /** @return unique identifier, or {@code null} / 唯一标识符，或 {@code null} */
    String getID();

    /** Set the unique identifier. / 设置唯一标识符。 */
    void setID(String id);

    /** @return card background colour as ARGB / 卡片背景颜色（ARGB） */
    int getBackgroundColor();

    /** Set the card background colour. / 设置卡片背景颜色。 */
    void setBackgroundColor(int backgroundColor);

    /** @return left accent border and progress bar colour as ARGB / 左侧强调边框与进度条颜色（ARGB） */
    int getBorderColor();

    /** Set the left accent border and progress bar colour. / 设置左侧强调边框与进度条颜色。 */
    void setBorderColor(int borderColor);

    /** @return display duration in ticks (20 ticks = 1 s) / 显示时长（tick 数，20 tick = 1 秒） */
    int getDuration();

    /** Set the display duration in ticks. / 设置显示时长（tick 数）。 */
    void setDuration(int duration);
}
