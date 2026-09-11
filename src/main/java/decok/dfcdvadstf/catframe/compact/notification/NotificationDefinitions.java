package decok.dfcdvadstf.catframe.compact.notification;

import com.google.gson.*;
import decok.dfcdvadstf.catframe.CatFrameCompat;
import decok.dfcdvadstf.catframe.ui.extended.components.notifications.NotificationBase;
import net.minecraft.server.MinecraftServer;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

/**
 * <p>
 * Loads and caches named notification definitions from {@code catframenotifications.json}
 * in the server's root directory.<br>
 * Definitions are referenced by ID in the {@code /notification} command.
 * The file is created automatically with a sample entry on first run.
 * </p>
 * <p>
 * 从服务端根目录的 {@code catframenotifications.json} 加载并缓存命名通知定义。<br>
 * 定义通过 ID 在 {@code /notification} 命令中引用。首次运行时自动创建含示例条目的文件。
 * </p>
 *
 * <h3>Example {@code catframenotifications.json}</h3>
 * <pre>{@code
 * {
 *   "restart_warning": {
 *     "title": "Server Restart",
 *     "message": "The server restarts in 5 minutes.",
 *     "duration": 8,
 *     "title_color": "#FFFF55",
 *     "message_color": "#FFFFFF",
 *     "background_color": "#FF000000",
 *     "border_color": "#FFFF55"
 *   }
 * }
 * }</pre>
 */
public final class NotificationDefinitions {

    private static final String FILE_NAME = "catframenotifications.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Charset UTF8 = Charset.forName("UTF-8");

    private static final Map<String, NotificationBase> definitions = new LinkedHashMap<>();

    private NotificationDefinitions() {
    }

    /**
     * Clear the cache and reload all definitions from disk.
     * <p>清空缓存并从磁盘重新加载所有定义。</p>
     *
     * @param server the running server instance (used to resolve the file path)
     *               / 运行中的服务器实例（用于解析文件路径）
     */
    public static void reload(MinecraftServer server) {
        definitions.clear();
        File file = server.getFile(FILE_NAME);

        if (!file.exists()) {
            createDefaultFile(file);
            return;
        }

        try {
            String raw = readFile(file).trim();
            if (raw.isEmpty()) return;

            JsonObject root = new JsonParser().parse(raw).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                try {
                    NotificationBase n = NotificationJsonParser.parse(entry.getValue().getAsJsonObject());
                    if (n != null) {
                        definitions.put(entry.getKey(), n);
                    }
                } catch (Exception e) {
                    CatFrameCompat.logger.warn("[Notification] Skipping definition '{}': {}",
                            entry.getKey(), e.getMessage());
                }
            }
            CatFrameCompat.logger.info("[Notification] Loaded {} definition(s) from {}",
                    definitions.size(), FILE_NAME);

        } catch (Exception e) {
            CatFrameCompat.logger.error("[Notification] Failed to load {}: {}", FILE_NAME, e.getMessage());
        }
    }

    /**
     * Returns the notification registered under the given ID, or {@code null} if not found.
     * <p>返回以给定 ID 注册的通知，未找到时返回 {@code null}。</p>
     */
    public static NotificationBase get(String id) {
        return definitions.get(id);
    }

    /**
     * Returns an unmodifiable view of all currently loaded definition IDs.
     * Used to power tab-completion in {@code /notification send}.
     * <p>返回所有已加载定义 ID 的只读视图。用于 {@code /notification send} 的 Tab 补全。</p>
     */
    public static Set<String> getIds() {
        return Collections.unmodifiableSet(definitions.keySet());
    }

    // ──── File I/O helpers (1.7.10 compatible) ────

    private static String readFile(File file) throws IOException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), UTF8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (sb.length() > 0) sb.append('\n');
                sb.append(line);
            }
            return sb.toString();
        } finally {
            if (reader != null) {
                try { reader.close(); } catch (IOException ignored) { }
            }
        }
    }

    private static void writeFile(File file, String content) throws IOException {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), UTF8));
            writer.write(content);
        } finally {
            if (writer != null) {
                try { writer.close(); } catch (IOException ignored) { }
            }
        }
    }

    private static void createDefaultFile(File file) {
        JsonObject root = new JsonObject();
        JsonObject example = new JsonObject();
        example.addProperty("title", "Example Notification");
        example.addProperty("message", "This is a sample notification from CatFrameCompact.");
        example.addProperty("duration", 5.0F);
        example.addProperty("title_color", "#FFFFFF");
        example.addProperty("message_color", "#E0E0E0");
        example.addProperty("background_color", "#FF000000");
        example.addProperty("border_color", "#555555");
        root.add("example", example);

        try {
            writeFile(file, GSON.toJson(root));
            CatFrameCompat.logger.info("[Notification] Created default {} in server root.", FILE_NAME);
        } catch (Exception e) {
            CatFrameCompat.logger.error("[Notification] Could not create default {}: {}",
                    FILE_NAME, e.getMessage());
        }
    }
}
