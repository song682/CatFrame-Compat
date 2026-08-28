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
 * Recreates the rotation physics of ItemPhysic (Mixin edition) for CatFrame's
 * drop-item rendering.
 *
 * <p><b>Background</b>: all rotation injections of the ItemPhysic Mixin edition
 * ({@code injectRotations} / {@code applyRotationsBeforeRenderBlock} /
 * {@code injectRotationsItem} etc.) live in the vanilla rendering branch (the
 * else side) of {@code RenderItem.doRender}. When CatFrame takes over
 * drop-item rendering via Forge {@code IItemRenderer}
 * ({@code ForgeHooksClient.renderEntityItem} returning true short-circuits the
 * vanilla path), none of those injections run, so the drop item neither spins
 * nor flips. This Mixin applies the same rotation before the ENTITY branch of
 * {@link RenderJsonItemModel#renderItem} renders (before the pipeline flush),
 * aligning with ItemPhysic's GL transforms one by one.</p>
 *
 * <p><b>Guard</b>: only active when the Mixin edition of ItemPhysic is
 * installed and a drop-item entity is being rendered; the official edition
 * (ASM coremod) is already rejected at {@code CatFrameCompact.preInit}. The
 * rotation logic is invoked via {@link ItemPhysic#applyRotations} by
 * reflection (zero compile-time dependency).</p>
 *
 * <p>The injection target {@link RenderJsonItemModel} is a CatFrame class (not
 * obfuscated in releases), hence {@code remap = false}: the method descriptor
 * is identical in dev and release environments.</p>
 */
@Mixin(value = RenderJsonItemModel.class, remap = false)
public abstract class MixinRenderJsonItemModel {

    /**
     * Applies ItemPhysic-style rotation before the CatFrame renderer submits
     * vertices (pipeline flush): updates {@code rotationPitch} via reflection
     * (falling flip / landing reset / fluid and web slowdown), then applies the
     * GL rotation along the ItemPhysic injection path:
     * <ul>
     *   <li>Block items (DROPPED_BLOCK_GROUND): yaw around Y + pitch around X;</li>
     *   <li>Regular items (DROPPED_ITEM_GROUND): laid flat at 90° + yaw around Z
     *       + pitch around X.</li>
     * </ul>
     * Uniform scale and rotation commute (Forge presets scale(0.5) × this
     * rotation × the pipeline's counter-scale(2.0) = rotation), so the
     * counter-scale chain is unaffected.
     *
     * @param type  Forge item render type (only ENTITY takes effect)
     * @param stack the item stack being rendered
     * @param data  IItemRenderer varargs (data[0]=RenderBlocks, data[1]=entity)
     * @param ci    callback
     */
    @Inject(
            method = "renderItem(Lnet/minecraftforge/client/IItemRenderer$ItemRenderType;Lnet/minecraft/item/ItemStack;[Ljava/lang/Object;)V",
            at = @At("HEAD"))
    private void catframecompact$applyItemPhysicRotation(ItemRenderType type, ItemStack stack, Object[] data,
        CallbackInfo ci) {
        // Only for drop-item entities + the Mixin edition ItemPhysic environment
        if (type != ItemRenderType.ENTITY || !ItemPhysic.isMixinInstalled()) return;
        if (data == null || data.length <= 1 || !(data[1] instanceof EntityItem)) return;

        EntityItem item = (EntityItem) data[1];
        // No physics rotation inside an item frame (renderInFrame)
        if (RenderItem.renderInFrame) return;

        // Same injection condition as vanilla ItemPhysic: falling or landed
        if (item.prevPosY != item.posY || item.onGround) {
            // Update rotationPitch (fluid density/web slowdown, landing reset)
            ItemPhysic.applyRotations(item);
            if (item.rotationPitch > 360) item.rotationPitch = 0;

            if (stack != null && stack.getItem() instanceof ItemBlock) {
                // Block item: aligned with MixinRenderItem.injectRotations (3D branch)
                GL11.glRotatef(item.rotationYaw, 0.0F, 1.0F, 0.0F);
                GL11.glRotatef(item.rotationPitch, 1.0F, 0.0F, 0.0F);
            } else {
                // Regular item: aligned with injectRotationsItem + applyRotationsItem (2D branch)
                GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
                GL11.glRotatef(item.rotationYaw, 0.0F, 0.0F, 1.0F);
                GL11.glRotatef(item.rotationPitch, 1.0F, 0.0F, 0.0F);
            }
        }
    }
}
