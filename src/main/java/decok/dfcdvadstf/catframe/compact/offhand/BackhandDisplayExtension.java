package decok.dfcdvadstf.catframe.compact.offhand;

import decok.dfcdvadstf.catframe.model.core.ModelJson;
import decok.dfcdvadstf.catframe.model.core.baking.JsonModelBake.BakedQuad;
import decok.dfcdvadstf.catframe.model.render.IModelRenderExtension;
import decok.dfcdvadstf.catframe.model.render.RenderJsonItemModel;
import decok.dfcdvadstf.catframe.model.render.api.RenderContext;
import decok.dfcdvadstf.catframe.model.render.api.RenderPhase;
import decok.dfcdvadstf.catframe.model.state.BlockStateModelPart;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.player.EntityPlayer;
import xonin.backhand.api.core.BackhandUtils;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4d;
import javax.vecmath.Vector3d;
import java.util.List;

/**
 * Backhand left-hand display bridge.
 *
 * <p>Rebuilds the display-transform half of CatFrame's item rendering for
 * Backhand's offhand passes, following the modern (26.1.2) left-hand semantics
 * value-for-value. Everything else (arm anchor, equip/swing/use animations,
 * the preTransform inverse cancellation) stays Backhand's and CatFrame's
 * exactly as for the main hand; this is the single semantic delta "the offhand
 * uses the left-hand display transform".</p>
 *
 * <h3>The modern left-hand semantics being reproduced (26.1.2 evidence)</h3>
 * <ul>
 *   <li><b>Resolution</b> - {@code ItemTransforms.Deserializer}
 *       (cuboid/ItemTransforms.java L52-62): a model omitting
 *       {@code firstperson_lefthand} / {@code thirdperson_lefthand} falls back
 *       to the <b>corresponding right-hand entry verbatim</b>. Modern vanilla
 *       {@code generated.json} relies on exactly that (it authors no left-hand
 *       entries); {@code handheld.json} / {@code block.json} author theirs.</li>
 *   <li><b>Application</b> - {@code ItemTransform.apply(applyLeftHandFix, pose)}
 *       (cuboid/ItemTransform.java L20-42): in left-hand contexts
 *       ({@code ItemDisplayContext.leftHand()}, ItemStackRenderState L266) the
 *       resolved entry - authored or fallback alike - is applied with
 *       <b>negated translation.x, rotation.y and rotation.z</b>; rotation.x,
 *       translation.y/z, scale and the trailing {@code translate(-0.5)} centering
 *       stay untouched. A field-level negation, <b>not</b> a full mirror
 *       conjugation M*X*M - a conjugation would additionally flip the centering
 *       to (+0.5,-0.5,-0.5) and displace the item by a full block.</li>
 *   <li><b>Anchor</b> - {@code ItemInHandRenderer} (first person) and
 *       {@code ItemInHandLayer.submitArmWithItem} (third person: translateToHand
 *       + XP(-90) x YP(180) + {@code (isLeftHand ? -1 : 1)} offsets) mirror the
 *       handedness of the <i>anchor</i> through +/-1 invert factors only - the
 *       item geometry itself is never mirrored.</li>
 * </ul>
 *
 * <h3>Why that maps onto Backhand</h3>
 * <p>Backhand replays the whole vanilla right-hand pass under a full GL mirror
 * ({@code glScalef(-1,1,1)} + cull-face flip): the anchor chain CatFrame's
 * preTransform leaves (after inverse cancellation) auto-conjugates under that
 * mirror into the left-hand anchor - positionally identical to the modern
 * invert-factor anchor (mirrored translations, mirrored Y/Z rotation angles,
 * X untouched). With the anchor handedness provided by the mirror, writing the
 * modern left-hand display matrix (resolved entry + the tx/ry/rz negation fix)
 * into {@link RenderContext#displayTransform} reproduces the modern left-hand
 * pose: the rotations and the Y/Z translations come out exactly as modern's,
 * because the display negation and the anchor conjugation compose the same way
 * on both sides (verified vertex-by-vertex against the modern chain).</p>
 *
 * <p><b>Bounded deviation</b>: Backhand's outer mirror is forced GL state, so
 * the display translation.x - applied before the anchor - ends up
 * double-negated (the mirror negates it again after modern's own negation).
 * The offhand item therefore sits 2*|translation.x| laterally beside the
 * exact-modern position: invisible for vanilla-convention models (1.13px is
 * about 1.5px at display scale), growing for models with large hand
 * translations. Collapsing the deviation fully would require counter-mirroring
 * the item geometry inside the display matrix (restoring modern's un-mirrored
 * chirality) plus flipping Backhand's cull-face compensation per part -
 * deliberately out of scope for a compat layer; the item keeps Backhand's
 * mirrored look (same as its vanilla 2D sprites in the offhand).</p>
 *
 * <h3>Offhand-pass detection</h3>
 * <ul>
 *   <li><b>First person</b> ({@code ITEM_HAND_FIRST_PERSON}): Backhand's
 *       mirrored pass runs inside {@code BackhandUtils.useOffhandItem(...)},
 *       which swaps {@code inventory.currentItem} to the offhand slot for the
 *       whole call. CatFrame's hand-pass flush happens synchronously inside
 *       that window (no render scope is active on this path, so every submit
 *       flushes immediately), so the live {@code currentItem} field read at
 *       flush time is the marker itself - no stack identity needed. The
 *       offhand's active-use pose (eat/block/bow) renders through this same
 *       mirrored pass, hence it is deliberately <b>not</b> excluded.</li>
 *   <li><b>Third person</b> ({@code ITEM_HAND_THIRD_PERSON}): Backhand hands
 *       the exact offhand stack reference to the renderer with no slot swap,
 *       so the marker is stack identity against
 *       {@link BackhandUtils#getOffhandItem} of the holder (via
 *       {@link RenderJsonItemModel#getCurrentHolderEntity()}); works for the
 *       local player in F5 and for other players in multiplayer.</li>
 * </ul>
 *
 * <h3>Lifecycle / performance</h3>
 * <p>Registered at the mod default priority 0, i.e. <b>after</b> the builtin
 * chain head ({@code DisplayTransformExtension} sits at the builtin base
 * -1000), so this bridge reads whatever the builtin wrote and replaces it.
 * The verdict and the left-hand matrix are computed once per part in
 * {@link #beforePart}; {@link #apply} then only installs it, adding the
 * per-quad stack-identity check for the third person. Per the extension
 * thread-safety contract, the pending matrix lives in a ThreadLocal (Beddium
 * compile threads pass through {@code beforePart} for block phases, which
 * clears it first and never sets it).</p>
 *
 * <h3>Known upstream issues (reported, not patched here)</h3>
 * <ul>
 *   <li>CatFrame {@code RenderJsonItemModel.resolveHeadSlotKind} keys head-slot
 *       ownership off {@code stack != entity.getHeldItem()}; in third person
 *       there is no slot swap, so an <b>ItemBlock held in the offhand</b> is
 *       misclassified as a head-slot render ({@code ITEM_HEAD} phase + head
 *       preTransform) and renders wrong regardless of this bridge - needs a
 *       CatFrame core fix.</li>
 *   <li>CatFrame's builtin {@code generated} model carries a placeholder
 *       {@code firstperson_lefthand} with rotation {@code {0,0,0}}; modern
 *       {@code generated.json} authors <b>no</b> left-hand entry (the verbatim
 *       right-hand fallback above is the mechanism), so the placeholder should
 *       be removed upstream - until then this bridge honors it as authored
 *       data and generated-style items face wrong in the offhand first
 *       person.</li>
 * </ul>
 */
