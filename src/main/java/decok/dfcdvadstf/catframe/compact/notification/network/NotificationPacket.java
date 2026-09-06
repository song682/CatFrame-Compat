package decok.dfcdvadstf.catframe.compact.notification.network;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;

/**
 * <p>
 * Network packet that carries notification data from server to client.<br>
 * Uses NBT serialisation via {@link ByteBufUtils} for flexible field transfer.
 * </p>
 * <p>
 * 从服务端向客户端传输通知数据的网络包。<br>
 * 使用 {@link ByteBufUtils} 的 NBT 序列化以实现灵活的字段传输。
 * </p>
 */
public class NotificationPacket implements IMessage {

    private String title;
    private String message;
    private int duration;
    private int titleColor;
    private int messageColor;
    private int backgroundColor;
    private int borderColor;

    /** No-arg constructor required by Forge. / Forge 要求的无参构造。 */
    public NotificationPacket() {
    }

    /**
     * Build a packet from notification data.
     * <p>从通知数据构建包。</p>
     */
    public NotificationPacket(String title, String message, int duration,
                              int titleColor, int messageColor,
                              int backgroundColor, int borderColor) {
        this.title = title;
        this.message = message;
        this.duration = duration;
        this.titleColor = titleColor;
        this.messageColor = messageColor;
        this.backgroundColor = backgroundColor;
        this.borderColor = borderColor;
    }

    // ──── IMessage ────

    @Override
    public void fromBytes(ByteBuf buf) {
        NBTTagCompound tag = ByteBufUtils.readTag(buf);
        if (tag == null) {
            title = "";
            message = "";
            return;
        }
        title = tag.getString("title");
        message = tag.getString("message");
        duration = tag.getInteger("duration");
        titleColor = tag.getInteger("titleColor");
        messageColor = tag.getInteger("messageColor");
        backgroundColor = tag.getInteger("backgroundColor");
        borderColor = tag.getInteger("borderColor");
    }

    @Override
    public void toBytes(ByteBuf buf) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("title", title != null ? title : "");
        tag.setString("message", message != null ? message : "");
        tag.setInteger("duration", duration);
        tag.setInteger("titleColor", titleColor);
        tag.setInteger("messageColor", messageColor);
        tag.setInteger("backgroundColor", backgroundColor);
        tag.setInteger("borderColor", borderColor);
        ByteBufUtils.writeTag(buf, tag);
    }

    // ──── Accessors ────

    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public int getDuration() { return duration; }
    public int getTitleColor() { return titleColor; }
    public int getMessageColor() { return messageColor; }
    public int getBackgroundColor() { return backgroundColor; }
    public int getBorderColor() { return borderColor; }
}
