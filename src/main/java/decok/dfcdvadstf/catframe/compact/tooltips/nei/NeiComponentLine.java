package decok.dfcdvadstf.catframe.compact.tooltips.nei;

import codechicken.lib.gui.GuiDraw;
import decok.dfcdvadstf.catframe.ui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.FontRenderer;

import java.awt.Dimension;

/**
 * Adapts a CatFrame {@link ClientTooltipComponent} to CodeChickenLib's line-handler
 * protocol, so NEI's {@code GuiDraw.drawMultilineTip} can reserve a row for it.
 * <p>
 * Protocol notes (both from {@code GuiDraw}):
 * <ul>
 *   <li>A row is measured through {@link #getSize()} and painted through
 *       {@link #draw(int, int)}; the row itself is a marker string
 *       {@code GuiDraw.TOOLTIP_HANDLER + getTipLineId(handler)} that renders no text.</li>
 *   <li>The handler table is a static list cleared at the end of every
 *       {@code drawMultilineTip} call, so a fresh instance must be registered during
 *       the content phase of each frame - never cached across frames.</li>
 * </ul>
 * <p>
 * The component's own geometry is authoritative: CatFrame passes the whole-tooltip
 * width/height into {@code renderImage}, while image components (the bundle grid, for
 * instance) draw at their intrinsic size.
 * </p>
 */
final class NeiComponentLine implements GuiDraw.ITooltipLineHandler {

    private final ClientTooltipComponent component;
    private final FontRenderer font;

    /** Lazily measured row size; the same instance is queried twice per tooltip. */
    private Dimension size;

    NeiComponentLine(final ClientTooltipComponent component, final FontRenderer font) {
        this.component = component;
        this.font = font;
    }

    @Override
    public Dimension getSize() {
        if (this.size == null) {
            this.size = new Dimension(
                    Math.max(1, this.component.getWidth(this.font)),
                    Math.max(1, this.component.getHeight(this.font)));
        }
        return this.size;
    }

    @Override
    public void draw(final int x, final int y) {
        final Dimension size = getSize();
        this.component.renderImage(this.font, x, y, size.width, size.height);
    }
}
