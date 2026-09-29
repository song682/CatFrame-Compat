package decok.dfcdvadstf.catframe.compact.mcpatcher.natural;

import com.falsepattern.mcpatcher.internal.config.ModuleConfig;
import com.falsepattern.mcpatcher.internal.modules.common.Side;
import com.falsepattern.mcpatcher.internal.modules.natural.NaturalTexturesEngine;
import decok.dfcdvadstf.catframe.model.render.IModelRenderExtension;
import decok.dfcdvadstf.catframe.model.render.api.RenderContext;
import decok.dfcdvadstf.catframe.model.render.api.RenderPhase;
import net.minecraft.util.IIcon;

/**
 * Render extension bridging RPMCP (Right Proper MCPatcher) Natural Textures into the
 * CatFrame pipeline: for every BLOCK_WORLD quad it asks RPMCP's
 * {@link NaturalTexturesEngine} for the per-position UV rotation / mirror and forwards
 * the result through {@link RenderContext#uvOverride} (the native UV-override channel of
 * CatFrame >= 0.9.4).
 * <p>
 * Kept as a separate class from {@link NaturalExtension} for class-loading isolation:
 * this one references {@code com.falsepattern.mcpatcher} classes, so it must never be
 * loaded when RPMCP is absent. The extension is only registered when {@code mcpatcher}
 * is detected, and the per-quad check of RPMCP's own
 * {@link ModuleConfig#isNaturalTexturesEnabled()} mirrors the guard used by RPMCP's
 * RenderBlocks mixins.
 * <p>
 * <b>Space conversion</b>: RPMCP's engine speaks <em>atlas-space</em> UVs in
 * TL/TR/BR/BL texture order (same contract as its RenderBlocks mixins), while CatFrame's
 * {@code uvOverride} is <em>model-space 0-16</em> in quad-vertex order. This bridge
 * therefore forward-maps the effective UVs to atlas space with the very same
 * {@code getInterpolatedU/V} the pipeline uses, classifies the four quad corners into
 * TL/TR/BR/BL, runs the engine, and inverse-maps the result back to model space (exact
 * two-point linear inversion, f(0)/f(16)). Quads whose UVs do not form a clean corner
 * bijection (or that carry no face) are left untouched.
 * <p>
 * Writes are conservative: the chain-inherited {@code uvOverride} is used as the input
 * baseline and nothing is written unless the engine actually produced a transform, so
 * other extensions' results (e.g. the smooth-graphics leaves {@code iconOverride}) are
 * never clobbered.
 * <p>
 * Thread-safe: all working state lives in a {@link ThreadLocal} scratch buffer
 * (thread-confined, no cross-thread sharing), so the extension is safe for Beddium
 * parallel chunk compilation.
 */
public class RPNaturalExtension implements IModelRenderExtension {

    public static final RPNaturalExtension INSTANCE = new RPNaturalExtension();

    /**
     * Per-thread scratch buffers. The render path must not allocate per quad (hot
     * path), and chunks may be compiled on Beddium worker threads, so the buffers are
     * thread-confined instead of shared.
     */
    private static final ThreadLocal<Scratch> SCRATCH = new ThreadLocal<Scratch>() {
        @Override
        protected Scratch initialValue() {
            return new Scratch();
        }
    };

    @Override
    public void apply(RenderContext ctx) {
        if (ctx.phase != RenderPhase.BLOCK_WORLD) return;
        // Respect RPMCP's own natural-textures toggle. The combined gate
        // (mixins loaded && feature enabled) also guarantees the engine's resource
        // table has been populated before it is queried.
        if (!ModuleConfig.isNaturalTexturesEnabled()) return;

        // Mirror RPMCP's "ignore blocks rendered in inventory / player's hand"
        // guard: the engine skips (0,0,0) queries itself; cheaper to skip here.
        if (ctx.x == 0 && ctx.y == 0 && ctx.z == 0) return;

        // Direction-less quads (cross etc.) have no side to salt the random with.
        if (ctx.world == null || ctx.block == null || ctx.quad.face == null) return;
        Side side = Side.fromMCDirection(ctx.quad.face.ordinal());
        if (side == null) return;

        // Effective sprite: chain-inherited override first (e.g. smooth-graphics
        // leaves switching to *_opaque), baked icon otherwise.
        IIcon icon = ctx.iconOverride != null ? ctx.iconOverride : ctx.quad.icon;
        if (icon == null) return;

        Scratch s = SCRATCH.get();

        // ---- 1. Load the effective UVs and classify the quad corners ----
        // Effective = chain-inherited uvOverride if present, baked model UVs otherwise.
        final float[] prev = ctx.effectiveUvOverride();
        double minU = Double.MAX_VALUE, maxU = -Double.MAX_VALUE;
        double minV = Double.MAX_VALUE, maxV = -Double.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            float u = (prev != null) ? prev[i * 2] : ctx.quad.up[i];
            float v = (prev != null) ? prev[i * 2 + 1] : ctx.quad.vp[i];
            s.uv[i * 2] = u;
            s.uv[i * 2 + 1] = v;
            if (u < minU) minU = u;
            if (u > maxU) maxU = u;
            if (v < minV) minV = v;
            if (v > maxV) maxV = v;
        }
        double spanU16 = maxU - minU;
        double spanV16 = maxV - minV;
        if (spanU16 <= 0.0 || spanV16 <= 0.0) return; // degenerate quad

