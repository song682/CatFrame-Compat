package decok.dfcdvadstf.catframe.compact.mixin.late;

import decok.dfcdvadstf.catframe.compact.tooltips.ChromaticTooltips;
import decok.dfcdvadstf.catframe.core.tooltip.TooltipComponent;
import decok.dfcdvadstf.catframe.ui.GuiGraphicsExtractor;
import decok.dfcdvadstf.catframe.ui.screens.container.AbstractContainerScreen;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/**
 * 物品槽位 tooltip 的 Chromatic Tooltips 重定向——为悬停槽位的物品
 * 保留 {@link ItemStack} 目标，使 Chromatic Tooltips 的物品系 enricher
 * （标题/属性图鉴/堆叠数/快捷键分区）对 CatFrame 容器界面完整生效。
 *
 * <p><b>为何用 {@code @Redirect} 而非 HEAD 取消</b>：CatFrame 的
 * {@code extractTooltip} 自带守卫（悬停槽非空、有物品、手上未持物
 * 或物品图像声明持物时仍显示）。重定向调用点位于守卫之后，全部守卫
 * 原样保留——重定向仅在原本真的要设置 tooltip 时触发，无需在 mixin
 * 里复制任何守卫逻辑（也就不必 shadow 泛型 final 字段与私有方法）。
 * 被重定向的行文字正是原实现计算好的 {@code getTooltipFromContainerItem}
 * 结果（子类可覆盖该方法追加行），保真度最高。</p>
 *
 * <p>CT 不可用时原调用照常执行（CatFrame 自有 tooltip 渲染不受影响）；
 * 该路径同时会被 {@code MixinGuiGraphicsExtractor} 的组件重载兜底接管
 * （不带物品目标，仅主题渲染生效）。</p>
 *
 * <p>目标与调用均为 CatFrame 类（非 vanilla），故 {@code remap = false}。</p>
 */
@Mixin(value = AbstractContainerScreen.class, remap = false)
public abstract class MixinAbstractContainerScreen {

    /** 悬停槽——守卫通过后非空，从中取回与本地变量 {@code item} 同一物品。 */
    @Shadow
    @Nullable
    protected Slot hoveredSlot;

    /**
     * 重定向 {@code extractTooltip} 内的延迟 tooltip 设置调用。
     * <p>取回悬停物品后连同行文字一并转交 Chromatic Tooltips；
     * 物品不可用时回退为不带目标的纯文本转交（由
     * {@code MixinGuiGraphicsExtractor} 的兜底注入处理一致性）。</p>
     */
    @Redirect(
            method = "extractTooltip(Ldecok/dfcdvadstf/catframe/ui/GuiGraphicsExtractor;II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Ldecok/dfcdvadstf/catframe/ui/GuiGraphicsExtractor;setTooltipForNextFrame(Lnet/minecraft/client/gui/FontRenderer;Ljava/util/List;Ljava/util/Optional;IILnet/minecraft/util/ResourceLocation;)V",
                    remap = false),
            remap = false)
    private void catframecompact$chromaticSlotTooltip(
            GuiGraphicsExtractor graphics, FontRenderer font, List<String> lines,
            Optional<TooltipComponent> component, int xo, int yo, @Nullable ResourceLocation style) {
        if (!ChromaticTooltips.isAvailable()) {
            graphics.setTooltipForNextFrame(font, lines, component, xo, yo, style);
            return;
        }

        ItemStack stack = this.hoveredSlot != null ? this.hoveredSlot.getStack() : null;
        if (stack != null && stack.getItem() != null) {
            ChromaticTooltips.drawTooltip(stack, lines);
        } else {
            ChromaticTooltips.drawTooltip(lines);
        }
    }
}