public final class BackhandDisplayExtension implements IModelRenderExtension {

    /** Singleton shared by every render (render logic is stateless besides the ThreadLocal). */
    public static final BackhandDisplayExtension INSTANCE = new BackhandDisplayExtension();

    /**
     * The left-hand display matrix baked for the current part by
     * {@link #beforePart}; null when the current part is not an offhand pass.
     * ThreadLocal per the extension thread-safety contract (Beddium compile
     * threads pass through the lifecycle for block phases without ever
     * setting it).
     */
    private static final ThreadLocal<Matrix4d> PENDING_LEFT_MATRIX = new ThreadLocal<>();

    private BackhandDisplayExtension() {}

    @Override
    public void beforePart(List<BakedQuad> allQuads, RenderPhase phase, BlockStateModelPart part) {
        // Self-healing: clear any leftover (an escaped exception mid-part cannot
        // poison the next part), then re-evaluate for this part.
        PENDING_LEFT_MATRIX.remove();
        if (!BackHand.isEnabled()) return;

        String displayKey;
        if (phase == RenderPhase.ITEM_HAND_FIRST_PERSON) {
            // Full verdict available here: first-person detection needs no stack.
            if (!isOffhandFirstPersonPass()) return;
            displayKey = "firstperson_lefthand";
        } else if (phase == RenderPhase.ITEM_HAND_THIRD_PERSON) {
            // Partial verdict: the holder must be a player (cheap gate here);
            // the stack identity is only verifiable per-quad in apply (ctx.stack).
            if (!(RenderJsonItemModel.getCurrentHolderEntity() instanceof EntityPlayer)) return;
            displayKey = "thirdperson_lefthand";
        } else {
            return;
        }

        PENDING_LEFT_MATRIX.set(computeLeftHandMatrix(part, displayKey));
    }

