package decok.dfcdvadstf.catframe.compact.notification;

import com.google.gson.JsonObject;
import decok.dfcdvadstf.catframe.CatFrameCompat;
import decok.dfcdvadstf.catframe.ui.extended.components.notifications.Notification;
import decok.dfcdvadstf.catframe.ui.extended.components.notifications.NotificationBase;

/**
 * <p>
 * Utility for parsing {@link NotificationBase} instances from JSON objects.<br>
 * Accepts both 6-digit ({@code #RRGGBB}) and 8-digit ({@code #AARRGGBB}) hex colour strings.
 * 6-digit values are treated as fully opaque (alpha = 0xFF).
 * </p>
 * <p>
 * 从 JSON 对象解析 {@link NotificationBase} 实例的工具类。<br>
 * 支持 6 位（{@code #RRGGBB}）和 8 位（{@code #AARRGGBB}）十六进制颜色字符串。
 * 6 位值视为完全不透明（alpha = 0xFF）。
 * </p>
 */
public final class NotificationJsonParser {

    private NotificationJsonParser() {
    }

    /**
     * Parse a {@link NotificationBase} from a JSON object.
     * Returns {@code null} and logs a warning if the required {@code "title"} field is missing.
     * <p>从 JSON 对象解析 {@link NotificationBase}。若缺少必需的 {@code "title"} 字段则返回
     * {@code null} 并记录警告。</p>
     *
     * @param obj the JSON object to parse / 要解析的 JSON 对象
     * @return the parsed notification, or {@code null} on failure / 解析结果，失败时为 {@code null}
     */
    public static NotificationBase parse(JsonObject obj) {
        if (!obj.has("title")) {
            CatFrameCompat.logger.warn("[Notification] Entry missing 'title', skipping.");
            return null;
        }

        String title = obj.get("title").getAsString();
        String message = getString(obj, "message", "");
        float durationSec = getFloat(obj, "duration", 5.0F);
        int durationTicks = Math.max(1, (int) (durationSec * 20));

        int titleColor = parseColor(obj, "title_color", Notification.DEFAULT_TITLE_COLOR);
        int messageColor = parseColor(obj, "message_color", Notification.DEFAULT_MESSAGE_COLOR);
        int backgroundColor = parseColor(obj, "background_color", Notification.DEFAULT_BACKGROUND_COLOR);
        int borderColor = parseColor(obj, "border_color", Notification.DEFAULT_BORDER_COLOR);

        return NotificationBase.builder(title, message)
                .duration(durationTicks)
                .titleColor(titleColor)
                .messageColor(messageColor)
                .backgroundColor(backgroundColor)
                .borderColor(borderColor)
                .build();
    }

    /**
     * Returns the string value of {@code key}, or {@code def} if absent.
     * <p>返回 {@code key} 的字符串值，缺失时返回 {@code def}。</p>
     */
    public static String getString(JsonObject obj, String key, String def) {
        return obj.has(key) ? obj.get(key).getAsString() : def;
    }

    /**
     * Returns the float value of {@code key}, or {@code def} if absent.
     * <p>返回 {@code key} 的浮点值，缺失时返回 {@code def}。</p>
     */
    public static float getFloat(JsonObject obj, String key, float def) {
        return obj.has(key) ? obj.get(key).getAsFloat() : def;
    }

    /**
     * Parse an ARGB colour from a hex string field.
     * Returns {@code def} if the field is absent or the value is malformed.
     * <p>从十六进制字符串字段解析 ARGB 颜色。字段缺失或值格式错误时返回 {@code def}。</p>
     */
    public static int parseColor(JsonObject obj, String key, int def) {
        if (!obj.has(key)) return def;
        String raw = obj.get(key).getAsString().replace("#", "").trim();
        try {
            long v = Long.parseLong(raw, 16);
            if (raw.length() <= 6) v |= 0xFF000000L;
            return (int) v;
        } catch (NumberFormatException e) {
            CatFrameCompat.logger.warn("[Notification] Invalid color '{}' for '{}', using default.", raw, key);
            return def;
        }
    }
}
