package decok.dfcdvadstf.catframe.compact.mixin.early;

import decok.dfcdvadstf.catframe.compact.tooltips.CatFrameTooltips;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * CatFrame tooltip rendering on vanilla-style screens.
 * <p>
 * Replaces the vanilla tooltip of every screen that keeps the vanilla path -
 * other mods' container GUIs, the survival/creative inventory, chat hover text,
 * creative tab names - with CatFrame's themed tooltip, i.e. the same deferred
 * pipeline CatFrame's own screens use. The switch and the precedence rules live in
 * {@link CatFrameTooltips#isAvailable()}: off by default, and inert whenever
 * NotEnoughItems or Chromatic Tooltips is installed (both take the vanilla path
 * over wholesale).
 * </p>
 *
 * <p><b>Why {@code @Inject} at {@code HEAD} and not a redirect.</b> Both methods are
 * the tooltip funnel itself, so cancelling at the head leaves every upstream guard
 * intact: {@code GuiContainer.drawScreen} already decided that a slot is hovered,
 * that it holds a stack and that nothing is carried on the cursor before it calls
 * {@code renderToolTip}, and {@code drawHoveringText} already received the finished
 * text lines. Nothing of the vanilla guards has to be duplicated here.</p>
 *
 * <p><b>Why {@code renderToolTip} is hooked as well.</b> {@code drawHoveringText}
 * alone would lose the {@link ItemStack}: structured tooltip components
 * ({@code ItemTooltipImages}, e.g. the bundle grid) and the item's
 * {@code TOOLTIP_STYLE} can only be resolved from the stack. The item path queues
 * from the stack and cancels, so the {@code drawHoveringText} hook below only serves
 * the text-only callers - and screens that override {@code renderToolTip} still fall
 * through to it.</p>
 *
 * <p><b>Timing.</b> {@code GuiScreen} is a vanilla class loaded during client
 * startup, hence this mixin is registered through the early channel
 * ({@code IEarlyMixinLoader}) rather than the late one - a late mixin would arrive
 * after the class has been loaded.</p>
 *
 * <p><b>Remapping.</b> {@code renderToolTip} is a vanilla method and keeps the default
 * remapping, so it resolves through the refmap. {@code drawHoveringText} must not be
 * remapped: it is a method <i>added</i> by Forge's binary patch and therefore absent
 * from the obfuscation mappings ({@code mcp-srg.srg} has no entry for it - the
 * unpatched {@code GuiScreen} only carries the three-argument
 * {@code func_146283_a}). Its literal name survives into production, which is why the
 * injector below sets {@code remap = false}.</p>
 */
@Mixin(GuiScreen.class)
public abstract class MixinGuiScreenTooltip {

    /**
     * 物品 tooltip——带物品目标排队，结构化组件与 TOOLTIP_STYLE 一并生效。
     *
     * @param stack  hovered stack (vanilla guarantees non-null with an item)
     * @param mouseX mouse X
     * @param mouseY mouse Y
     * @param ci     callback
     */
    @Inject(method = "renderToolTip", at = @At("HEAD"), cancellable = true)
    private void catframecompact$catFrameItemTooltip(final ItemStack stack, final int mouseX, final int mouseY,
                                                     final CallbackInfo ci) {
        if (!CatFrameTooltips.isAvailable()) return;
        if (stack == null || stack.getItem() == null) return;

        // Same font resolution as the vanilla method this replaces.
        final FontRenderer itemFont = stack.getItem().getFontRenderer(stack);
        CatFrameTooltips.drawItemTooltip(stack,
                itemFont != null ? itemFont : Minecraft.getMinecraft().fontRenderer,
                mouseX, mouseY);
        ci.cancel();
    }

    /**
     * 纯文本 tooltip——聊天栏悬停文字、创造模式标签名等无物品目标的调用。
     *
     * @param lines  finished text lines (already colour-coded by the caller)
     * @param mouseX mouse X
     * @param mouseY mouse Y
     * @param font   font requested by the caller, may be {@code null}
     * @param ci     callback
     */
    // remap = false: Forge-added method, see the class javadoc.
    @Inject(method = "drawHoveringText", at = @At("HEAD"), cancellable = true, remap = false)
    private void catframecompact$catFrameHoveringText(final List<String> lines, final int mouseX, final int mouseY,
                                                      final FontRenderer font, final CallbackInfo ci) {
        if (!CatFrameTooltips.isAvailable()) return;
        if (lines == null || lines.isEmpty()) return;

        // The vanilla list is consumed verbatim, so callers that pre-format their
        // lines (creative tab names, chat components) keep their colours.
        CatFrameTooltips.drawTooltip(font != null ? font : Minecraft.getMinecraft().fontRenderer,
                lines, mouseX, mouseY);
        ci.cancel();
    }
}
