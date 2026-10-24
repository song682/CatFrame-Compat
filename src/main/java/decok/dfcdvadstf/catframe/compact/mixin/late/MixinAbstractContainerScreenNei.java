package decok.dfcdvadstf.catframe.compact.mixin.late;

import decok.dfcdvadstf.catframe.compact.nei.NeiTooltipBridge;
import decok.dfcdvadstf.catframe.ui.GuiGraphicsExtractor;
import decok.dfcdvadstf.catframe.ui.screens.container.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * NEI tooltip hand-over - cancels CatFrame's deferred slot tooltip on the frames where
 * NEI already painted one.
 * <p>
 * NEI replaces the {@code renderToolTip} call site in {@code GuiContainer.drawScreen}
 * with {@code GuiContainerManager.renderToolTips}, which paints its own frame around
 * the same content; CatFrame's {@code extractTooltip} then runs at the end of the frame
 * and would paint a second one. The ownership marker is set by
 * {@link NeiTooltipBridge} only when NEI actually rendered a tooltip for a CatFrame
 * screen in this frame, so screens where NEI declines to draw (an item carried on the
 * cursor, for instance) keep CatFrame's tooltip untouched.
 * </p>
 * <p>
 * Cancelling at the head of {@code extractTooltip} rather than at the inner
 * {@code setTooltipForNextFrame} call keeps every guard of the original method intact -
 * no shadowed fields, no duplicated conditions. The target is a CatFrame class (not
 * vanilla), hence {@code remap = false}.
 * </p>
 */
@Mixin(value = AbstractContainerScreen.class, remap = false)
public abstract class MixinAbstractContainerScreenNei {

    @Inject(
            method = "extractTooltip(Ldecok/dfcdvadstf/catframe/ui/GuiGraphicsExtractor;II)V",
            at = @At("HEAD"),
            cancellable = true)
    private void catframecompact$neiSlotTooltip(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (NeiTooltipBridge.consumeOwnership()) {
            ci.cancel();
        }
    }
}
