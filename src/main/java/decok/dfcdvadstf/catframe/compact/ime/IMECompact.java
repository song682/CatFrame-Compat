package decok.dfcdvadstf.catframe.compact.ime;

import decok.dfcdvadstf.catframe.compact.CompactBase;
import decok.dfcdvadstf.catframe.ui.components.AbstractEditBox;
import decok.dfcdvadstf.catframe.ui.components.AbstractTextAreaWidget;
import decok.dfcdvadstf.catframe.ui.components.MultilineEditBox;
import decok.dfcdvadstf.catframe.ui.components.events.ContainerEventHandler;
import decok.dfcdvadstf.catframe.ui.components.events.GuiEventListener;
import decok.dfcdvadstf.catframe.ui.overlay.Overlay;
import decok.dfcdvadstf.catframe.ui.overlay.OverlayManager;
import decok.dfcdvadstf.catframe.ui.overlay.ScreenAnchor;
import decok.dfcdvadstf.imeinput.api.IMECommitTarget;
import decok.dfcdvadstf.imeinput.api.IMEInputAPI;
import net.minecraft.client.Minecraft;

import javax.annotation.Nullable;

/**
 * <p>
 * IME Input Backport compat — hooks CatFrame's self-drawn text-area family
 * ({@link AbstractTextAreaWidget} and subclasses such as
 * {@link AbstractEditBox}
 * and {@link MultilineEditBox}) into the IMEInputBackport input pipeline: a
 * focused text area inside any visible Overlay activates the IME, committed
 * text is inserted at the caret as a whole (the same path as a manual paste —
 * filtering, length limits and the refresh callbacks all apply), and the
 * active text area anchors the candidate window.
 * </p>
 * <p>
 * Thread model: All methods are called by IMEInputBackport during the client's
 * tick (main thread), consistent with the registration convention of {@link IMEInputAPI};
 * this class is registered once in ClientProxy.preInit and does not need to be unregistered during the client lifecycle.
 * </p>
 */
public final class IMECompact implements IMECommitTarget {

    /** Singleton: the instance registered with {@link IMEInputAPI}. */
    private static final IMECompact INSTANCE = new IMECompact();

    private IMECompact() {
    }

    /**
     * Registers as an IME commit target when IMEInputBackport is loaded (call
     * once at client init).
     */
    public static void register() {
        if (CompactBase.isIMEBackportInstalled()) {
            IMEInputAPI.register(INSTANCE);
        }
    }

    @Override
    public boolean shouldReceiveInput() {
        // Any visible Overlay whose component tree holds a focused text area
        // claims IME input; both AbstractEditBox and MultilineEditBox fall
        // under AbstractTextAreaWidget.
        return findFocusedArea() != null;
    }

    @Override
    public void commitText(String text) {
        // Bulk-insert into the focused text area (same path as Ctrl+V paste),
        // refreshing exactly once. The focus is resolved anew instead of reusing
        // shouldReceiveInput's result, so a focus switch within the same tick
        // cannot route the text into a control that just lost focus.
        AbstractTextAreaWidget area = findFocusedArea();
        if (area != null) {
            area.insertText(text);
        }
    }

    @Override
    public int getCaretX() {
        AbstractTextAreaWidget area = findFocusedArea();
        if (area == null) {
            return -1;
        }
        int x = area.getX() + area.innerPadding();
        if (area instanceof AbstractEditBox) {
            // Single-line box: follow the caret (text-prefix width); multi-line
            // boxes fall back to the text start.
            AbstractEditBox box = (AbstractEditBox) area;
            String text = box.getText();
            int cursor = Math.min(box.getCursorPosition(), text.length());
            x += Minecraft.getMinecraft().fontRenderer.getStringWidth(text.substring(0, cursor));
        }
        return x;
    }

    @Override
    public int getCaretY() {
        // Bottom edge of the text area (below the caret), mirroring the
        // IMEHandler fallback for vanilla fields.
        AbstractTextAreaWidget area = findFocusedArea();
        return area != null ? area.getY() + area.getHeight() : -1;
    }

    /**
     * Finds the focused text area by walking the focus chain of every visible
     * Overlay registered in OverlayManager. Component coordinates are
     * layout-resolved screen absolutes (GUI units), usable as anchors directly.
     * Package-visible: {@link IgIMECompact} reuses the same lookup.
     *
     * @return the focused text area, or {@code null} if none
     */
    @Nullable
    static AbstractTextAreaWidget findFocusedArea() {
        OverlayManager manager = OverlayManager.INSTANCE;
        for (ScreenAnchor anchor : ScreenAnchor.values()) {
            for (Overlay overlay : manager.getOverlays(anchor)) {
                if (!overlay.isVisible()) {
                    continue;
                }
                AbstractTextAreaWidget area = findFocusedIn(overlay);
                if (area != null) {
                    return area;
                }
            }
        }
        return null;
    }

    /**
     * Recursively walks the component focus chain for a focused text area.
     *
     * @param node the component
     * @return the focused text area, or {@code null} if none
     */
    @Nullable
    private static AbstractTextAreaWidget findFocusedIn(GuiEventListener node) {
        if (node instanceof AbstractTextAreaWidget) {
            AbstractTextAreaWidget area = (AbstractTextAreaWidget) node;
            return area.isActive() && area.isVisible() && area.isFocused() ? area : null;
        }
        if (node instanceof ContainerEventHandler) {
            GuiEventListener focused = ((ContainerEventHandler) node).getFocused();
            if (focused != null) {
                return findFocusedIn(focused);
            }
        }
        return null;
    }
}
