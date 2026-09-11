package decok.dfcdvadstf.catframe.compact.notification.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import decok.dfcdvadstf.catframe.ui.extended.components.notifications.NotificationBase;
import decok.dfcdvadstf.catframe.ui.extended.components.notifications.NotificationManager;

/**
 * <p>
 * Client-side handler for {@link NotificationPacket}. Receives notification data
 * from the server, constructs a {@link NotificationBase}, and enqueues it via
 * {@link NotificationManager#show}.
 * </p>
 * <p>
 * {@link NotificationPacket} 的客户端处理器。从服务端接收通知数据，
 * 构建 {@link NotificationBase} 并通过 {@link NotificationManager#show} 入队显示。
 * </p>
 *
 * <p>
 * This class is {@code @SideOnly(Side.CLIENT)} because it references
 * {@link NotificationManager} which is itself client-only. It is never loaded
 * on the dedicated server.
 * </p>
 */
@SideOnly(Side.CLIENT)
public class NotificationPacketHandler implements IMessageHandler<NotificationPacket, IMessage> {

    @Override
    public IMessage onMessage(NotificationPacket packet, MessageContext ctx) {
        // In 1.7.10 MCP, Minecraft has no addScheduledTask(). Direct call is safe
        // here: show() only enqueues to a Deque; rendering occurs on the main thread
        // during the next frame via OverlayManager.
        NotificationBase n = new NotificationBase(packet.getTitle(), packet.getMessage());
        n.setDuration(packet.getDuration());
        n.setTitleColor(packet.getTitleColor());
        n.setMessageColor(packet.getMessageColor());
        n.setBackgroundColor(packet.getBackgroundColor());
        n.setBorderColor(packet.getBorderColor());
        NotificationManager.show(n);
        return null;
    }
}