    @Override
    public void apply(RenderContext ctx) {
        Matrix4d leftMatrix = PENDING_LEFT_MATRIX.get();
        if (leftMatrix == null) return;
        // Third person: finalize the verdict with the stack identity (the holder
        // gate already ran in beforePart). First person: verdict is final.
        if (ctx.phase == RenderPhase.ITEM_HAND_THIRD_PERSON
                && !BackHand.isOffhandThirdPersonPass(ctx.stack, RenderJsonItemModel.getCurrentHolderEntity())) {
            return;
        }
        // Last writer wins: installed after the builtin DisplayTransformExtension
        // (mod priority 0 > builtin base -1000), replacing its right-hand matrix.
        ctx.displayTransform = leftMatrix;
    }

    @Override
    public void afterPart() {
        PENDING_LEFT_MATRIX.remove();
    }

    /**
     * Whether the current render is Backhand's <b>mirrored</b> first-person offhand
     * pass. Client-only detection (lives here, not in {@link BackHand}, so the
     * utility class stays common-side loadable).
     *
     * @return true when the local player's current-item slot is the offhand
     *         slot - Backhand's {@code useOffhandItem} window marker
     */
    private static boolean isOffhandFirstPersonPass() {
        EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
        if (player == null) return false;
        return player.inventory.currentItem == BackhandUtils.getOffhandSlot(player);
    }

    // ==================== Left-hand resolution + baking ====================

    /**
     * Resolves the left-hand display entry with the modern
     * {@code ItemTransforms.Deserializer} fallback order and bakes it into a
     * matrix with the modern {@code ItemTransform.apply(applyLeftHandFix=true)}
     * negation:
     * <ol>
     *   <li>the part's authored {@code firstperson_lefthand} /
     *       {@code thirdperson_lefthand} entry (ModelJson retains every display
     *       slot name, including the ones CatFrame does not yet consume);</li>
     *   <li>missing: the <b>corresponding right-hand entry verbatim</b> (the
     *       modern fallback - modern vanilla {@code generated.json} relies on
     *       exactly this);</li>
     *   <li>both missing: the right-hand default values of the vanilla
     *       {@code generated.json} convention (the same table CatFrame's builtin
     *       {@code getDefaultTransform} substitutes for the right-hand keys).</li>
     * </ol>
     * Whatever entry is resolved, the bake applies the modern left-hand fix:
     * translation.x, rotation.y and rotation.z are negated; rotation.x,
     * translation.y/z, scale and the -0.5 centering stay untouched.
     *
     * @param part       the part being rendered (provides the authored display map)
     * @param displayKey "firstperson_lefthand" / "thirdperson_lefthand"
     * @return the baked left-hand display matrix
     */
    private static Matrix4d computeLeftHandMatrix(@Nullable BlockStateModelPart part, String displayKey) {
        String rightHandKey = displayKey.replace("lefthand", "righthand");

        ModelJson.DisplayTransform dt = null;
        if (part != null && part.getDisplay() != null) {
            // Authored left-hand entry first; modern fallback: the right-hand
            // entry verbatim.
            dt = part.getDisplay().get(displayKey);
            if (dt == null) {
                dt = part.getDisplay().get(rightHandKey);
            }
        }
        if (dt == null) {
            dt = defaultRightHandTransform(rightHandKey);
        }
        return computeDisplayMatrix(dt, true);
    }

    /**
     * Right-hand default display entries, aligned with the vanilla
     * {@code generated.json} convention - the same values the builtin
     * {@code DisplayTransformExtension.getDefaultTransform} table substitutes
     * for the right-hand keys. Serves the models that declare no hand display
     * entry at all; through {@link #computeLeftHandMatrix} they get the same
     * fallback modern gives them.
     *
     * @param rightHandKey "firstperson_righthand" / "thirdperson_righthand"
     * @return the default DisplayTransform data
     */
    private static ModelJson.DisplayTransform defaultRightHandTransform(String rightHandKey) {
        ModelJson.DisplayTransform dt = new ModelJson.DisplayTransform();
        dt.rotation = new float[]{0, 0, 0};
        dt.translation = new float[]{0, 0, 0};
        dt.scale = new float[]{1, 1, 1};
        switch (rightHandKey) {
            case "firstperson_righthand":
                dt.rotation = new float[]{0, -90, 25};
                dt.translation = new float[]{1.13f, 3.2f, 1.13f};
                dt.scale = new float[]{0.68f, 0.68f, 0.68f};
                break;
            case "thirdperson_righthand":
                dt.translation = new float[]{0, 3, 1};
                dt.scale = new float[]{0.55f, 0.55f, 0.55f};
                break;
            default:
                break;
        }
        return dt;
    }

