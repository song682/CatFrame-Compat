package decok.dfcdvadstf.catframe.compact.mixin.late;

import decok.dfcdvadstf.catframe.compact.physic.ItemPhysic;
import decok.dfcdvadstf.catframe.model.BakedModelCache;
import decok.dfcdvadstf.catframe.model.IItemStateProvider;
import decok.dfcdvadstf.catframe.model.ModelRegistry;
import decok.dfcdvadstf.catframe.model.core.baking.JsonModelBake;
import decok.dfcdvadstf.catframe.model.render.RenderJsonItemModel;
import decok.dfcdvadstf.catframe.model.render.api.RenderPhase;
import decok.dfcdvadstf.catframe.model.state.BlockStateModelPart;
import decok.dfcdvadstf.catframe.model.state.item.EvalResult;
import decok.dfcdvadstf.catframe.model.state.item.ItemStateModel;
import decok.dfcdvadstf.catframe.model.state.item.ItemStateNode;
import decok.dfcdvadstf.catframe.model.state.property.ItemProperties;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4d;
import java.util.List;
import java.util.Map;

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
 *   <li>the pose branch follows the geometry CatFrame actually baked
 *       ({@code gui_light == "front"} ⇒ flat sheet), falling back to the
 *       verbatim vanilla {@code doRender} selector
 *       ({@code getItemSpriteNumber() == 0 && instanceof ItemBlock &&
 *       RenderBlocks.renderItemIn3d(renderType)}) when that geometry cannot be
 *       introspected;</li>
 *   <li>the {@code prevPosY != posY || onGround} gate controls the yaw/flip
 *       pair only — the pitch rotation stays unconditional, exactly like
 *       {@code applyRotationsItem} does;</li>
 *   <li>{@code ClientPhysic.applyRotations} runs every frame (through
 *       {@link ItemPhysic#applyRotations}, a direct compile-time-verified call
 *       into the installed Mixin edition): an airborne item keeps accumulating
 *       its flip, while a resting one would have its pitch zeroed — that reset
 *       stands for flat sheets, and is superseded for block geometry by the
 *       landing-face preservation described below;</li>
 *   <li>factor order follows the GL call order (first call is the leftmost
 *       factor): 2D = {@code Rx(90°) × Rz(yaw) × Rx(pitch)},
 *       3D = {@code Ry(yaw) × Rx(pitch)}; the block branch additionally clamps
 *       {@code rotationPitch > 360} back to {@code 0}.</li>
 * </ul>
 *
 * <p><b>Landing-face preservation</b> — a deliberate extension of the Mixin
 * edition rather than a verbatim port. Because the edition zeroes a resting
 * item's tumble angle, a block always settles on the very same face: a log
 * thrown from a height tumbles on the way down and then always lies core-down,
 * never bark-down. Here the angle the item arrives with is kept instead, and
 * quantized to the nearest quarter turn
 * ({@link #catframecompact$snapToQuarterTurn}), so a block comes to rest on
 * whichever face was closest to the ground at the moment of landing — a log can
 * end up bark-down — while still lying flush instead of hovering tilted above
 * the surface. Block-like geometry only: flat sheets keep the vanilla flat rest
 * pose.</p>
 *
 * <p><b>Upstream mismatches corrected here as well</b> (both are
 * CatFrame/Forge semantics, independent of ItemPhysic):</p>
 * <ul>
 *   <li><b>Drop-item size</b> — {@code ForgeHooksClient.renderEntityItem} routes
 *       every block item into its 3D branch (the guard is
 *       {@code is3D || (block != null && RenderBlocks.renderItemIn3d(renderType))},
 *       so CatFrame returning {@code BLOCK_3D = false} for {@code ENTITY} does not
 *       change the branch) and pre-applies {@code scale(0.25)} there — {@code 0.5}
 *       only for render types 1/19/12/2 — while flat items take the {@code else}
 *       branch with {@code scale(0.5)}. CatFrame's {@code ENTITY}
 *       counter-transform (a fixed {@code scale(2.0)} up to 0.9.1) only cancels the
 *       flat-item factor, so block items render at half the size of flat ones;
 *       {@code catframecompact$compensateBlockItemScale} multiplies the missing
 *       {@code 0.5 / forgeScale} back in;</li>
 *   <li><b>Pose branch</b> — the vanilla selector keys off the block's render
 *       type table, but CatFrame renders the baked {@code items/*.json} model,
 *       whose geometry may disagree with it (a {@code builtin/generated} sheet on
 *       a block item, or a rail/plate-style block model on a render type vanilla
 *       would treat as 2D), which put the item into the wrong pose. The branch is
 *       therefore decided by the geometry CatFrame actually baked, with the
 *       vanilla selector kept as fallback when the geometry cannot be
 *       introspected.</li>
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
     * Supplies the ItemPhysic rotation — plus the block-item size compensation
     * and the landing-face preservation — as the pre-transform of CatFrame's
     * item pipeline for the drop item currently being rendered.
     *
     * @param preTransform the baked pre-transform of the pipeline
     *                     ({@code scale(2.0)} for {@code ENTITY} up to CatFrame
     *                     0.9.1); may be {@code null} in theory, then the rotation
     *                     alone is used
     * @return {@code R × preTransform} (size-compensated), or the argument
     *         unchanged when no drop item is being rendered / the compat layer
     *         is inactive
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
        // Assigned by the call site right before this invocation; null for
        // every non-drop pass (GUI / hand / item frame).
        EntityItem item = RenderJsonItemModel.getCurrentDroppedEntity();
        if (item == null) return preTransform;

        ItemStack stack = item.getEntityItem();
        if (stack == null || stack.getItem() == null) return preTransform;

        Item stackItem = stack.getItem();
        Block block = stackItem instanceof ItemBlock ? Block.getBlockFromItem(stackItem) : null;

        // 上游尺寸错位修正（与 ItemPhysic 无关）：Forge 对方块物品的 3D 分支
        // 预应用 scale(0.25)，而 CatFrame 的 ENTITY 反抵消只抵消了扁平物品
        // 分支的 scale(0.5) —— 把缺失的因子补回来。
        // Upstream size mismatch (ItemPhysic-independent): Forge pre-applies
        // scale(0.25) in its block-item 3D branch, while CatFrame's ENTITY
        // counter-transform only cancels the flat-item scale(0.5) — put the
        // missing factor back.
        preTransform = catframecompact$compensateBlockItemScale(preTransform, block);

        // Compat layer disabled by config, or the official (ASM) edition is
        // the installed one — nothing to recreate then.
        if (!ItemPhysic.isMixinInstalled()) return preTransform;

        // 刷新前先记下翻滚角：这就是物品此刻呈现的角度，也是落地那一帧的"到达角"
        // ——ClientPhysic 一旦判定物品静止就把 rotationPitch 归零，而落地要保持的
        // 恰恰是这个值。
        // The tumble angle is captured before the refresh: it is the angle the
        // item is showing right now — the one it arrives with on the landing
        // frame — which ClientPhysic is about to zero once the item rests.
        float tumblePitch = item.rotationPitch;

        // Same order as ItemPhysic: refresh rotationPitch first (landing reset,
        // airborne flip accumulation, fluid/web slowdown), then build the
        // rotation from it.
        ItemPhysic.applyRotations(item);

        // 姿态分支按 CatFrame 实际烘焙的几何决定（gui_light == "front" ⇒ 扁平
        // 薄片平面；否则为方块类几何），几何无法自省时退回 vanilla 判据。
        // Pose branch follows the geometry CatFrame actually baked
        // (gui_light == "front" => flat sheet, otherwise block-like geometry);
        // the vanilla selector is kept as fallback when that cannot be
        // introspected.
        Boolean flatGeometry = catframecompact$isFlatItemGeometry(stack);
        boolean renderAsBlock;
        if (flatGeometry != null) {
            // 方块物品渲染方块模型（保持方块朝向并翻滚）；烘焙为扁平薄片的
            // 物品（builtin/generated 语义）则像扁平物品一样躺倒。
            // Block items render the block model (upright, tumbling); items
            // baked as flat sheets (builtin/generated semantics) lie down like
            // flat items.
            renderAsBlock = stackItem instanceof ItemBlock && !flatGeometry;
        } else {
            renderAsBlock = stack.getItemSpriteNumber() == 0
                    && stackItem instanceof ItemBlock
                    && RenderBlocks.renderItemIn3d(block.getRenderType());
        }

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

            // 落地面保持：不再沿用 ClientPhysic 的归零，而是把到达角吸附到最近的
            // 1/4 圈，让方块停在"落地瞬间最接近地面的那个面"上（原木因此可以树皮面
            // 朝地，而不是永远木心朝地）。必须回写实体字段，否则下一帧 ClientPhysic
            // 的归零会把它抹掉。
            // Landing-face preservation: instead of following ClientPhysic's
            // reset, the angle the item arrives with is quantized to the nearest
            // quarter turn, so the block settles on the face that was closest to
            // the ground at the moment of landing (a log can rest bark-down
            // instead of always ending up core-down). Written back to the entity
            // field, otherwise ClientPhysic's next reset would wipe it.
            if (item.onGround) {
                tumblePitch = catframecompact$snapToQuarterTurn(tumblePitch);
                item.rotationPitch = tumblePitch;
            } else {
                // Still airborne: the refreshed angle is this frame's tumble.
                tumblePitch = item.rotationPitch;
            }

            // injectRotations: glRotatef(yaw, 0,1,0) then glRotatef(pitch, 1,0,0)
            step.setIdentity();
            step.rotY(Math.toRadians(item.rotationYaw));
            rotation.mul(step);
            step.setIdentity();
            step.rotX(Math.toRadians(tumblePitch));
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

        // R × preTransform (both pure uniform scales, which commute): this
        // equals the GL chain of the original mod — Forge's scale × R × the
        // pipeline's counter-scale (plus size compensation) and display
        // transform.
        if (preTransform == null) return rotation;
        rotation.mul(preTransform);
        return rotation;
    }

    /**
     * Quantizes a tumble angle to the nearest quarter turn.
     *
     * <p>Serves the landing-face preservation: an item arriving at, say,
     * {@code 137°} settles at {@code 90°}, the closest-to-ground face (a bark
     * side for a log), which also keeps the block flush with the surface instead
     * of leaving it hovering tilted over it. Idempotent — an already quantized
     * angle maps to itself — so it can be re-applied on every resting frame.</p>
     *
     * <p>把翻滚角吸附到最近的 1/4 圈（90° 的整数倍）：137° → 90°，使方块停在落地
     * 瞬间最接近地面的那个面，并贴合地面。幂等，可逐帧重复调用。</p>
     *
     * @param angle the tumble angle of the landing frame, in degrees
     * @return the angle rounded to the nearest multiple of {@code 90}
     */
    private static float catframecompact$snapToQuarterTurn(float angle) {
        return Math.round(angle / 90.0F) * 90.0F;
    }

    /**
     * Re-applies the size difference between Forge's two entity-item branches to
     * block items.
     *
     * <p>{@code ForgeHooksClient.renderEntityItem} routes every block item into
     * its 3D branch — the guard is
     * {@code is3D || (block != null && RenderBlocks.renderItemIn3d(renderType))},
     * so {@code BLOCK_3D == false} on {@code ENTITY} does not change the branch —
     * and pre-applies {@code scale(0.25)} there ({@code 0.5} only for render types
     * 1/19/12/2). CatFrame's {@code ENTITY} counter-transform is a fixed
     * {@code scale(2.0)} that cancels the {@code else} branch's {@code scale(0.5)}
     * only, so a block item renders at half the size of a flat one. This multiplies
     * the missing {@code 0.5 / forgeScale} back in ({@code 2.0} for render type 0
     * and friends, {@code 1.0} — a no-op — for the types that already get 0.5).
     *
     * <p>非方块物品（Forge 走 else 分支、预应用 scale(0.5)）不受影响，无需补偿。
     *
     * <p>Guarded by {@link #catframecompact$isBaselineEntityPreScale}: once the
     * upstream corrects its {@code ENTITY} counter-transform for block items, this
     * compensation switches itself off instead of double-scaling.
     *
     * @param preTransform the pre-transform passed to the model; may be
     *                     {@code null} in theory
     * @param block        the block of an {@code ItemBlock}, {@code null} for
     *                     plain items
     * @return the pre-transform with the missing size factor applied
     */
    private static Matrix4d catframecompact$compensateBlockItemScale(Matrix4d preTransform, @Nullable Block block) {
        if (block == null || !RenderBlocks.renderItemIn3d(block.getRenderType())) return preTransform;
        if (!catframecompact$isBaselineEntityPreScale(preTransform)) return preTransform;

        int renderType = block.getRenderType();
        // Mirrors ForgeHooksClient.renderEntityItem's 3D-branch scale by render type.
        double forgeScale = (renderType == 1 || renderType == 19 || renderType == 12 || renderType == 2) ? 0.5 : 0.25;
        double factor = 0.5 / forgeScale;
        if (factor == 1.0) return preTransform;

        Matrix4d compensation = new Matrix4d();
        compensation.setIdentity();
        compensation.m00 = factor;
        compensation.m11 = factor;
        compensation.m22 = factor;

        Matrix4d result = new Matrix4d(compensation);
        result.mul(preTransform);
        return result;
    }

    /**
     * Whether the given pre-transform is still CatFrame's unmodified
     * {@code ENTITY} counter-transform: an exact uniform {@code scale(2.0)}
     * ({@code computePreTransform} through CatFrame 0.9.1 returns it for every
     * drop item).
     *
     * <p>一旦上游针对物品类型修正了该反抵消（例如对方块物品直接返回
     * {@code scale(4.0)}），本检查即失效，
     * {@link #catframecompact$compensateBlockItemScale} 随之停止补偿，避免双重放大。
     *
     * @param preTransform the pre-transform passed to the model; may be
     *                     {@code null} in theory
     * @return {@code true} when it is a plain uniform {@code scale(2.0)}
     */
    private static boolean catframecompact$isBaselineEntityPreScale(@Nullable Matrix4d preTransform) {
        if (preTransform == null) return false;
        final double epsilon = 1.0e-6;
        return Math.abs(preTransform.m00 - 2.0) < epsilon
                && Math.abs(preTransform.m11 - 2.0) < epsilon
                && Math.abs(preTransform.m22 - 2.0) < epsilon;
    }

    /**
     * Introspects the geometry CatFrame is about to render for the given stack and
     * reports whether it is a flat 2D item sheet.
     *
     * <p>CatFrame bakes the {@code items/*.json} model — not the vanilla render
     * type table — so the pose must follow the baked quads' {@code gui_light}: flat
     * item sheets carry {@code "front"} ({@code builtin/generated} and everything
     * inheriting from it), block models carry {@code "side"}. The selected model is
     * resolved exactly like {@code ItemStateModel#render} does it: phase properties
     * → decision-tree evaluation → baked part lookup, using the phase
     * {@code RenderJsonItemModel.toRenderPhase} picks for {@code ENTITY}
     * ({@code ItemBlock} ⇒ {@code DROPPED_BLOCK_GROUND}).
     *
     * <p>与 CatFrame 自身渲染的开销同级（属性表构建 + 决策树求值 + 缓存查询），每帧
     * 每次掉落物渲染执行一次，无额外缓存。
     *
     * @param stack the drop item stack being rendered
     * @return {@code TRUE} = flat sheet, {@code FALSE} = block-like geometry,
     *         {@code null} = not introspectable (left to the vanilla selector)
     */
    @Nullable
    private static Boolean catframecompact$isFlatItemGeometry(ItemStack stack) {
        IItemStateProvider provider = ModelRegistry.getRegisteredItemModel(stack.getItem());
        if (!(provider instanceof ItemStateModel)) return null;
        ItemStateNode root = ((ItemStateModel) provider).getRootNode();
        if (root == null) return null;

        // Same phase RenderJsonItemModel.toRenderPhase(ENTITY, stack) selects.
        RenderPhase phase = stack.getItem() instanceof ItemBlock
                ? RenderPhase.DROPPED_BLOCK_GROUND
                : RenderPhase.DROPPED_ITEM_GROUND;

        Map<String, Comparable<?>> props = ItemProperties.buildProperties(stack, phase);
        EvalResult result = root.evaluate(props);
        if (result.isEmpty()) return null;

        boolean hasPart = false;
        for (String path : result.getModels()) {
            BlockStateModelPart part = BakedModelCache.INSTANCE.get(BakedModelCache.buildKey(path, 0, 0));
            if (part == null || part.isEmpty()) continue;
            hasPart = true;
            List<JsonModelBake.BakedQuad> quads = part.getAllQuads();
            if (!quads.isEmpty() && "front".equals(quads.get(0).guiLight)) return Boolean.TRUE;
        }
        return hasPart ? Boolean.FALSE : null;
    }
}
