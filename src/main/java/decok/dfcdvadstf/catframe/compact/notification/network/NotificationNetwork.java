package decok.dfcdvadstf.catframe.compact.notification.network;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import decok.dfcdvadstf.catframe.compact.Tags;

/**
 * <p>
 * Holds the {@link SimpleNetworkWrapper} channel used by the notification system.<br>
 * Call {@link #init()} once during pre-init (common proxy) to register the channel
 * and the server→client packet discriminator.
 * </p>
 * <p>
 * 持有通知系统使用的 {@link SimpleNetworkWrapper} 通道。<br>
 * 在 preInit（common proxy）期间调用一次 {@link #init()} 以注册通道和服务端→客户端包鉴别器。
 * </p>
 */
public final class NotificationNetwork {

    /** The network channel for notification packets. / 通知包的网络通道。 */
    public static SimpleNetworkWrapper CHANNEL;

    private NotificationNetwork() {
    }

    /**
     * Register the network channel and packet handler. Call once from common proxy preInit.
     * <p>注册网络通道和包处理器。在 common proxy preInit 中调用一次。</p>
     */
    public static void init() {
        CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(Tags.MODID + ":notif");
        // Discriminator id 0 — server→client notification packet
        CHANNEL.registerMessage(NotificationPacketHandler.class, NotificationPacket.class, 0, Side.CLIENT);
    }
}
