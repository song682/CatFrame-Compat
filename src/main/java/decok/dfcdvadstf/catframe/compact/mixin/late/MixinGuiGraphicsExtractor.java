package decok.dfcdvadstf.catframe.compact.mixin.late;

import decok.dfcdvadstf.catframe.compact.tooltips.ChromaticTooltips;
import decok.dfcdvadstf.catframe.ui.GuiGraphicsExtractor;
import decok.dfcdvadstf.catframe.ui.screens.Screen;
import decok.dfcdvadstf.catframe.ui.tooltip.ClientTooltipPositioner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/**
 * Chromatic Tooltips 渲染接管——把 CatFrame 延迟 tooltip 管线的三个
 * {@code List<String>}/{@code ItemStack} 级公开入口重定向到 Chromatic Tooltips。
 *
 * <p><b>入口覆盖</b>（CatFrame 0.10.2 的调用拓扑）：</p>
 * <ul>
 *   <li>全参重载 {@code (FontRenderer, List, Optional, ClientTooltipPositioner, int, int, boolean, ResourceLocation)}
 *       —— widget tooltip 的唯一漏斗（{@code WidgetTooltipHolder.refreshTooltipForNextRenderPass}），
 *       其余简便重载均委托到它；</li>
 *   <li>组件重载 {@code (FontRenderer, List, Optional, int, int, ResourceLocation)}
 *       —— 不经全参重载、自行组装组件的旁路（物品槽 tooltip 若未被
 *       {@code MixinAbstractContainerScreen} 截获也会落到这里）；</li>
 *   <li>物品重载 {@code (FontRenderer, ItemStack, int, int)}
 *       —— 物品 tooltip 公开 API，行文字由 {@code Screen.getTooltipFromItem} 取得。</li>
 * </ul>
 *
 * <p><b>重定向语义</b>：取消 CatFrame 自己的延迟渲染（背景九宫格 + 文字行），
 * 转交 {@code TooltipHandler.drawHoveringText} 排队；定位/延迟显示/分页/绘制
 * 全部由 Chromatic Tooltips 在 {@code DrawScreenEvent.Post} 完成。由于取消后
 * {@code deferredTooltip} 保持 null，本帧末尾的 {@code extractDeferredElements}
 * 不会再绘制 CatFrame tooltip，不存在双重渲染。CatFrame 的 positioner、
 * 结构化图像组件与自定义样式 ID 在 Chromatic Tooltips 主题下无对应概念，一并舍弃。</p>
 *
 * <p><b>类加载隔离</b>：本 mixin 仅在 Chromatic Tooltips 安装且配置开启时应用
 * （见 {@code Mixins} 枚举的 applyIf），处理器引用的桥接类因此可安全链接 CT 类。
 * 目标是 CatFrame 类（非 vanilla），故 {@code remap = false}。</p>
 */
@Mixin(value = GuiGraphicsExtractor.class, remap = false)
public abstract class MixinGuiGraphicsExtractor {

    /**
     * 全参重载——widget tooltip 漏斗。CatFrame 的 positioner 语义（跟随菜单/固定在
     * widget 上下方）由 Chromatic Tooltips 的鼠标定位算法取代（官方 Compat 同此处理）。
     */
    @Inject(
            method = "setTooltipForNextFrame(Lnet/minecraft/client/gui/FontRenderer;Ljava/util/List;Ljava/util/Optional;Ldecok/dfcdvadstf/catframe/ui/tooltip/ClientTooltipPositioner;IIZLnet/minecraft/util/ResourceLocation;)V",
            at = @At("HEAD"), cancellable = true)
    private void catframecompact$chromaticWidgetTooltip(
            FontRenderer font, List<String> lines, Optional<?> component,
            ClientTooltipPositioner positioner, int xo, int yo, boolean replaceExisting,
            @Nullable ResourceLocation style, CallbackInfo ci) {
        if (!ChromaticTooltips.isAvailable()) return;
        ChromaticTooltips.drawTooltip(lines);
        ci.cancel();
    }

    /**
     * 组件重载——绕过全参漏斗的旁路（携带结构化图像组件与样式）。
     * 图像组件无 Chromatic Tooltips 对应物，文字行照常转交。
     */
    @Inject(
            method = "setTooltipForNextFrame(Lnet/minecraft/client/gui/FontRenderer;Ljava/util/List;Ljava/util/Optional;IILnet/minecraft/util/ResourceLocation;)V",
            at = @At("HEAD"), cancellable = true)
    private void catframecompact$chromaticComponentTooltip(
            FontRenderer font, List<String> lines, Optional<?> component,
            int xo, int yo, @Nullable ResourceLocation style, CallbackInfo ci) {
        if (!ChromaticTooltips.isAvailable()) return;
        ChromaticTooltips.drawTooltip(lines);
        ci.cancel();
    }

    /**
     * 物品重载——携带 {@link ItemStack}，转交后 Chromatic Tooltips 的物品系
     * enricher 完整生效；文字行与原实现同源（{@code Screen.getTooltipFromItem}）。
     */
    @Inject(
            method = "setTooltipForNextFrame(Lnet/minecraft/client/gui/FontRenderer;Lnet/minecraft/item/ItemStack;II)V",
            at = @At("HEAD"), cancellable = true)
    private void catframecompact$chromaticItemTooltip(
            FontRenderer font, ItemStack stack, int xo, int yo, CallbackInfo ci) {
        if (!ChromaticTooltips.isAvailable()) return;
        if (stack != null && stack.getItem() != null) {
            ChromaticTooltips.drawTooltip(stack, Screen.getTooltipFromItem(Minecraft.getMinecraft(), stack));
        }
        ci.cancel();
    }
}
