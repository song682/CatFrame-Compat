package decok.dfcdvadstf.catframe.bounds;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import decok.dfcdvadstf.catframe.CatFrameCompact;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ModelBounds — derives block bounds from CatFrame JSON block models.
 *
 * <p>ModelBounds derives block bounds (collision box / selection box) from
 * CatFrame JSON block models: it reads the {@code from}/{@code to} of every
 * {@code elements} entry in the model JSON (0~16 pixel space), unions them
 * into 0~1 block space and clamps, then binds the result to the block via
 * {@link Block#setBlockBounds}. This provides a capability that high versions
 * no longer have but is often expected: deriving bounds automatically from
 * JSON block models, filling the gap of 1.7.10 lacking the VoxelShape system
 * (collision shapes of every vanilla version are independent hand-written
 * shape data and have never been generated from render models; this is the
 * bridging facility provided by this mod).
 *
 * <p>Design notes:
 * <ul>
 *   <li>Shape data is deliberately read only from classpath resources — an
 *       intentional trade-off rather than a simplification: collision
 *       detection must not be affected by client resource packs (vanilla
 *       shapes are code/data constants too; resource packs only override
 *       rendering), and dedicated servers must keep working (a server does
 *       not load JSON models; depending on the client render pipeline would
 *       break server-side collision). Note: this path only rules out
 *       resource-pack influence; it does not claim to prevent unpacking and
 *       modifying the jar itself.</li>
 *   <li>The {@code parent} inheritance chain is resolved from classpath
 *       resources on its own (a child that defines {@code elements} overrides
 *       the parent entirely, matching vanilla semantics); the depth limit and
 *       cycle detection follow the core library convention.</li>
 *   <li>Simple-version limitation: {@code rotation} on an element is ignored
 *       and the unrotated {@code from}/{@code to} are used directly, so the
 *       bounding box of models with rotated parts may be oversized.</li>
 *   <li>This utility is positioned as an <b>extension-layer capability</b>
 *       above CatFrame core (core ports back high-version rendering features,
 *       this mod builds bridging facilities on top of it): in 1.7.10 collision
 *       detection is driven entirely by the instance fields set via
 *       {@link Block#setBlockBounds} (see
 *       {@code getCollisionBoundingBoxFromPool}), which is exactly the anchor
 *       where the model-derived result can take effect.</li>
 *   <li>Applicability boundary: {@link Block#setBlockBounds} writes instance
 *       fields shared by the whole Block singleton (minX…maxZ), so different
 *       metadata of the same block cannot get different bounds via a one-shot
 *       {@link #applyFromModel}; state-varying blocks should override
 *       {@code setBlockBoundsBasedOnState} to set bounds per state instead of
 *       calling it once at registration time.</li>
 *   <li>The union AABB is oversized for non-convex models; 1.7.10 supports
 *       multiple collision boxes per block (e.g. fences override
 *       {@code addCollisionBoxesToList} to emit per-part boxes); such models
 *       can use {@link #computeElementBoxes} / {@link #addElementCollisionBoxes}
 *       to emit per-element boxes.</li>
 *   <li>Collision is part of game balance rather than an accessory of
 *       rendering: vanilla deliberately keeps visuals and logic inconsistent
 *       (cactus collision is smaller than the displayed box to avoid
 *       scratching, fence collision is higher than the visual), so
 *       {@link #applyFromModel} is an optional entry — callers may always
 *       override the derived result manually with {@link Block#setBlockBounds}.</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>{@code
 * // reads /assets/catframe/models/block/my_block.json (+ parent chain)
 * ModelBounds.applyFromModel(myBlock, "catframe", "block/my_block");
 *
 * // non-convex models: override addCollisionBoxesToList instead
 * ModelBounds.addElementCollisionBoxes(world, x, y, z, mask, list, entity, "catframe", "block/my_block");
 * }</pre>
 */
public final class ModelBounds {

    /** Maximum parent-chain depth, aligned with the core library convention. */
    private static final int MAX_DEPTH = 16;

    /** Default namespace for unqualified parent references. */
    private static final String DEFAULT_NAMESPACE = "minecraft";

    private static final Gson GSON = new Gson();

    /**
     * Per-model element box cache (qualified path → per-element 6-tuple array),
     * shared by union and multi-box computation; only successful results are
     * cached so failures can be retried.
     */
    private static final Map<String, float[][]> BOX_CACHE = new ConcurrentHashMap<>();

    private ModelBounds() {
    }

    // ==================== Data model ====================

    /**
     * Minimal model JSON projection: only the fields bounds computation needs;
     * the rest is ignored by Gson.
     */
    private static class ModelData {
        public String parent;
        public List<Element> elements;
    }

    private static class Element {
        public float[] from;
        public float[] to;
    }

    // ==================== Public API ====================

    /**
     * Computes the union bounds of all elements in the given model JSON.
     *
     * <p>Values are converted from the 0~16 pixel space to 0~1 block space and
     * clamped to [0, 1]. Element {@code rotation} is ignored (simple version).
     *
     * @param reader JSON content source; closed by the caller
     * @return bounds as {minX, minY, minZ, maxX, maxY, maxZ}, or {@code null}
     *         if the model defines no valid elements
     */
    public static float[] computeBounds(Reader reader) {
        ModelData model = parse(reader);
        if (model == null) {
            return null;
        }
        List<Element> elements = model.elements;
        // Consistent with the two-arg overload: a model carrying only a parent
        // reference falls back to resolving that chain from the classpath.
        if ((elements == null || elements.isEmpty()) && model.parent != null && !model.parent.isEmpty()) {
            elements = collectElements(qualify(model.parent), 0, new HashSet<String>());
        }
        return unionOf(boxesOf(elements));
    }

    /**
     * Loads a model by namespace + path, resolves its parent chain from the
     * classpath, and computes the resulting bounds.
     *
     * <p>The resolved path is {@code /assets/<namespace>/models/<modelPath>.json}.
     * A child model defining {@code elements} overrides its parent's elements
     * entirely (vanilla semantics); otherwise elements are inherited from the
     * nearest parent that defines them.
     *
     * @param namespace resource namespace, e.g. "catframe"
     * @param modelPath model path without extension, e.g. "block/my_block"
     * @return bounds as {minX, minY, minZ, maxX, maxY, maxZ}, or {@code null}
     *         if the model (or its parent chain) yields no valid elements
     */
    public static float[] computeBounds(String namespace, String modelPath) {
        return unionOf(elementBoxes(namespace, modelPath));
    }

    /**
     * Computes the per-element boxes of a model (resolved incl. parent chain),
     * for blocks that need multiple collision boxes instead of one union AABB.
     *
     * <p>Each box is {minX, minY, minZ, maxX, maxY, maxZ} in 0~1 block space.
     *
     * @param namespace resource namespace, e.g. "catframe"
     * @param modelPath model path without extension, e.g. "block/my_block"
     * @return one box per valid element, or {@code null} if none
     */
    public static float[][] computeElementBoxes(String namespace, String modelPath) {
        return elementBoxes(namespace, modelPath);
    }

    /**
     * Adds the model's per-element collision boxes to the given list, mirroring
     * vanilla's {@code Block.addCollisionBoxesToList} contract (only boxes
     * intersecting the mask are added, offset to world coordinates).
     *
     * <p>Intended to be called from a block's own {@code addCollisionBoxesToList}
     * override for non-convex models, where a single union AABB would be
     * overly inclusive.
     *
     * @param world     the world
     * @param x, y, z   block position
     * @param mask      the mask AABB to test intersection against
     * @param out       list to receive intersecting boxes
     * @param entity    colliding entity, passed through for signature parity with
     *                  vanilla (unused, same as vanilla fences)
     * @param namespace resource namespace
     * @param modelPath model path without extension
     */
    @SuppressWarnings({"unchecked", "rawtypes"}) // raw List mirrors vanilla addCollisionBoxesToList signature
    public static void addElementCollisionBoxes(World world, int x, int y, int z,
                                                AxisAlignedBB mask, List out, Entity entity,
                                                String namespace, String modelPath) {
        float[][] boxes = elementBoxes(namespace, modelPath);
        if (boxes == null) {
            return;
        }
        for (float[] b : boxes) {
            AxisAlignedBB box = AxisAlignedBB.getBoundingBox(
                    x + b[0], y + b[1], z + b[2],
                    x + b[3], y + b[4], z + b[5]);
            if (mask.intersectsWith(box)) {
                out.add(box);
            }
        }
    }

    /**
     * Computes bounds from the given model and binds them to the block via
     * {@link Block#setBlockBounds}. No-op if the model yields no valid elements.
     *
     * <p>Suited to registration-time, metadata-independent bounds. Since
     * {@link Block#setBlockBounds} writes fields shared by the whole Block
     * singleton, state-varying blocks must override
     * {@code setBlockBoundsBasedOnState} instead of relying on this one-shot call.
     *
     * @param block     target block
     * @param namespace resource namespace, e.g. "catframe"
     * @param modelPath model path without extension, e.g. "block/my_block"
     * @return {@code true} if bounds were computed and applied
     */
    public static boolean applyFromModel(Block block, String namespace, String modelPath) {
        float[] bounds = computeBounds(namespace, modelPath);
        if (bounds == null) {
            CatFrameCompact.logger.warn(
                    "[ModelBounds] No valid elements found for model {}:{}, bounds of block {} left unchanged.",
                    namespace, modelPath, Block.blockRegistry.getNameForObject(block));
            return false;
        }
        block.setBlockBounds(bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]);
        return true;
    }

    // ==================== Internals ====================

    /**
     * Resolves the element list of a namespaced model (incl. parent chain).
     */
    private static List<Element> resolveElements(String namespace, String modelPath) {
        return collectElements(namespace + ":" + modelPath, 0, new HashSet<String>());
    }

    /**
     * Returns cached per-element boxes for the given model.
     */
    private static float[][] elementBoxes(String namespace, String modelPath) {
        String key = namespace + ":" + modelPath;
        float[][] cached = BOX_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        float[][] boxes = boxesOf(resolveElements(namespace, modelPath));
        if (boxes != null) {
            BOX_CACHE.put(key, boxes);
        }
        return boxes;
    }

    /**
     * Recursively collects elements along the parent chain (a child that
     * defines elements overrides them entirely).
     */
    private static List<Element> collectElements(String qualifiedPath, int depth, Set<String> visiting) {
        if (depth > MAX_DEPTH) {
            CatFrameCompact.logger.error("[ModelBounds] Model parent chain too deep for: {}", qualifiedPath);
            return null;
        }
        if (!visiting.add(qualifiedPath)) {
            CatFrameCompact.logger.error("[ModelBounds] Circular model dependency detected at: {}", qualifiedPath);
            return null;
        }
        try {
            ModelData model = loadModel(qualifiedPath);
            if (model == null) {
                return null;
            }
            if (model.elements != null && !model.elements.isEmpty()) {
                return model.elements;
            }
            if (model.parent != null && !model.parent.isEmpty()) {
                return collectElements(qualify(model.parent), depth + 1, visiting);
            }
            return null;
        } finally {
            visiting.remove(qualifiedPath);
        }
    }

    /**
     * Loads and parses one model from the classpath: {@code /assets/<ns>/models/<path>.json}.
     */
    private static ModelData loadModel(String qualifiedPath) {
        int colon = qualifiedPath.indexOf(':');
        String namespace = qualifiedPath.substring(0, colon);
        String path = qualifiedPath.substring(colon + 1);
        String resource = "/assets/" + namespace + "/models/" + path + ".json";

        InputStream in = ModelBounds.class.getResourceAsStream(resource);
        if (in == null) {
            CatFrameCompact.logger.warn("[ModelBounds] Model resource not found: {}", resource);
            return null;
        }
        try {
            return parse(new InputStreamReader(in, StandardCharsets.UTF_8));
        } finally {
            try {
                in.close();
            } catch (IOException ignored) {
                // Closing a classpath stream rarely fails; nothing meaningful to do.
            }
        }
    }

    /**
     * Parses model JSON; on malformed content, logs a warning and returns {@code null}.
     */
    private static ModelData parse(Reader reader) {
        try {
            return GSON.fromJson(reader, ModelData.class);
        } catch (JsonParseException e) {
            CatFrameCompact.logger.warn("[ModelBounds] Malformed model JSON: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Prefixes an unqualified parent reference with the default namespace.
     */
    private static String qualify(String modelPath) {
        return modelPath.indexOf(':') >= 0 ? modelPath : DEFAULT_NAMESPACE + ":" + modelPath;
    }

    /**
     * Computes the per-element boxes of the given elements (each clamped to the
     * block volume), or {@code null} if none is valid.
     */
    private static float[][] boxesOf(List<Element> elements) {
        if (elements == null || elements.isEmpty()) {
            return null;
        }
        float[][] boxes = new float[elements.size()][];
        int count = 0;

        for (Element e : elements) {
            if (e == null || e.from == null || e.to == null || e.from.length < 3 || e.to.length < 3) {
                continue;
            }
            // 16 pixels per block unit, clamped to the block volume.
            boxes[count++] = new float[]{
                    clamp01(e.from[0] / 16f), clamp01(e.from[1] / 16f), clamp01(e.from[2] / 16f),
                    clamp01(e.to[0] / 16f), clamp01(e.to[1] / 16f), clamp01(e.to[2] / 16f)
            };
        }
        if (count == 0) {
            return null;
        }
        if (count < boxes.length) {
            float[][] trimmed = new float[count][];
            System.arraycopy(boxes, 0, trimmed, 0, count);
            boxes = trimmed;
        }
        return boxes;
    }

    /**
     * Computes the union bounds of per-element boxes, or {@code null} if none.
     */
    private static float[] unionOf(float[][] boxes) {
        if (boxes == null || boxes.length == 0) {
            return null;
        }
        float minX = 1f, minY = 1f, minZ = 1f;
        float maxX = 0f, maxY = 0f, maxZ = 0f;
        for (float[] b : boxes) {
            minX = Math.min(minX, b[0]);
            minY = Math.min(minY, b[1]);
            minZ = Math.min(minZ, b[2]);
            maxX = Math.max(maxX, b[3]);
            maxY = Math.max(maxY, b[4]);
            maxZ = Math.max(maxZ, b[5]);
        }
        return new float[]{minX, minY, minZ, maxX, maxY, maxZ};
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }
}
