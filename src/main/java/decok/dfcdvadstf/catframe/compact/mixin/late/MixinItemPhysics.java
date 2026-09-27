package decok.dfcdvadstf.catframe.compact.mixin.late;

import decok.dfcdvadstf.catframe.compact.physic.ItemPhysic;
import decok.dfcdvadstf.catframe.model.render.RenderJsonItemModel;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import javax.vecmath.Matrix4d;

/**
 * Recreates the ItemPhysic (Mixin edition) ground-rest rotation physics for the
 * drop items rendered by CatFrame.
 *
 * <p><b>Background</b>: every rotation injection of ItemPhysic lives in the
 * vanilla (non-Forge) branch of {@code RenderItem.doRender} — the 3D block
 * branch ({@code injectRotations} + {@code applyRotationsBeforeRenderBlock})
 * and the 2D sheet branch reached through {@code renderDroppedItem}
 * ({@code injectRotationsItem} + {@code applyRotationsItem}). Once CatFrame
 * takes an item over through Forge's {@code IItemRenderer}
 * ({@code ForgeHooksClient.renderEntityItem} returns {@code true} and
 * short-circuits {@code doRender}), none of those injections run: a dropped
 * sheet item keeps the upright ground pose described by the model
 * ({@code display.ground}) instead of lying flat with its north/south faces
 * against the ground — which is the ItemPhysic signature look.</p>
 *
 * <p><b>Approach</b>: CatFrame bakes all vertex transforms in Java
 * ({@code preTransform × transformation × display}) before handing the
 * vertices to the Tessellator, so the rotation is injected the same way: this
 * Mixin rewrites the {@code preTransform} argument of the single
 * {@code IItemStateProvider#render(stack, phase, preTransform)} call inside
 * {@link RenderJsonItemModel#renderItem} to {@code R × preTransform}. For
 * {@code ItemRenderType.ENTITY} that pre-transform is a pure uniform
 * {@code scale(2.0)} (it cancels Forge's {@code scale(0.5)}), so it commutes
 * with the rotation, and left-multiplying reproduces the GL state of the
 * original mod, where the {@code glRotatef} calls are emitted outside the
 * model-space translations and therefore rotate around the entity render
 * origin.</p>
 *
 * <p><b>Fidelity</b> — mirrored from the original mod one to one:</p>
 * <ul>
 *   <li>branch selector copied verbatim from vanilla {@code doRender}:
 *       {@code getItemSpriteNumber() == 0 && instanceof ItemBlock &&
 *       RenderBlocks.renderItemIn3d(renderType)};</li>
 *   <li>the {@code prevPosY != posY || onGround} gate controls the yaw/flip
 *       pair only — the pitch rotation stays unconditional, exactly like
 *       {@code applyRotationsItem} does;</li>
 *   <li>{@code ClientPhysic.applyRotations} runs every frame (through
 *       {@link ItemPhysic#applyRotations}, reflection, zero compile-time
 *       dependency), so a landing item's pitch resets to {@code 0} while an
 *       airborne one keeps accumulating its flip;</li>
 *   <li>factor order follows the GL call order (first call is the leftmost
 *       factor): 2D = {@code Rx(90°) × Rz(yaw) × Rx(pitch)},
 *       3D = {@code Ry(yaw) × Rx(pitch)}; the block branch additionally clamps
 *       {@code rotationPitch > 360} back to {@code 0}.</li>
 * </ul>
 *
 * <p>The rendered entity is read from
 * {@link RenderJsonItemModel#getCurrentDroppedEntity()} — assigned right
 * before the rewritten call and cleared after it — so GUI / hand / frame
 * passes are naturally ignored. The target is a CatFrame class (never
 * obfuscated), hence {@code remap = false}.</p>
 */
@Mixin(value = RenderJsonItemModel.class, remap = false)
public abstract class MixinItemPhysics {

