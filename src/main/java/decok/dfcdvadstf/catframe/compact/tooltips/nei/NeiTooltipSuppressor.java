package decok.dfcdvadstf.catframe.compact.tooltips.nei;

import codechicken.nei.guihook.IContainerObjectHandler;
import decok.dfcdvadstf.catframe.ui.screens.container.AbstractContainerScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;

/**
 * Suppresses NEI's container tooltip on CatFrame screens while another tooltip
 * renderer owns them.
 * <p>
 * Used only in the configuration where Chromatic Tooltips replaces CatFrame's tooltip
 * rendering: without this, NEI would still paint its own frame inside
 * {@code GuiContainer.drawScreen} and the player would see two tooltips again. The
 * predicate is deliberately screen-scoped - NEI's own GUIs and every other container
 * keep their tooltips.
 * </p>
 */
final class NeiTooltipSuppressor implements IContainerObjectHandler {

    static final NeiTooltipSuppressor INSTANCE = new NeiTooltipSuppressor();

    private NeiTooltipSuppressor() {}

    @Override
    public void guiTick(final GuiContainer gui) {
    }

    @Override
    public void refresh(final GuiContainer gui) {
    }

    @Override
    public void load(final GuiContainer gui) {
    }

    @Override
    public ItemStack getStackUnderMouse(final GuiContainer gui, final int mousex, final int mousey) {
        return null;
    }

    @Override
    public boolean objectUnderMouse(final GuiContainer gui, final int mousex, final int mousey) {
        return false;
    }

    @Override
    public boolean shouldShowTooltip(final GuiContainer gui) {
        // false tells NEI to skip its tooltip; other screens are unaffected.
        return !(gui instanceof AbstractContainerScreen);
    }
}
