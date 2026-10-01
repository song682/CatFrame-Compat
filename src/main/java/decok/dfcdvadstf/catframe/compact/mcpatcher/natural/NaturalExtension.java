package decok.dfcdvadstf.catframe.compact.mcpatcher.natural;

import com.prupe.mcpatcher.natural.NaturalProperties;
import com.prupe.mcpatcher.natural.NaturalTextures;
import decok.dfcdvadstf.catframe.model.render.IModelRenderExtension;
import decok.dfcdvadstf.catframe.model.render.api.RenderContext;
import decok.dfcdvadstf.catframe.model.render.api.RenderPhase;
import decok.dfcdvadstf.optifuture.config.MCPatcherForgeConfig;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;

/**
 * Render extension bridging OptiFuture's Natural Textures module into the
 * CatFrame pipeline: for every BLOCK_WORLD quad it queries OptiFuture's
 * {@link NaturalTextures} table for the per-position UV rotation / mirror
 * derived from the OptiFine position hash and forwards the result through
 * {@link RenderContext#uvOverride} (the native UV-override channel of
 * CatFrame >= 0.9.4).
 * <p>
 * Sibling of {@link RPNaturalExtension}, which bridges the same feature from
 * RPMCP for the {@code com.falsepattern.mcpatcher} family; this class covers
 * the MCPatcher-heritage {@code com.prupe.mcpatcher} family (OptiFuture). The
 * UV transform is the same geometric recipe in both bridges - a 90-degree
 * rotation (aspect-corrected for non-square sprites) followed by a horizontal
 * mirror, expressed in atlas space in TL/TR/BR/BL texture order - so a
 * natural.properties entry renders identically whichever bridge feeds it.
 * <p>
 * Class-loading isolation: this class references {@code com.prupe.mcpatcher}
 * and {@code decok.dfcdvadstf.optifuture} classes, so it must never be loaded
 * unless OptiFuture carries the natural module. The extension is only
 * registered behind the capability probe in
 * {@code CompactBase#isOptiFutureNaturalAvailable()}.
 * <p>
 * Random source: the per-face 3-bit value (bits 0-1 rotation, bit 2 flip) is
 * recomputed here from {@link NaturalTextures#getRandomBlockId(int, int, int)}
 * instead of using {@link NaturalTextures#getRandomValue(int)}, which reads the
 * static {@code randomBlockId} field seeded by OptiFuture's own RenderBlocks
 * mixins; the bridge may run on Beddium parallel chunk-compilation threads and
 * must not touch that shared field. The bit extraction is exactly what
 * getRandomValue performs.
 * <p>
 * Writes are conservative: the chain-inherited {@code uvOverride} is used as
 * the input baseline and nothing is written unless the transform actually
 * changed the UVs, so other extensions' results (e.g. the smooth-graphics
 * leaves {@code iconOverride}) are never clobbered.
 * <p>
 * Thread-safe: all working state lives in a {@link ThreadLocal} scratch buffer
 * (thread-confined, no cross-thread sharing), so the extension is safe for
 * Beddium parallel chunk compilation.
 */
public class NaturalExtension implements IModelRenderExtension {

    public static final NaturalExtension INSTANCE = new NaturalExtension();