        double epsU = spanU16 * 1.0e-3;
        double epsV = spanV16 * 1.0e-3;
        int bits = 0;
        final int[] qIdx = s.qIdx;
        for (int i = 0; i < 4; i++) {
            double u = s.uv[i * 2];
            double v = s.uv[i * 2 + 1];
            boolean atMinU = (u - minU) <= epsU;
            boolean atMaxU = (maxU - u) <= epsU;
            if (atMinU == atMaxU) return; // non-corner or degenerate UV
            boolean atMinV = (v - minV) <= epsV;
            boolean atMaxV = (maxV - v) <= epsV;
            if (atMinV == atMaxV) return;
            int slot;
            if (atMinV) {
                slot = atMaxU ? 1 : 0; // top row: TR : TL
            } else {
                slot = atMaxU ? 2 : 3; // bottom row: BR : BL
            }
            if ((bits & (1 << slot)) != 0) return; // duplicate corner
            bits |= 1 << slot;
            qIdx[slot] = i;
        }
        if (bits != 0xF) return; // not a full bijection

        // ---- 2. Forward-map to atlas space (same interpolators QuadWriter uses) ----
        final double baseU = icon.getInterpolatedU(0.0);
        final double baseV = icon.getInterpolatedV(0.0);
        final double atlasSpanU = icon.getInterpolatedU(16.0) - baseU;
        final double atlasSpanV = icon.getInterpolatedV(16.0) - baseV;
        if (atlasSpanU == 0.0 || atlasSpanV == 0.0) return; // degenerate sprite
        for (int k = 0; k < 4; k++) {
            int i = qIdx[k];
            double au = icon.getInterpolatedU(s.uv[i * 2]);
            double av = icon.getInterpolatedV(s.uv[i * 2 + 1]);
            s.us[k] = au;
            s.vs[k] = av;
            s.origUs[k] = au;
            s.origVs[k] = av;
        }

        // ---- 3. Run the RPMCP engine (atlas space, TL/TR/BR/BL order) ----
        NaturalTexturesEngine.applyNaturalTexture(ctx.x, ctx.y, ctx.z, side, icon, s.us, s.vs);

        // ---- 4. Publish only when the engine actually transformed the quad ----
        // (0-degree rotation short-circuits inside the engine; a miss leaves the
        // arrays untouched, so an unchanged result means "no natural texture".)
        double maxDelta = 0.0;
        for (int k = 0; k < 4; k++) {
            double du = Math.abs(s.us[k] - s.origUs[k]);
            double dv = Math.abs(s.vs[k] - s.origVs[k]);
            if (du > maxDelta) maxDelta = du;
            if (dv > maxDelta) maxDelta = dv;
        }
        if (maxDelta <= 1.0e-7) return; // leave the inherited value untouched

        // ---- 5. Inverse-map back to model space and publish ----
        // Exact for the linear interpolator: model = (atlas - f(0)) * 16 / (f(16) - f(0)).
        for (int k = 0; k < 4; k++) {
            int i = qIdx[k];
            s.out[i * 2] = (float) ((s.us[k] - baseU) / atlasSpanU * 16.0);
            s.out[i * 2 + 1] = (float) ((s.vs[k] - baseV) / atlasSpanV * 16.0);
        }
        ctx.uvOverride = s.out;
    }

    /** Per-thread scratch buffers (thread-confined, never shared across threads). */
    private static final class Scratch {
        /** Effective model-space UVs, [u0,v0,...,u3,v3] in quad-vertex order. */
        final float[] uv = new float[8];
        /** Atlas-space U/V in TL/TR/BR/BL order (engine input/output). */
        final double[] us = new double[4];
        final double[] vs = new double[4];
        /** Pre-transform copies for the "did anything change" check. */
        final double[] origUs = new double[4];
        final double[] origVs = new double[4];
        /** TL/TR/BR/BL -> quad vertex index. */
        final int[] qIdx = new int[4];
        /** uvOverride payload (model space, quad-vertex order). */
        final float[] out = new float[8];
    }
}
