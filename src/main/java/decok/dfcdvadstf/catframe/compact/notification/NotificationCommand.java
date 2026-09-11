package decok.dfcdvadstf.catframe.compact.notification;

import decok.dfcdvadstf.catframe.compact.notification.network.NotificationAPI;
import decok.dfcdvadstf.catframe.ui.extended.components.notifications.NotificationBase;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;

import java.util.List;

/**
 * <p>
 * Server-side {@code /notification} command — manages and dispatches notification definitions
 * loaded from {@code catframenotifications.json}.
 * </p>
 * <p>
 * 服务端 {@code /notification} 命令 —— 管理并分发从 {@code catframenotifications.json}
 * 加载的通知定义。
 * </p>
 *
 * <h3>Syntax</h3>
 * <pre>
 *   /notification send &lt;id&gt;          — broadcast a predefined notification to all players
 *   /notification send &lt;id&gt; &lt;player&gt; — send a predefined notification to a specific player
 *   /notification reload              — reload definitions from disk
 * </pre>
 */
public class NotificationCommand extends CommandBase {

    private static final String[] SUB_COMMANDS = {"send", "reload"};

    @Override
    public String getCommandName() {
        return "notification";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/notification <send|reload>";
    }

    /**
     * Requires permission level 2 (standard for game-master commands).
     * <p>需要权限等级 2（游戏管理员命令的标准等级）。</p>
     */
    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return sender.canCommandSenderUseCommand(2, getCommandName());
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length < 1) {
            throw new WrongUsageException(getCommandUsage(sender));
        }

        String sub = args[0].toLowerCase();

        if ("reload".equals(sub)) {
            MinecraftServer server = MinecraftServer.getServer();
            NotificationDefinitions.reload(server);
            func_152373_a(sender, this,
                    "[Notification] catframenotifications.json reloaded (%d definitions).",
                    NotificationDefinitions.getIds().size());
        } else if ("send".equals(sub)) {
            if (args.length < 2) {
                throw new WrongUsageException("/notification send <id> [player]");
            }
            String id = args[1];
            NotificationBase notification = NotificationDefinitions.get(id);
            if (notification == null) {
                throw new CommandException(
                        "[Notification] ID '" + id + "' not found. Check catframenotifications.json or run /notification reload.");
            }

            MinecraftServer server = MinecraftServer.getServer();

            if (args.length >= 3) {
                // Send to a specific player
                String targetName = args[2];
                NotificationAPI.sendTo(server, targetName, notification);
                func_152373_a(sender, this,
                        "[Notification] '%s' sent to player '%s'.", id, targetName);
            } else {
                // Broadcast to all players
                NotificationAPI.broadcast(server, notification);
                func_152373_a(sender, this,
                        "[Notification] '%s' broadcast to all online players.", id);
            }
        } else {
            throw new WrongUsageException(getCommandUsage(sender));
        }
    }

    // ──── Tab completion ────

    @Override
    @SuppressWarnings("rawtypes")
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, SUB_COMMANDS);
        }
        if (args.length == 2 && "send".equals(args[0])) {
            return getListOfStringsMatchingLastWord(args,
                    NotificationDefinitions.getIds().toArray(new String[0]));
        }
        if (args.length == 3 && "send".equals(args[0])) {
            return getListOfStringsMatchingLastWord(args,
                    MinecraftServer.getServer().getAllUsernames());
        }
        return null;
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return index == 2 && args.length > 0 && "send".equals(args[0]);
    }
}
