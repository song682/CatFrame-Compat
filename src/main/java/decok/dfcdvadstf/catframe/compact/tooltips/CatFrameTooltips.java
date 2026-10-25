package decok.dfcdvadstf.catframe.compact.tooltips;

import decok.dfcdvadstf.catframe.CompatConfig;
import decok.dfcdvadstf.catframe.compact.CompactBase;
import decok.dfcdvadstf.catframe.ui.GuiGraphicsExtractor;
import decok.dfcdvadstf.catframe.ui.screens.inventory.tooltip.DefaultTooltipPositioner;
import decok.dfcdvadstf.catframe.world.tooltip.TooltipComponent;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * CatFrame 自有 tooltip 渲染在「原版式界面」上的入口。
 * <p>
 * Route tooltips of vanilla-style screens through CatFrame's deferred tooltip
 * pipeline - the same one CatFrame's own screens use (nine-patch themed frame,
 * {@code TOOLTIP_STYLE}, structured {@link TooltipComponent}s such as the bundle
 * grid, {@code ClientTooltipPositioner} placement).
 * </p>
 *
 * <p><b>Why this is a separate layer from the two existing ones.</b> On vanilla-style
 * screens the tooltip is painted by {@code GuiScreen.renderToolTip} /
 * {@code GuiScreen.drawHoveringText}, which is exactly where the other two layers
 * already take over when they are installed:</p>
 * <ul>
 *   <li><b>Chromatic Tooltips</b> injects into both methods at {@code HEAD}
 *       ({@code priority = 999}) and replaces every tooltip wholesale, on every
 *       screen - CatFrame screens included;</li>
 *   <li><b>NotEnoughItems</b> rewrites the {@code renderToolTip} call site inside
 *       {@code GuiContainer.drawScreen} with an ASM {@code MethodReplacer}, so item
 *       tooltips of every container screen are painted by its own frame.</li>
 * </ul>
 * <p>Both therefore own the vanilla path already, and this layer steps aside when
 * either is present - it only fills the gap they leave: vanilla-style screens
 * without NEI and without Chromatic Tooltips.</p>
 *
 * <p><b>Frame lifecycle.</b> The queue is driven by Forge's
 * {@code DrawScreenEvent.Pre/Post} (CatFrame's {@code ClientScreenGraphicsHandler}),
 * which wrap {@code currentScreen.drawScreen} for <b>all</b> screens - so a tooltip
 * queued from a foreign screen at any point of its {@code drawScreen} is flushed at
 * the end of that same frame, on top of the screen.</p>
 */
public final class CatFrameTooltips {

    private CatFrameTooltips() {}

    /**
     * 本层是否生效——配置开启，且 NEI 与 Chromatic Tooltips 均未安装。
     * <p>Needs the config switch (default off) and the absence of both competing
     * renderers. Mod detection stays in {@link CompactBase}, config gating in
     * {@link CompatConfig} - neither touches the NEI nor the CT classes, so this
     * check is safe to run when they are not on the classpath.</p>
     */
    public static boolean isAvailable() {
        return CompatConfig.vanillaTooltipsCompat
                && !CompactBase.isNEIInstalled()
                && !CompactBase.isChromaticTooltipsInstalled();
    }

    /**
     * 物品 tooltip——携带物品目标的排队（结构化组件与样式随之生效）。
     * <p>Delegates to <code>setTooltipForNextFrame(Font, ItemStack, int, int)</code>,
     * i.e. the very same entry CatFrame's own container screens use: text from
     * {@code Screen.getTooltipFromItem}, structured component from
     * {@code ItemTooltipImages}, style from {@code DataComponents.TOOLTIP_STYLE}.</p>
     *
     * @param stack 悬停的物品（非空，调用方已守卫）
     * @param font  物品自身的字体或界面默认字体
     * @param xo    鼠标 X
     * @param yo    鼠标 Y
     */
    public static void drawItemTooltip(ItemStack stack, FontRenderer font, int xo, int yo) {
        GuiGraphicsExtractor.getInstance().setTooltipForNextFrame(font, stack, xo, yo);
    }

    /**
     * 纯文本 tooltip——无物品目标的排队（聊天栏悬停文字、创造模式标签名等）。
     * <p>{@code replaceExisting} is on: vanilla paints the tooltip immediately at the
     * call site, so the last call of a frame is the one on top. The deferred queue
     * reproduces that by letting the last queued tooltip replace the earlier ones.</p>
     *
     * @param font  绘制该 tooltip 的字体
     * @param lines 已带格式码的文字行（原版调用方传入的列表）
     * @param xo    鼠标 X
     * @param yo    鼠标 Y
     */
    public static void drawTooltip(FontRenderer font, List<String> lines, int xo, int yo) {
        if (lines == null || lines.isEmpty()) return;
        GuiGraphicsExtractor.getInstance().setTooltipForNextFrame(
                font, lines, Optional.<TooltipComponent>empty(),
                DefaultTooltipPositioner.INSTANCE, xo, yo, true, null);
    }
}
