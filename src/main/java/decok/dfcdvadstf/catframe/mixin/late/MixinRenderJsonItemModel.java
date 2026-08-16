package decok.dfcdvadstf.catframe.mixin.late;

import decok.dfcdvadstf.catframe.compact.ItemPhysic;
import decok.dfcdvadstf.catframe.model.render.RenderJsonItemModel;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 为 CatFrame 掉落物渲染复刻 ItemPhysic（Mixin 版）的旋转物理。
 *
 * <p><b>背景</b>：ItemPhysic Mixin 版的旋转注入（{@code injectRotations} /
 * {@code applyRotationsBeforeRenderBlock} / {@code injectRotationsItem} 等）全部位于
 * {@code RenderItem.doRender} 的原版渲染分支（else 侧）。CatFrame 通过 Forge
 * {@code IItemRenderer} 接管掉落物渲染（{@code ForgeHooksClient.renderEntityItem}
 * 返回 true 短路原版路径）时，这些注入一概不执行，掉落物因此既不旋转也不翻滚。
 * 该 Mixin 在 {@link RenderJsonItemModel#renderItem} 的 ENTITY 分支渲染前
 * （管线 flush 之前）补上同一套旋转，与 ItemPhysic 的 GL 变换逐一对齐。</p>
 *
 * <p><b>守卫</b>：仅在 Mixin 版 ItemPhysic 安装且渲染掉落物实体时生效；
 * 官方版（ASM coremod）已在 {@code CatFrameCompact.preInit} 被拒绝启动。
 * 旋转逻辑通过 {@link ItemPhysic#applyRotations} 反射调用（编译期零依赖）。</p>
 *
 * <p>注入目标 {@link RenderJsonItemModel} 是 CatFrame 的类（发布版不混淆），
 * 因此使用 {@code remap = false}，方法描述符在开发/发布环境一致。</p>
 */
@Mixin(value = RenderJsonItemModel.class, remap = false)
public abstract class MixinRenderJsonItemModel {

    /**
     * 在 CatFrame 渲染器提交顶点（管线 flush）前应用 ItemPhysic 式旋转：
     * 反射更新 {@code rotationPitch}（下落翻滚 / 落地归零 / 流体与蛛网减速），
     * 再按 ItemPhysic 注入路径施加 GL 旋转：
     * <ul>
     *   <li>方块物品（DROPPED_BLOCK_GROUND）：yaw 绕 Y + pitch 绕 X；</li>
     *   <li>普通物品（DROPPED_ITEM_GROUND）：90° 躺平 + yaw 绕 Z + pitch 绕 X。</li>
     * </ul>
     * 均匀缩放与旋转可交换（Forge 预置 scale(0.5) × 本旋转 × 管线反抵消 scale(2.0) = 旋转），
     * 反抵消链不受影响。
     *
     * @param type  Forge 物品渲染类型（仅 ENTITY 生效）
     * @param stack 正在渲染的物品栈
     * @param data  IItemRenderer 变参（data[0]=RenderBlocks, data[1]=实体）
     * @param ci    回调
     */
    @Inject(
            method = "renderItem(Lnet/minecraftforge/client/IItemRenderer$ItemRenderType;Lnet/minecraft/item/ItemStack;[Ljava/lang/Object;)V",
            at = @At("HEAD"))
    private void catframecompact$applyItemPhysicRotation(ItemRenderType type, ItemStack stack, Object[] data,
        CallbackInfo ci) {
        // 仅掉落物实体 + Mixin 版 ItemPhysic 环境
        if (type != ItemRenderType.ENTITY || !ItemPhysic.isMixinInstalled()) return;
        if (data == null || data.length <= 1 || !(data[1] instanceof EntityItem)) return;

        EntityItem item = (EntityItem) data[1];
        // 展示框（renderInFrame）中不应用物理旋转
        if (RenderItem.renderInFrame) return;

        // 与原版 ItemPhysic 的注入条件一致：下落中或已落地
        if (item.prevPosY != item.posY || item.onGround) {
            // 更新 rotationPitch（含流体密度/蛛网减速、落地归零）
            ItemPhysic.applyRotations(item);
            if (item.rotationPitch > 360) item.rotationPitch = 0;

            if (stack != null && stack.getItem() instanceof ItemBlock) {
                // 方块物品：对齐 MixinRenderItem.injectRotations（3D 分支）
                GL11.glRotatef(item.rotationYaw, 0.0F, 1.0F, 0.0F);
                GL11.glRotatef(item.rotationPitch, 1.0F, 0.0F, 0.0F);
            } else {
                // 普通物品：对齐 injectRotationsItem + applyRotationsItem（2D 分支）
                GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
                GL11.glRotatef(item.rotationYaw, 0.0F, 0.0F, 1.0F);
                GL11.glRotatef(item.rotationPitch, 1.0F, 0.0F, 0.0F);
            }
        }
    }
}