    /**
     * Per-thread scratch buffers. The render path must not allocate per quad
     * (hot path), and chunks may be compiled on Beddium worker threads, so the
     * buffers are thread-confined instead of shared.
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
        // Respect OptiFuture's own natural-textures toggle, mirroring the guard
        // its RenderBlocks mixins use.
        if (!MCPatcherForgeConfig.instance().naturalTexturesEnabled) return;
        // No configured table (module off, loader dead-locked or no
        // natural.properties in any pack): cheap bail-out before the per-quad work.
        if (!NaturalTextures.isEnabled()) return;

        // Direction-less quads (cross etc.) have no side to salt the random with.
        if (ctx.world == null || ctx.block == null || ctx.quad.face == null) return;

        // Effective sprite: chain-inherited override first (e.g. smooth-graphics
        // leaves switching to *_opaque), baked icon otherwise. The same icon feeds
        // both the table lookup and the atlas-space math below.
        IIcon icon = ctx.iconOverride != null ? ctx.iconOverride : ctx.quad.icon;
        if (icon == null) return;
        NaturalProperties properties = NaturalTextures.getNaturalProperties(icon);
        if (properties == null) return;

        // Face index 0-5 matches OptiFuture's own numbering, which is the vanilla
        // face order: 0 = YNeg (bottom), 1 = YPos (top), 2 = ZNeg (north),
        // 3 = ZPos (south), 4 = XNeg (west), 5 = XPos (east).
        int face = ctx.quad.face.ordinal();
        // Per-face random bits: bits 0-1 select the rotation, bit 2 the flip.
        // Taken straight from the position hash; see the class javadoc for why
        // NaturalTextures.getRandomValue is not used here.
        int randomBits = (int) (NaturalTextures.getRandomBlockId(ctx.x, ctx.y, ctx.z) >> (face * 3));
        int rotation = properties.getRotation(randomBits);
        boolean flip = properties.getFlip(randomBits);
        if (rotation == 0 && !flip) return; // nothing to apply on this face

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

        // ---- 3. Apply the natural transform: rotate first, then mirror ----
        // Same geometric recipe as the RPMCP bridge's engine so both families
        // produce identical visuals for the same natural.properties entry.
        if (rotation != 0) rotateQuadUVs(rotation * Math.PI / 2D, icon, s.us, s.vs);
        if (flip) mirrorQuadUVs(s.us);

        // ---- 4. Publish only when the transform actually changed the quad ----
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

    /**
     * Rotates the four atlas-space quad corners around the sprite center by the
     * given angle. Intended for exact 90-degree steps only; the aspect
     * correction keeps non-square sprites at their original proportions (a
     * rotated 16x8 sprite would otherwise smear). Mirrors the geometry the
     * RPMCP natural engine applies so both bridges stay pixel-compatible.
     */
    private static void rotateQuadUVs(double angle, IIcon icon, double[] us, double[] vs) {
        final float lengthU = icon.getMaxU() - icon.getMinU();
        final float lengthV = icon.getMaxV() - icon.getMinV();
        final float centerU = icon.getMinU() + lengthU / 2F;
        final float centerV = icon.getMinV() + lengthV / 2F;

        angle %= 2D * Math.PI;
        final float rotSin = MathHelper.sin((float) angle);
        final float rotCos = MathHelper.cos((float) angle);

        final double aspectU = Math.abs(rotSin * ((lengthV / lengthU) - 1D)) + 1D;
        final double aspectV = 1F / aspectU;

        for (int i = 0; i < 4; i++) {
            double deltaU = (us[i] - centerU) * aspectU;
            double deltaV = (vs[i] - centerV) * aspectV;

            us[i] = (rotCos * deltaU) + (rotSin * deltaV) + centerU;
            vs[i] = (rotCos * deltaV) - (rotSin * deltaU) + centerV;
        }
    }

    /**
     * Mirrors the quad horizontally in atlas space by swapping the U values of
     * the left and right texture columns (TL with TR, BL with BR).
     */
    private static void mirrorQuadUVs(double[] us) {
        double swap = us[0];
        us[0] = us[1];
        us[1] = swap;

        swap = us[2];
        us[2] = us[3];
        us[3] = swap;
    }

    /** Per-thread scratch buffers (thread-confined, never shared across threads). */
    private static final class Scratch {
        /** Effective model-space UVs, [u0,v0,...,u3,v3] in quad-vertex order. */
        final float[] uv = new float[8];
        /** Atlas-space U/V in TL/TR/BR/BL order (transform input/output). */
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
