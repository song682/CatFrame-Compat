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
 * <p>ModelBounds 从 CatFrame JSON 方块模型中推导方块的边界（碰撞箱/选择框）：
 * 读取模型 JSON 中所有 {@code elements} 的 {@code from}/{@code to}（0~16 像素空间），
 * 取并集换算为 0~1 方块空间并夹紧，再通过 {@link Block#setBlockBounds} 绑定到方块。
 * 提供高版本不存在、但常被期望的能力：从 JSON 方块模型自动推导边界，
 * 弥补 1.7.10 无 VoxelShape 体系的空缺（原版各版本的碰撞形状均为独立的手写形状数据、
 * 从未由渲染模型生成，此为本模组的桥接设施）。
 *
 * <p>设计要点（Design notes）：
 * <ul>
 *   <li>形状数据刻意只从 classpath 资源读取——这是有意取舍而非简化：碰撞判定不应受
 *       客户端资源包影响（原版形状亦为代码/数据常量，资源包仅覆盖渲染），同时保证
 *       专用服务端可用（服务端不加载 JSON 模型，依赖客户端渲染管线将使服务端碰撞失效）。
 *       注：此路径仅排除资源包层面的影响，不宣称能阻止对 jar 本身的解包修改。</li>
 *   <li>{@code parent} 继承链通过 classpath 资源自行解析（child 定义 elements 时
 *       整体覆盖 parent，与原版语义一致），深度上限与循环检测同 core 库约定。</li>
 *   <li>简单版限制：element 上的 {@code rotation} 会被忽略，直接采用未旋转的
 *       {@code from}/{@code to}，因此带旋转部件的模型包围盒可能偏大。</li>
 *   <li>本工具定位为 CatFrame core 之上的<b>扩展层能力</b>（core 负责回移高版本渲染特性，
 *       本模组负责在其上搭建桥接设施）：1.7.10 的碰撞判定完全由
 *       {@link Block#setBlockBounds} 设置的实例字段驱动（见
 *       {@code getCollisionBoundingBoxFromPool}），这正是模型推导结果可落地的锚点。</li>
 *   <li>适用边界：{@link Block#setBlockBounds} 写的是整个 Block 单例共享的实例字段
 *       （minX…maxZ），同一方块的不同 metadata 无法经由一次性 {@link #applyFromModel}
 *       获得不同边界；有状态变化的方块应覆写 {@code setBlockBoundsBasedOnState}
 *       按状态设置边界，而非在注册期调一次。</li>
 *   <li>并集 AABB 对非凸模型会偏大；1.7.10 支持单方块多碰撞盒（如栅栏覆写
 *       {@code addCollisionBoxesToList} 逐部件出盒），此类模型可用
 *       {@link #computeElementBoxes} / {@link #addElementCollisionBoxes} 逐元素出盒。</li>
 *   <li>碰撞箱是游戏平衡的一部分而非渲染的附属品：原版刻意让视觉与逻辑不一致
 *       （仙人掌碰撞框比显示框小以防蹭伤，栅栏碰撞比视觉高），因此
 *       {@link #applyFromModel} 是可选入口，调用方随时可用 {@link Block#setBlockBounds}
 *       手动覆盖推导结果。</li>
 * </ul>
 *
 * <p>使用示例（Usage）：
 * <pre>{@code
 * // reads /assets/catframe/models/block/my_block.json (+ parent chain)
 * ModelBounds.applyFromModel(myBlock, "catframe", "block/my_block");
 *
 * // non-convex models: override addCollisionBoxesToList instead
 * ModelBounds.addElementCollisionBoxes(world, x, y, z, mask, list, entity, "catframe", "block/my_block");
 * }</pre>
 */
public final class ModelBounds {

    /** Maximum parent-chain depth, aligned with the core library convention. 父链深度上限。 */
    private static final int MAX_DEPTH = 16;

    /** Default namespace for unqualified parent references. 无限定命名空间的 parent 默认按 minecraft 解析。 */
    private static final String DEFAULT_NAMESPACE = "minecraft";

    private static final Gson GSON = new Gson();

    /**
     * Per-model element box cache, shared by union and multi-box computation.
     * 模型 element 盒缓存（限定路径 → 逐元素六元组数组）；仅缓存成功结果，失败不缓存以便重试。
     */
    private static final Map<String, float[][]> BOX_CACHE = new ConcurrentHashMap<>();

    private ModelBounds() {
    }

    // ==================== Data model / 数据模型 ====================

    /**
     * Minimal model JSON projection: only the fields bounds computation needs.
     * 模型 JSON 的最小投影：只保留边界计算所需的字段，其余字段由 Gson 忽略。
     */
    private static class ModelData {
        public String parent;
        public List<Element> elements;
    }

    private static class Element {
        public float[] from;
        public float[] to;
    }

    // ==================== Public API / 公开接口 ====================

    /**
     * Computes the union bounds of all elements in the given model JSON.
     * 计算给定模型 JSON 中所有 elements 的并集边界。
     *
     * <p>Values are converted from the 0~16 pixel space to 0~1 block space and
     * clamped to [0, 1]. Element {@code rotation} is ignored (simple version).
     * 像素坐标换算为方块坐标并夹紧到 [0, 1]；旋转部件按未旋转的 from/to 处理（简单版）。
     *
     * @param reader JSON content source; closed by the caller
     * @return bounds as {minX, minY, minZ, maxX, maxY, maxZ}, or {@code null}
     *         if the model defines no valid elements
     *         返回六元组边界；模型无有效 elements 时返回 {@code null}
     */
    public static float[] computeBounds(Reader reader) {
        ModelData model = parse(reader);
        if (model == null) {
            return null;
        }
        List<Element> elements = model.elements;
        // Consistent with the two-arg overload: a model carrying only a parent
        // reference falls back to resolving that chain from the classpath.
        // 与双参重载行为一致：自身无 elements、仅声明 parent 时，从 classpath 解析继承链兜底。
        if ((elements == null || elements.isEmpty()) && model.parent != null && !model.parent.isEmpty()) {
            elements = collectElements(qualify(model.parent), 0, new HashSet<String>());
        }
        return unionOf(boxesOf(elements));
    }

    /**
     * Loads a model by namespace + path, resolves its parent chain from the
     * classpath, and computes the resulting bounds.
     * 按命名空间与路径加载模型，解析 classpath 上的 parent 继承链后计算边界。
     *
     * <p>The resolved path is {@code /assets/<namespace>/models/<modelPath>.json}.
     * A child model defining {@code elements} overrides its parent's elements
     * entirely (vanilla semantics); otherwise elements are inherited from the
     * nearest parent that defines them.
     * 子模型定义 elements 时整体覆盖 parent（原版语义），否则继承最近一个定义 elements 的 parent。
     *
     * @param namespace resource namespace, e.g. "catframe" 资源命名空间
     * @param modelPath model path without extension, e.g. "block/my_block" 模型路径（不含 .json）
     * @return bounds as {minX, minY, minZ, maxX, maxY, maxZ}, or {@code null}
     *         if the model (or its parent chain) yields no valid elements
     */
    public static float[] computeBounds(String namespace, String modelPath) {
        return unionOf(elementBoxes(namespace, modelPath));
    }

    /**
     * Computes the per-element boxes of a model (resolved incl. parent chain),
     * for blocks that need multiple collision boxes instead of one union AABB.
     * 计算模型逐 element 的边界盒（含 parent 继承链解析），供需要多碰撞盒而非单一并集 AABB 的方块使用。
     *
     * <p>Each box is {minX, minY, minZ, maxX, maxY, maxZ} in 0~1 block space.
     * 每个盒为 0~1 方块空间的六元组。
     *
     * @param namespace resource namespace, e.g. "catframe" 资源命名空间
     * @param modelPath model path without extension, e.g. "block/my_block" 模型路径（不含 .json）
     * @return one box per valid element, or {@code null} if none 逐元素盒数组；无有效元素时返回 {@code null}
     */
    public static float[][] computeElementBoxes(String namespace, String modelPath) {
        return elementBoxes(namespace, modelPath);
    }

    /**
     * Adds the model's per-element collision boxes to the given list, mirroring
     * vanilla's {@code Block.addCollisionBoxesToList} contract (only boxes
     * intersecting the mask are added, offset to world coordinates).
     * 将模型的逐元素碰撞盒加入列表，对齐原版 {@code addCollisionBoxesToList} 约定
     * （仅加入与 mask 相交的盒，并平移到世界坐标）。
     *
     * <p>Intended to be called from a block's own {@code addCollisionBoxesToList}
     * override for non-convex models, where a single union AABB would be
     * overly inclusive.
     * 供非凸模型在自己的 {@code addCollisionBoxesToList} 覆写中调用——单一并集 AABB 对非凸形状会偏大。
     *
     * @param world     the world 世界实例
     * @param x, y, z   block position 方块坐标
     * @param mask      the mask AABB to test intersection against 用于相交检测的掩码盒
     * @param out       list to receive intersecting boxes 接收相交盒的列表
     * @param entity    colliding entity, passed through for signature parity with
     *                  vanilla (unused, same as vanilla fences) 碰撞实体，仅为对齐原版签名透传（原版栅栏同样不使用）
     * @param namespace resource namespace 资源命名空间
     * @param modelPath model path without extension 模型路径（不含 .json）
     */
    @SuppressWarnings({"unchecked", "rawtypes"}) // raw List mirrors vanilla addCollisionBoxesToList signature / 裸 List 对齐原版签名
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
     * 计算模型边界并通过 {@link Block#setBlockBounds} 绑定到方块；模型无有效 elements 时不做任何修改。
     *
     * <p>Suited to registration-time, metadata-independent bounds. Since
     * {@link Block#setBlockBounds} writes fields shared by the whole Block
     * singleton, state-varying blocks must override
     * {@code setBlockBoundsBasedOnState} instead of relying on this one-shot call.
     * 适用于注册期、与 metadata 无关的静态边界；因 setBlockBounds 写的是整个 Block
     * 单例共享的字段，有状态变化的方块应覆写 {@code setBlockBoundsBasedOnState}，
     * 而不是依赖这一次性调用。
     *
     * @param block     target block 目标方块
     * @param namespace resource namespace, e.g. "catframe" 资源命名空间
     * @param modelPath model path without extension, e.g. "block/my_block" 模型路径（不含 .json）
     * @return {@code true} if bounds were computed and applied 是否成功计算并应用了边界
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

    // ==================== Internals / 内部实现 ====================

    /**
     * Resolves the element list of a namespaced model (incl. parent chain).
     * 解析命名空间模型的 element 列表（含 parent 继承链）。
     */
    private static List<Element> resolveElements(String namespace, String modelPath) {
        return collectElements(namespace + ":" + modelPath, 0, new HashSet<String>());
    }

    /**
     * Returns cached per-element boxes for the given model.
     * 返回指定模型的逐元素盒（带缓存）。
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
     * Recursively collects elements along the parent chain.
     * 沿 parent 继承链递归收集 elements（子模型定义时整体覆盖）。
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
     * 从 classpath 加载并解析单个模型文件。
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
     * Parses model JSON; returns {@code null} on malformed content.
     * 解析模型 JSON；内容非法时记录日志并返回 {@code null}。
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
     * 为无限定命名空间的 parent 引用补上默认命名空间。
     */
    private static String qualify(String modelPath) {
        return modelPath.indexOf(':') >= 0 ? modelPath : DEFAULT_NAMESPACE + ":" + modelPath;
    }

    /**
     * Computes the per-element boxes of the given elements (each clamped to the
     * block volume), or {@code null} if none is valid.
     * 计算 elements 的逐元素盒（各自夹紧到方块体积内）；无有效元素时返回 {@code null}。
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
            // 像素换算为方块单位，并夹紧到方块体积内。
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
     * 计算逐元素盒的并集边界；无盒时返回 {@code null}。
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