    /**
     * Supplies the ItemPhysic rotation as the pre-transform of CatFrame's item
     * pipeline for the drop item currently being rendered.
     *
     * @param preTransform the baked pre-transform of the pipeline
     *                     ({@code scale(2.0)} for {@code ENTITY}); may be
     *                     {@code null} in theory, then the rotation alone is used
     * @return {@code R × preTransform}, or the argument unchanged when the
     *         compat layer is inactive or no drop item is being rendered
     */
    @ModifyArg(
            method = "renderItem(Lnet/minecraftforge/client/IItemRenderer$ItemRenderType;Lnet/minecraft/item/ItemStack;[Ljava/lang/Object;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Ldecok/dfcdvadstf/catframe/model/IItemStateProvider;render(Lnet/minecraft/item/ItemStack;Ldecok/dfcdvadstf/catframe/model/render/api/RenderPhase;Ljavax/vecmath/Matrix4d;)V",
                    remap = false),
            index = 2,
            remap = false)
    private Matrix4d catframecompact$applyItemPhysicRotation(Matrix4d preTransform) {
        // Compat layer disabled by config, or the official (ASM) edition is
        // the installed one — nothing to recreate then.
        if (!ItemPhysic.isMixinInstalled()) return preTransform;

        // Assigned by the call site right before this invocation; null for
        // every non-drop pass (GUI / hand / item frame).
        EntityItem item = RenderJsonItemModel.getCurrentDroppedEntity();
        if (item == null) return preTransform;

        ItemStack stack = item.getEntityItem();
        if (stack == null || stack.getItem() == null) return preTransform;

        // Same order as ItemPhysic: refresh rotationPitch first (landing reset,
        // airborne flip accumulation, fluid/web slowdown), then build the
        // rotation from it.
        ItemPhysic.applyRotations(item);

        // Verbatim copy of the vanilla RenderItem.doRender branch selector:
        // 3D block items rotate with the block model, everything else takes the
        // 2D sheet path (laid flat).
        boolean renderAsBlock = stack.getItemSpriteNumber() == 0
                && stack.getItem() instanceof ItemBlock
                && RenderBlocks.renderItemIn3d(Block.getBlockFromItem(stack.getItem()).getRenderType());

        // ItemPhysic's gate for the yaw/flip pair: vertically moving or resting
        // on ground. The pitch rotation below is not gated.
        boolean groundedOrMoving = item.prevPosY != item.posY || item.onGround;

        // Composite built by right-multiplication so that the factor order
        // matches the glRotatef call order of the original mod.
        Matrix4d rotation = new Matrix4d();
        rotation.setIdentity();
        Matrix4d step = new Matrix4d();

        if (renderAsBlock) {
            // applyRotationsBeforeRenderBlock: clamp the accumulated pitch.
            if (item.rotationPitch > 360.0F) item.rotationPitch = 0.0F;
            if (!groundedOrMoving) return preTransform;

            // injectRotations: glRotatef(yaw, 0,1,0) then glRotatef(pitch, 1,0,0)
            step.setIdentity();
            step.rotY(Math.toRadians(item.rotationYaw));
            rotation.mul(step);
            step.setIdentity();
            step.rotX(Math.toRadians(item.rotationPitch));
            rotation.mul(step);
        } else {
            // injectRotationsItem: glRotatef(90, 1,0,0) then glRotatef(yaw, 0,0,1)
            // — the vanilla Y spin between them is disabled by ItemPhysic.
            if (groundedOrMoving) {
                step.setIdentity();
                step.rotX(Math.toRadians(90.0));
                rotation.mul(step);
                step.setIdentity();
                step.rotZ(Math.toRadians(item.rotationYaw));
                rotation.mul(step);
            }

            // applyRotationsItem: glRotatef(pitch, 1,0,0), unconditional
            step.setIdentity();
            step.rotX(Math.toRadians(item.rotationPitch));
            rotation.mul(step);
        }

        // R × S(2.0): the uniform scale commutes, so this equals the GL chain
        // of the original mod — Forge's scale(0.5) × R × the pipeline's
        // counter-scale and display transform.
        if (preTransform == null) return rotation;
        rotation.mul(preTransform);
        return rotation;
    }
}
