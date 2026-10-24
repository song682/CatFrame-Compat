package decok.dfcdvadstf.catframe.ui.extended.components;

import net.minecraft.util.ResourceLocation;

public class WidgetSpriteStateResident {

    private final ResourceLocation enabled;
    private final ResourceLocation selected;
    private final ResourceLocation highlighted;
    private final ResourceLocation selectedHighlighted;
    private final int type = 0;

    public WidgetSpriteStateResident(ResourceLocation sprite) {
        this(sprite, sprite, sprite, sprite);
    }

    public WidgetSpriteStateResident(ResourceLocation enabled, ResourceLocation selected, ResourceLocation highlighted) {
        this(enabled, selected, highlighted, highlighted);
    }

    public WidgetSpriteStateResident(ResourceLocation enabled, ResourceLocation selected,
                                     ResourceLocation highlighted, ResourceLocation selectedHighlighted) {
        this.enabled = enabled;
        this.selected = selected;
        this.highlighted = highlighted;
        this.selectedHighlighted = selectedHighlighted;
    }

    public int getType() {
        return this.type;
    }

    public ResourceLocation get(boolean isSelected, boolean hoverOrFocused) {
        if (isSelected) {
            return hoverOrFocused ? this.selected : this.selectedHighlighted;
        } else {
            return hoverOrFocused ? this.enabled : this.highlighted;
        }
    }
}
