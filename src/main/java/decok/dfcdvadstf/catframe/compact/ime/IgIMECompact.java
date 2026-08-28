package decok.dfcdvadstf.catframe.compact.ime;

import city.windmill.ingameime.ClientProxy;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import decok.dfcdvadstf.catframe.compact.CompactBase;
import decok.dfcdvadstf.catframe.ui.components.AbstractEditBox;
import decok.dfcdvadstf.catframe.ui.components.AbstractTextAreaWidget;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nullable;

/**
 * IngameIME compat — bridges CatFrame's self-drawn text-area family
 * ({@link AbstractTextAreaWidget} and subclasses such as
 * {@link AbstractEditBox}
 * and {@code MultilineEditBox}) into the IngameIME input pipeline: a focused
 * text
 * area inside any visible Overlay drives IngameIME's {@code onControlFocus}
 * into
 * its OpenedAuto state machine (focus activates the IME, blur deactivates it,
 * the
 * manual Home toggle is left untouched), and the caret is synced every tick so
 * the
 * pre-edit text / input-mode indicator follows the CatFrame text box.
 */
@SideOnly(Side.CLIENT)
public final class IgIMECompact {

    /** Singleton: An instance registered to the Forge event bus */
    private static final IgIMECompact INSTANCE = new IgIMECompact();

    /** The focused text area from the previous tick; used to detect focus changes */
    @Nullable
    private AbstractTextAreaWidget lastFocused = null;

    private IgIMECompact() {
    }

    /**
     * Registers the bridge when IngameIME is loaded and IMEInputBackport is not
     * (call once at client init).
     */
    public static void register() {
        if (CompactBase.isIGIMEInstalled() && !CompactBase.isIMEBackportInstalled()) {
            MinecraftForge.EVENT_BUS.register(INSTANCE);
        }
    }

    /**
     * Drives the focus bridge and caret sync once per client tick.
     *
     * @param event tick event
     */
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            return;
        }
        // IngameIME already disabled itself when the screen closed (its
        // MixinMinecraft.displayGuiScreen hook); only drop the reference here —
        // calling onControlFocus would re-activate it.
        if (mc.currentScreen == null) {
            lastFocused = null;
            return;
        }

        AbstractTextAreaWidget area = IMECompact.findFocusedArea();
        if (area != lastFocused) {
            ClientProxy proxy = ClientProxy.INSTANCE;
            if (proxy != null) {
                // Notify the old control's blur first, then the new one's focus,
                // so the OpenedAuto state machine matches on ActiveControl.
                if (lastFocused != null) {
                    proxy.onControlFocus(lastFocused, false);
                }
                if (area != null) {
                    proxy.onControlFocus(area, true);
                }
            }
            lastFocused = area;
        }

        if (area != null) {
            ClientProxy.Screen.setCaretPos(caretX(area), caretY(area));
        }
    }

    /**
     * Caret X (GUI units): component left edge + inner padding, plus the
     * caret-prefix text width for single-line boxes.
     *
     * @param area focused text area
     * @return caret X
     */
    private static int caretX(AbstractTextAreaWidget area) {
        int x = area.getX() + area.innerPadding();
        if (area instanceof AbstractEditBox) {
            AbstractEditBox box = (AbstractEditBox) area;
            String text = box.getText();
            int cursor = Math.min(box.getCursorPosition(), text.length());
            x += Minecraft.getMinecraft().fontRenderer.getStringWidth(text.substring(0, cursor));
        }
        return x;
    }

    /**
     * Caret Y (GUI units): bottom edge of the text area, mirroring the IngameIME
     * fallback for vanilla fields.
     *
     * @param area focused text area
     * @return caret Y
     */
    private static int caretY(AbstractTextAreaWidget area) {
        return area.getY() + area.getHeight();
    }
}