    /** Translation clamp (pixels), aligned with the builtin display transform table. */
    private static final float TRANSLATION_CLAMP = 80.0f;

    /** Scale clamp, aligned with the builtin display transform table. */
    private static final float SCALE_CLAMP = 4.0f;

    /**
     * Bakes a display transform into a matrix with the exact same semantics as
     * the builtin {@code DisplayTransformExtension.computeMatrix} (26.1
     * {@code ItemTransform} alignment, field defaults and clamps included):
     * <pre>{@code v' = T(display) × RX × RY × RZ × S(display) × T(-0.5) × v}</pre>
     * (translation in pixels, 0.0625 = block units; rotation free in degrees;
     * scale clamped; construction follows GL post-multiply order). When
     * {@code leftHandFix} is set, the modern {@code ItemTransform.apply}
     * left-hand negation is applied to the field values before baking:
     * translation.x, rotation.y and rotation.z flip, everything else stays.
     *
     * @param dt           the display transform data
     * @param leftHandFix  whether to apply the modern left-hand negation
     * @return the 4x4 matrix, or null when dt is null (no display transform)
     */
    @Nullable
    private static Matrix4d computeDisplayMatrix(@Nullable ModelJson.DisplayTransform dt, boolean leftHandFix) {
        if (dt == null) return null;

        float rx = (dt.rotation != null && dt.rotation.length > 0) ? dt.rotation[0] : 0f;
        float ry = (dt.rotation != null && dt.rotation.length > 1) ? dt.rotation[1] : 0f;
        float rz = (dt.rotation != null && dt.rotation.length > 2) ? dt.rotation[2] : 0f;
        float tx = clamp((dt.translation != null && dt.translation.length > 0) ? dt.translation[0] : 0f,
                -TRANSLATION_CLAMP, TRANSLATION_CLAMP);
        float ty = clamp((dt.translation != null && dt.translation.length > 1) ? dt.translation[1] : 0f,
                -TRANSLATION_CLAMP, TRANSLATION_CLAMP);
        float tz = clamp((dt.translation != null && dt.translation.length > 2) ? dt.translation[2] : 0f,
                -TRANSLATION_CLAMP, TRANSLATION_CLAMP);
        float sx = clamp((dt.scale != null && dt.scale.length > 0) ? dt.scale[0] : 1f,
                -SCALE_CLAMP, SCALE_CLAMP);
        float sy = clamp((dt.scale != null && dt.scale.length > 1) ? dt.scale[1] : 1f,
                -SCALE_CLAMP, SCALE_CLAMP);
        float sz = clamp((dt.scale != null && dt.scale.length > 2) ? dt.scale[2] : 1f,
                -SCALE_CLAMP, SCALE_CLAMP);

        // The modern left-hand fix (ItemTransform.apply, applyLeftHandFix=true):
        // translation.x, rotation.y and rotation.z negate; rotation.x,
        // translation.y/z, scale and the centering stay untouched.
        if (leftHandFix) {
            tx = -tx;
            ry = -ry;
            rz = -rz;
        }

        Matrix4d m = new Matrix4d();
        m.setIdentity();

        // 4) translate(display) - pixels x 0.0625 = block units
        Matrix4d tDisplay = new Matrix4d();
        tDisplay.setIdentity();
        tDisplay.setTranslation(new Vector3d(tx * 0.0625, ty * 0.0625, tz * 0.0625));
        m.mul(tDisplay);

        // 3) rotate XYZ
        Matrix4d r = new Matrix4d();
        r.rotX(Math.toRadians(rx));
        m.mul(r);
        r.rotY(Math.toRadians(ry));
        m.mul(r);
        r.rotZ(Math.toRadians(rz));
        m.mul(r);

        // 2) scale
        Matrix4d s = new Matrix4d();
        s.setIdentity();
        s.m00 = sx; s.m11 = sy; s.m22 = sz;
        m.mul(s);

        // 1) translate(-0.5) - center offset (display-centered model space);
        //    uniform, hence untouched by the left-hand fix
        Matrix4d tCenter = new Matrix4d();
        tCenter.setIdentity();
        tCenter.setTranslation(new Vector3d(-0.5, -0.5, -0.5));
        m.mul(tCenter);

        return m;
    }

    /**
     * Clamps a value into [min, max], aligned with the builtin display transform table.
     */
    private static float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }
}
