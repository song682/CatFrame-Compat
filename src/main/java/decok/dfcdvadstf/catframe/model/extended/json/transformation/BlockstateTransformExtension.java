package decok.dfcdvadstf.catframe.model.extended.json.transformation;

import com.google.gson.JsonParseException;
import decok.dfcdvadstf.catframe.CatFrameCompat;
import decok.dfcdvadstf.catframe.model.ModelManagerDataLoader;
import decok.dfcdvadstf.catframe.model.RenderDispatcher;
import decok.dfcdvadstf.catframe.model.core.ModelJson;
import decok.dfcdvadstf.catframe.model.core.ModelResolver;
import decok.dfcdvadstf.catframe.model.render.IModelRenderExtension;
import decok.dfcdvadstf.catframe.model.render.api.RenderContext;
import decok.dfcdvadstf.catframe.model.render.api.RenderPhase;
import decok.dfcdvadstf.catframe.model.state.BlockstateJson;
import net.minecraft.block.Block;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4d;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BlockstateTransformExtension — applies the Forge model-level {@code transform}
 * field to CatFrame block renders.
 *
 * <p>CatFrame core reads its blockstate variants from the standard fields
 * ({@code model}, {@code x}, {@code y}, {@code z}, {@code uvlock}, {@code weight})
 * and keeps everything else in two raw snapshots:
 * {@link BlockstateJson.Variant#getRawJson()} for a variant object and
 * {@link ModelJson#raw} for a model's unrecognized top-level keys. This extension
 * is the reader of that channel: it picks up the {@code transform} field
 * (see {@link BlockstateTransform}) and installs the resulting matrix, so a model
 * can be translated / scaled / rotated by any angle and around any pivot - a
 * capability 1.7.10's blockstate form lacks, where {@code y} only supports quarter
 * turns and nothing else exists.
 *
 * <h3>Where the matrix goes</h3>
 * <p>The matrix is written into {@link RenderContext#displayTransform}, which
 * {@code QuadWriter} applies per vertex inside the block write loop: vertices are
 * in block space (0..1, one block = 1.0) and the block origin is added afterwards,
 * so the transform is a pure block-space operation - no GL state, no extra draw
 * call. Block phases do not receive a display transform from the builtin
 * {@code DisplayTransformExtension} (that one only serves item phases), so this
 * extension is the only writer on the block path; if another extension ever writes
 * one first, the two are composed with the block transform innermost.
 *
 * <p>Composition order matches Forge: the variant rotation is already baked into
 * the quads by the core bake step, so applying {@code transform} afterwards in
 * block space equals Forge's
 * {@code Ms x rootTransform x Ms^-1} conjugation of the model-state rotation
 * ({@code UnbakedGeometryHelper#applyRootTransform}) - the root transform is
 * expressed in the block's own coordinate system either way.
 *
 * <h3>Variant resolution</h3>
 * <p>A baked part carries no model path, so the transform is resolved from the
 * values the render context exposes ({@code block}, {@code blockstateProps},
 * {@code metadata}, position) by mirroring the core's own variant lookup:
 * {@link ModelManagerDataLoader#getBlockstateData(Block)} then the same key
 * selection and the same {@code normal} fallback. Results are cached per
 * (blockstate JSON instance, variant key); a resource reload replaces the
 * blockstate instance, which invalidates the entry by construction.
 *
 * <h3>Limitations (by construction, not oversights)</h3>
 * <ul>
 *   <li><b>Multipart blockstates are not transformed</b>: their cases are merged
 *       into one part whose quads carry no per-case identity, so a per-case
 *       transform cannot be attributed. Only {@code variants}-based blockstates are
 *       handled.</li>
 *   <li><b>Face culling is not re-evaluated</b>: the builtin cull extension looks at
 *       the quad's authored direction, before this matrix. A rotated model keeps the
 *       culling decisions of its unrotated self, the same relationship Forge has
 *       between root transforms and cull directions.</li>
 *   <li><b>Weighted variants are resolved per position</b> and therefore bypass the
 *       cache; the seed is the core's position seed, so the chosen variant matches
 *       the blockstate and provider paths (the resident state-model path uses the
 *       metadata seed instead - a divergence only visible on weighted entries that
 *       also carry a transform).</li>
 *   <li><b>Item phases are untouched</b>: a block's item form comes from its item
 *       state model, which owns its own transform chain.</li>
 *   <li><b>Bounds are unaffected</b>: collision / selection boxes stay the block's
 *       own data (see {@code ModelBounds} for deriving them from the model).</li>
 * </ul>
 *
 * <p>Thread safety: per the extension contract, the only per-thread state is the
 * last-hit cache (ThreadLocal); the shared caches are concurrent maps of immutable
 * matrices, and readers never mutate a cached instance.
 */
public final class BlockstateTransformExtension implements IModelRenderExtension {

    /** Singleton shared by every render. */
    public static final BlockstateTransformExtension INSTANCE = new BlockstateTransformExtension();

    /**
     * Upper bound on cached blockstate generations; each resource reload adds one
     * generation, so the cap keeps the leak bounded without needing a reload hook.
     */
    private static final int MAX_CACHED_BLOCKSTATES = 64;

    /** Per (blockstate instance, variant key) transform; a null matrix means "no transform". */
    private static final Map<BlockstateJson, ConcurrentHashMap<String, Resolved>> STATE_CACHE =
            new ConcurrentHashMap<>();

    /** Last resolution of the current thread, so the variant key is not rebuilt per quad. */
    private static final ThreadLocal<LastHit> LAST_HIT = new ThreadLocal<LastHit>() {
        @Override
        protected LastHit initialValue() {
            return new LastHit();
        }
    };

    private BlockstateTransformExtension() {
    }

    @Override
    public void apply(RenderContext ctx) {
        RenderPhase phase = ctx.phase;
        if (phase != RenderPhase.BLOCK_WORLD && phase != RenderPhase.BLOCK_DESTROY) {
            return;
        }
        if (ctx.block == null) {
            return;
        }
        Matrix4d transform = resolve(ctx);
        if (transform == null) {
            return;
        }

        Matrix4d current = ctx.displayTransform;
        if (current == null) {
            ctx.displayTransform = transform;
        } else {
            // Compose with a foreign matrix, keeping the block transform innermost
            Matrix4d composed = new Matrix4d();
            composed.mul(current, transform);
            ctx.displayTransform = composed;
        }
    }

    /**
     * Drops every cached transform. Not required for resource reloads (those replace
     * the blockstate instances that key the cache), but available for callers that
     * keep custom model state alive.
     */
    public static void clearCache() {
        STATE_CACHE.clear();
    }

    // ==================== Resolution ====================

    /**
     * Resolves the transform of the variant being rendered.
     *
     * @return the block-space matrix, or {@code null} when the variant has none
     */
    @Nullable
    private static Matrix4d resolve(RenderContext ctx) {
        Block block = ctx.block;
        Map<String, String> props = ctx.blockstateProps;
        int metadata = ctx.metadata;

        LastHit hit = LAST_HIT.get();
        if (hit.block == block && hit.meta == metadata && !hit.weighted
                && (hit.props == props || (props != null && props.equals(hit.props)))) {
            return hit.matrix;
        }

        Matrix4d matrix = null;
        boolean weighted = false;
        try {
            BlockstateJson blockstate = ModelManagerDataLoader.getBlockstateData(block);
            if (blockstate == null || blockstate.variants == null) {
                return null;
            }

            String key = variantKey(blockstate, props, metadata);
            BlockstateJson.VariantEntry entry = blockstate.variants.get(key);
            if (entry == null) {
                // Same fallback the core applies when the property key does not match
                entry = blockstate.variants.get("normal");
            }
            if (entry == null) {
                return null;
            }

            weighted = entry.isArray();
            int seed = ctx.x * 3129871 ^ ctx.z * 116129781 ^ ctx.y;
            BlockstateJson.Variant variant = entry.getVariant(seed);
            if (variant == null) {
                return null;
            }

            matrix = weighted ? transformOf(variant) : cachedTransform(blockstate, key, variant);
        } finally {
            hit.block = block;
            hit.props = props;
            hit.meta = metadata;
            hit.weighted = weighted;
            hit.matrix = matrix;
        }
        return matrix;
    }

    /**
     * Returns the transform for a variant key, resolving it at most once per
     * blockstate generation.
     */
    @Nullable
    private static Matrix4d cachedTransform(BlockstateJson blockstate, String key,
                                            BlockstateJson.Variant variant) {
        ConcurrentHashMap<String, Resolved> perState = STATE_CACHE.get(blockstate);
        if (perState == null) {
            if (STATE_CACHE.size() >= MAX_CACHED_BLOCKSTATES) {
                STATE_CACHE.clear();
            }
            ConcurrentHashMap<String, Resolved> created = new ConcurrentHashMap<>();
            ConcurrentHashMap<String, Resolved> existing = STATE_CACHE.putIfAbsent(blockstate, created);
            perState = (existing != null) ? existing : created;
        }

        Resolved resolved = perState.get(key);
        if (resolved == null) {
            resolved = new Resolved(transformOf(variant));
            perState.put(key, resolved);
        }
        return resolved.matrix;
    }

    /**
     * Builds the variant key the core used for this state.
     *
     * <p>Mirrors the two paths of the core lookup: properties win when present
     * ({@link RenderDispatcher#buildVariantKey}), otherwise the metadata-keyed forms.
     * The case-insensitive pass covers the typed-definition path, which serialises
     * values through {@code Property#getName} while the exposed property map uses
     * {@code value.toString()} - the two differ only in letter case for enum values.
     */
    private static String variantKey(BlockstateJson blockstate, @Nullable Map<String, String> props,
                                     int metadata) {
        Map<String, BlockstateJson.VariantEntry> variants = blockstate.variants;
        if (props != null) {
            String key = RenderDispatcher.buildVariantKey(props);
            if (variants.containsKey(key)) {
                return key;
            }
            for (String candidate : variants.keySet()) {
                if (candidate.equalsIgnoreCase(key)) {
                    return candidate;
                }
            }
            return key;
        }
        if (hasMetaVariantKeys(blockstate)) {
            return "meta=" + metadata;
        }
        return findNumberKeyForMeta(blockstate, metadata);
    }

    /**
     * Reads the transform of a variant: the variant object first (the more specific
     * declaration), then the model's raw top-level keys.
     */
    @Nullable
    private static Matrix4d transformOf(BlockstateJson.Variant variant) {
        try {
            Matrix4d matrix = BlockstateTransform.parseField(variant.getRawJson());
            if (matrix != null) {
                return matrix;
            }
            String modelPath = variant.model;
            if (modelPath == null || modelPath.isEmpty()) {
                return null;
            }
            ModelJson model = ModelResolver.resolve(modelPath);
            return (model != null) ? BlockstateTransform.parseField(model.raw) : null;
        } catch (JsonParseException e) {
            CatFrameCompat.logger.warn("[BlockstateTransform] Invalid transform on model {}: {}",
                    variant.model, e.getMessage());
            return null;
        }
    }

    // ==================== Metadata-keyed variant helpers ====================

    /**
     * Whether the blockstate keys its variants as {@code meta=<n>}.
     */
    private static boolean hasMetaVariantKeys(BlockstateJson blockstate) {
        for (String key : blockstate.variants.keySet()) {
            if (key == null || !key.startsWith("meta=")) {
                continue;
            }
            try {
                Integer.parseInt(key.substring(5));
                return true;
            } catch (NumberFormatException ignored) {
                // Not a metadata key; keep scanning
            }
        }
        return false;
    }

    /**
     * Picks the variant key for a metadata value: the plain numeric key, then
     * {@code normal}, then the first declared key.
     */
    private static String findNumberKeyForMeta(BlockstateJson blockstate, int metadata) {
        String metaKey = String.valueOf(metadata);
        if (blockstate.variants.containsKey(metaKey)) {
            return metaKey;
        }
        if (blockstate.variants.containsKey("normal")) {
            return "normal";
        }
        for (String key : blockstate.variants.keySet()) {
            return key;
        }
        return "normal";
    }

    // ==================== Cache value types ====================

    /** One cached resolution; {@code matrix} is null when the variant has no transform. */
    private static final class Resolved {
        final Matrix4d matrix;

        Resolved(@Nullable Matrix4d matrix) {
            this.matrix = matrix;
        }
    }

    /**
     * Last resolution of one thread. {@code weighted} marks a position-dependent
     * resolution, which must never be reused for another position.
     */
    private static final class LastHit {
        Block block;
        Map<String, String> props;
        int meta;
        boolean weighted;
        Matrix4d matrix;
    }
}
