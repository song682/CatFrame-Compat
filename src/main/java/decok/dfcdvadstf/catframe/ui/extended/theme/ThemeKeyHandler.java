package decok.dfcdvadstf.catframe.ui.extended.theme;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

/**
 * <p>
 * 打开主题选择界面的输入入口 —— 提供一个可自定义的按键绑定（默认不绑定按键），
 * 并在客户端 tick 的 END 阶段统一延迟打开界面：
 * 无论请求来自按键还是 {@code /theme} 客户端命令，都等当前输入处理结束
 * （聊天框关闭、按键事件派发完毕）后再切换界面，避免与输入管线冲突。
 * </p>
 * <p>
 * Input entry point for the theme selection screen — provides a rebindable key
 * binding (unbound by default) and opens the screen deferred to the END phase of
 * the client tick: whether the request comes from the hotkey or the {@code /theme}
 * client command, the screen is switched only after the current input processing
 * finishes (chat closed, key event fully dispatched), avoiding conflicts with the
 * input pipeline.
 * </p>
 */
@SideOnly(Side.CLIENT)
public final class ThemeKeyHandler {

    /** Key binding that opens the theme selection screen (unbound by default). */
    public static final KeyBinding OPEN_THEME_SCREEN = new KeyBinding(
            "key.catframe_compact.open_theme_screen", Keyboard.KEY_NONE, "CatFrame Compact");

    private static final ThemeKeyHandler INSTANCE = new ThemeKeyHandler();

    /** True when an open request is waiting for the next tick. / 有打开请求等待下一 tick 处理。 */
    private static boolean pendingOpen;

    private ThemeKeyHandler() {
    }

    /**
     * Register the key binding and the FML event listeners.
     * <p>注册按键绑定与 FML 事件监听器。</p>
     */
    public static void register() {
        ClientRegistry.registerKeyBinding(OPEN_THEME_SCREEN);
        FMLCommonHandler.instance().bus().register(INSTANCE);
    }

    /**
     * Queue a request to open the theme selection screen on the next client tick.
     * <p>排队一个请求，在下一客户端 tick 打开主题选择界面。</p>
     */
    public static void requestOpen() {
        pendingOpen = true;
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (OPEN_THEME_SCREEN.isPressed() && Minecraft.getMinecraft().currentScreen == null) {
            requestOpen();
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !pendingOpen) {
            return;
        }
        pendingOpen = false;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen == null) {
            mc.displayGuiScreen(new ThemeSelectScreen());
        }
    }
}
