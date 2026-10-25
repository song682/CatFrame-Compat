package decok.dfcdvadstf.catframe.compact.tooltips.nei;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.guihook.GuiContainerManager;
import codechicken.nei.guihook.IContainerTooltipHandler;
import decok.dfcdvadstf.catframe.CompatConfig;
import decok.dfcdvadstf.catframe.compact.CompactBase;
import decok.dfcdvadstf.catframe.compact.tooltips.ChromaticTooltips;
import decok.dfcdvadstf.catframe.ui.screens.container.AbstractContainerScreen;
import decok.dfcdvadstf.catframe.ui.screens.inventory.tooltip.ClientTooltipComponent;
import decok.dfcdvadstf.catframe.world.tooltip.ItemTooltipImages;
import decok.dfcdvadstf.catframe.world.tooltip.TooltipComponent;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;

import java.util.List;
import java.util.Optional;

import static decok.dfcdvadstf.catframe.CatFrameCompat.logger;

/**
 * NEI container-tooltip bridge - lets CatFrame's structured tooltip components
 * coexist with NEI's single tooltip frame.
 * <p>
 * NEI replaces the {@code renderToolTip} call site inside
 * {@code GuiContainer.drawScreen} (ASM {@code MethodReplacer}), so CatFrame's own
 * {@code AbstractContainerScreen.renderToolTip} override becomes dead code and the
 * two tooltip pipelines end up painting the same content twice: NEI draws its frame
 * mid-frame, CatFrame draws its deferred nine-slice frame at the end of the frame.
 * This bridge keeps exactly one frame by:
 * </p>
 * <ol>
 *   <li>giving NEI the structured components as CodeChickenLib line handlers
 *       ({@link NeiComponentLine}), which makes them survive in NEI's frame and, unlike
 *       CatFrame's own pipeline, also appear in non-CatFrame containers; and</li>
 *   <li>marking the frame as NEI-owned, so the late mixin cancels CatFrame's deferred
 *       tooltip for that frame.</li>
 * </ol>
 * <p>
 * Text lines need no bridging at all: NEI resolves them through
 * {@code ItemStack.getTooltip}, the same call CatFrame makes, so
 * {@code ItemTooltipEvent} contributions (including the tag tooltips of this mod) are
 * already shared.
 * </p>
 * <p>
 * Class-loading isolation: this class links NEI and CodeChickenLib types, so it is
 * only referenced from {@code ClientProxy} behind the NEI detection gate and from a
 * late mixin whose applyIf carries the same gate.
 * </p>
 */
public final class NeiTooltipBridge implements IContainerTooltipHandler {

    private static final NeiTooltipBridge INSTANCE = new NeiTooltipBridge();

    /**
     * Set when NEI rendered a tooltip for a CatFrame screen in this frame. Consumed by
     * the late mixin at the head of {@code extractTooltip}, which always runs once per
     * frame on CatFrame screens.
     */
    private static boolean neiOwnedTooltip;

    private NeiTooltipBridge() {}

    /**
     * Registers the bridge with NEI. Must run after NEI's own class initialization
     * (post-init), so NEI's default handlers stay ahead of this one.
     */
    public static void register() {
        if (!isEnabled()) {
            return;
        }
        if (ChromaticTooltips.isAvailable()) {
            // Another tooltip renderer owns the frame for CatFrame screens, and it
            // replaces the tooltip wholesale rather than extending it: keep NEI from
            // drawing a second frame instead of translating components into it.
            GuiContainerManager.addObjectHandler(NeiTooltipSuppressor.INSTANCE);
            logger.info("NEI tooltip coexistence: Chromatic Tooltips owns CatFrame tooltips, NEI container tooltips suppressed there.");
            return;
        }
        GuiContainerManager.addTooltipHandler(INSTANCE);
        logger.info("NEI tooltip bridge enabled (NotEnoughItems detected): CatFrame structured components are rendered inside NEI's tooltip frame.");
    }

    /**
     * Whether the bridge should take part in the current setup.
     */
    public static boolean isEnabled() {
        return CompatConfig.neiTooltipsCompat && CompactBase.isNEIInstalled();
    }

    /**
     * Reads and clears the frame ownership marker.
     *
     * @return true when NEI painted a tooltip for a CatFrame screen this frame
     */
    public static boolean consumeOwnership() {
        final boolean owned = neiOwnedTooltip;
        neiOwnedTooltip = false;
        return owned;
    }

    @Override
    public List<String> handleTooltip(final GuiContainer gui, final int mousex, final int mousey,
                                      final List<String> currenttip) {
        // A non-empty container-level tooltip makes NEI skip the item tooltip branch
        // entirely, so CatFrame has to yield here as well - otherwise the slot tooltip
        // would be the only thing painted and NEI's frame would show something else.
        if (!currenttip.isEmpty()) {
            markOwnership(gui);
        }
        return currenttip;
    }

    @Override
    public List<String> handleItemDisplayName(final GuiContainer gui, final ItemStack itemstack,
                                              final List<String> currenttip) {
        // Marker rows must not be added here: NEI wraps every line after the first in
        // GRAY ... RESET, which would destroy the "§x" prefix the protocol relies on.
        return currenttip;
    }

    @Override
    public List<String> handleItemTooltip(final GuiContainer gui, final ItemStack itemstack, final int mousex,
                                          final int mousey, final List<String> currenttip) {
        if (!isEnabled() || itemstack == null || itemstack.getItem() == null) {
            return currenttip;
        }
        final Optional<TooltipComponent> image = ItemTooltipImages.get(itemstack);
        if (!image.isPresent()) {
            return currenttip;
        }
        final FontRenderer font = GuiContainerManager.getFontRenderer(itemstack);
        final ClientTooltipComponent component;
        try {
            component = ClientTooltipComponent.create(image.get());
        } catch (final IllegalArgumentException unregistered) {
            // The item declared a component type whose client renderer was never
            // registered (or is not loaded); fall back to the text-only tooltip.
            return currenttip;
        }
        if (component.getWidth(font) <= 0 || component.getHeight(font) <= 0) {
            return currenttip;
        }
        // Right below the item name, matching CatFrame's own component slot (index 1).
        final String marker = GuiDraw.TOOLTIP_HANDLER + GuiDraw.getTipLineId(new NeiComponentLine(component, font));
        currenttip.add(currenttip.isEmpty() ? 0 : 1, marker);
        markOwnership(gui);
        return currenttip;
    }

    /**
     * Flags the frame as NEI-owned, but only for CatFrame screens - those are the only
     * ones that would paint a second frame of their own.
     */
    private static void markOwnership(final GuiContainer gui) {
        if (gui instanceof AbstractContainerScreen) {
            neiOwnedTooltip = true;
        }
    }
}
