package decok.dfcdvadstf.catframe.ui.extended.theme;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;

import java.util.Collections;
import java.util.List;

/**
 * <p>
 * 客户端 {@code /theme} 命令 —— 打开主题选择界面。
 * 命令本身不做任何界面操作，仅向 {@link ThemeKeyHandler} 排队一个打开请求，
 * 由客户端 tick 的 END 阶段统一安全地切换界面。
 * </p>
 * <p>
 * Client-side {@code /theme} command — opens the theme selection screen.
 * The command performs no screen operation itself; it only queues an open request
 * with {@link ThemeKeyHandler}, and the screen switch happens safely on the END
 * phase of the client tick.
 * </p>
 */
@SideOnly(Side.CLIENT)
public class ThemeCommand implements ICommand {

    @Override
    public String getCommandName() {
        return "theme";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/theme";
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List getCommandAliases() {
        return Collections.emptyList();
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        ThemeKeyHandler.requestOpen();
    }

    /** Client-side command: always available to the local player. */
    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        return null;
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return false;
    }

    @Override
    public int compareTo(Object other) {
        return other instanceof ICommand
                ? getCommandName().compareTo(((ICommand) other).getCommandName())
                : 0;
    }
}
