package decok.dfcdvadstf.catframe.compact.tooltips;

import decok.dfcdvadstf.catframe.CompatConfig;
import decok.dfcdvadstf.catframe.compact.CompactBase;
import net.minecraft.item.ItemStack;

import java.util.List;

/**
 * Chromatic Tooltips 渲染桥——把 CatFrame UI 的 tooltip 内容转交给 Chromatic Tooltips。
 * <p>
 * 对应官方 ChromaticTooltipsCompat 对其他 UI mod（ModularUI、JEC 等）的接法：
 * 只需把文字行交给 {@code TooltipHandler.drawHoveringText(...)}，排队、延迟显示、
 * FPS 节流、分页与最终绘制均由 Chromatic Tooltips 在 {@code DrawScreenEvent.Post}
 * 统一完成，无需本方再绘制背景或文字。
 * <ul>
 *   <li>带 {@link ItemStack}：走 {@code drawHoveringText(stack, lines)}，
 *       Chromatic Tooltips 的 enricher（标题/物品属性/堆叠数/快捷键等分区）完整生效；</li>
 *   <li>纯文本（widget tooltip）：走 {@code drawHoveringText(lines)}，
 *       首行被其 TitleEnricher 提升为标题、其余行进 body 分区（官方语义）。</li>
 * </ul>
 * <p>
 * <b>类加载隔离</b>：本类直接引用 Chromatic Tooltips 的类，仅可被 mixin 处理器
 * （仅在检测到该 mod 安装且配置开启时才应用）加载，禁止在其他路径触碰本类。
 */
public final class ChromaticTooltips {

    private ChromaticTooltips() {}

    /**
     * 兼容层是否生效——mod 已安装且配置开启。
     * <p>mod 检测在 {@link CompactBase}，配置门控在 {@link CompatConfig}，均不触碰 CT 类。</p>
     */
    public static boolean isAvailable() {
        return CompatConfig.chromaticTooltipsCompat && CompactBase.isChromaticTooltipsInstalled();
    }

    /**
     * 纯文本 tooltip——无物品目标的排队（widget tooltip 路径）。
     * <p>空行列表直接跳过（与 CatFrame {@code setTooltipForNextFrameInternal} 的空组件守卫一致）。</p>
     *
     * @param lines 拆分后的 tooltip 文字行
     */
    public static void drawTooltip(List<?> lines) {
        if (lines == null || lines.isEmpty()) return;
        com.slprime.chromatictooltips.TooltipHandler.drawHoveringText(lines);
    }

    /**
     * 物品 tooltip——带物品目标的排队（物品槽位路径）。
     * <p>物品使 Chromatic Tooltips 的物品系 enricher（属性图鉴/堆叠/热键分区等）完整生效；
     * 文字行照常进入 contextInfo 分区。</p>
     *
     * @param stack 悬停的物品（可能为 null，null 时退化为纯文本语义）
     * @param lines tooltip 文字行
     */
    public static void drawTooltip(ItemStack stack, List<?> lines) {
        if (lines == null || lines.isEmpty()) return;
        com.slprime.chromatictooltips.TooltipHandler.drawHoveringText(stack, lines);
    }
}
