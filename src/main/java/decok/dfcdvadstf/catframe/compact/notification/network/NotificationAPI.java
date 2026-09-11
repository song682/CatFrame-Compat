package decok.dfcdvadstf.catframe.compact.notification.network;

import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import decok.dfcdvadstf.catframe.ui.extended.components.notifications.NotificationBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

/**
 * <p>
 * Server-side API for sending notifications to players.<br>
 * Other mods (or server logic) call {@link #sendTo} or {@link #broadcast} to push
 * a notification to connected clients.
 * </p>
 * <p>
 * 服务端 API —— 向玩家发送通知。<br>
 * 其他模组（或服务端逻辑）调用 {@link #sendTo} 或 {@link #broadcast} 将通知推送到客户端。
 * </p>
 *
 * <h3>Usage / 用法</h3>
 * <pre>{@code
 * // Send to a single player:
 * NotificationAPI.sendTo(player, "Event", "The PvP event has begun!", 100,
 *         0xFFFFFF55, 0xFFFFFFFF, 0xFF000000, 0xFFFF4444);
 *
 * // Broadcast to all online players:
 * NotificationAPI.broadcast(server, "Server", "Restarting in 5 minutes.", 200,
 *         0xFFFFFF55, 0xFFFFFFFF, 0xFF000000, 0xFFFF4444);
 * }</pre>
 *
 * <p>
 * This class is safe to load on the dedicated server — it only references
 * {@link EntityPlayerMP}, {@link MinecraftServer}, and the network channel,
 * none of which are client-only.
 * </p>
 */
public final class NotificationAPI {

    private NotificationAPI() {
    }

    /**
     * Send a notification to a single player.
     * <p>向单个玩家发送通知。</p>
     *
     * @param player          the target player / 目标玩家
     * @param title           notification title / 通知标题
     * @param message         notification body / 通知正文
     * @param duration        display duration in ticks (20 ticks = 1 s) / 显示时长（tick）
     * @param titleColor      title text colour (ARGB) / 标题颜色
     * @param messageColor    message text colour (ARGB) / 消息颜色
     * @param backgroundColor card background colour (ARGB) / 卡片背景颜色
     * @param borderColor     left accent border colour (ARGB) / 左侧强调边框颜色
     */
    public static void sendTo(EntityPlayerMP player,
                              String title, String message, int duration,
                              int titleColor, int messageColor,
                              int backgroundColor, int borderColor) {
        SimpleNetworkWrapper channel = NotificationNetwork.CHANNEL;
        if (channel == null || player == null) {
            return;
        }
        channel.sendTo(
                new NotificationPacket(title, message, duration,
                        titleColor, messageColor, backgroundColor, borderColor),
                player);
    }

    /**
     * Send a notification to a single player using a pre-built {@link NotificationBase}.
     * <p>使用预构建的 {@link NotificationBase} 向单个玩家发送通知。</p>
     */
    public static void sendTo(EntityPlayerMP player, NotificationBase notification) {
        sendTo(player,
                notification.getTitle(), notification.getMessage(), notification.getDuration(),
                notification.titleColor(), notification.messageColor(),
                notification.getBackgroundColor(), notification.getBorderColor());
    }

    /**
     * Send a notification to a player by name. No-op if the player is not online.
     * <p>按名称向玩家发送通知。玩家不在线时无操作。</p>
     */
    public static void sendTo(MinecraftServer server, String playerName, NotificationBase notification) {
        if (server == null || playerName == null || notification == null) return;
        for (Object obj : server.getConfigurationManager().playerEntityList) {
            if (obj instanceof EntityPlayerMP) {
                EntityPlayerMP mp = (EntityPlayerMP) obj;
                if (mp.getCommandSenderName().equalsIgnoreCase(playerName)) {
                    sendTo(mp, notification);
                    return;
                }
            }
        }
    }

    /**
     * Broadcast a notification to all online players.
     * <p>向所有在线玩家广播通知。</p>
     *
     * @param server          the running server instance / 当前运行的服务器实例
     * @param title           notification title / 通知标题
     * @param message         notification body / 通知正文
     * @param duration        display duration in ticks / 显示时长（tick）
     * @param titleColor      title text colour (ARGB) / 标题颜色
     * @param messageColor    message text colour (ARGB) / 消息颜色
     * @param backgroundColor card background colour (ARGB) / 卡片背景颜色
     * @param borderColor     left accent border colour (ARGB) / 左侧强调边框颜色
     */
    public static void broadcast(MinecraftServer server,
                                 String title, String message, int duration,
                                 int titleColor, int messageColor,
                                 int backgroundColor, int borderColor) {
        SimpleNetworkWrapper channel = NotificationNetwork.CHANNEL;
        if (channel == null || server == null) {
            return;
        }
        NotificationPacket packet = new NotificationPacket(
                title, message, duration,
                titleColor, messageColor, backgroundColor, borderColor);
        for (Object obj : server.getConfigurationManager().playerEntityList) {
            if (obj instanceof EntityPlayerMP) {
                channel.sendTo(packet, (EntityPlayerMP) obj);
            }
        }
    }

    /**
     * Broadcast a pre-built notification to all online players.
     * <p>使用预构建的 {@link NotificationBase} 向所有在线玩家广播通知。</p>
     */
    public static void broadcast(MinecraftServer server, NotificationBase notification) {
        broadcast(server,
                notification.getTitle(), notification.getMessage(), notification.getDuration(),
                notification.titleColor(), notification.messageColor(),
                notification.getBackgroundColor(), notification.getBorderColor());
    }
}
