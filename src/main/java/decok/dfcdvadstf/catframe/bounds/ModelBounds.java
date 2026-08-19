package decok.dfcdvadstf.catframe.bounds;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import decok.dfcdvadstf.catframe.CatFrameCompact;
import net.minecraft.block.Block;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ModelBounds — derives block bounds from CatFrame JSON block models.
 *
 * <p>ModelBounds 从 CatFrame JSON 方块模型中推导方块的边界（碰撞箱/选择框）：
 * 读取模型 JSON 中所有 {@code elements} 的 {@code from}/{@code to}（0~16 像素空间），
 * 取并集换算为 0~1 方块空间并夹紧，再通过 {@link Block#setBlockBounds} 绑定到方块。
 * 对齐高版本 Minecraft "碰撞形状由模型自动生成" 的工作方式。
 *
 * <p>设计要点（Design notes）：
 * <ul>
 *   <li>纯 Gson 解析，不依赖 CatFrame core 的客户端渲染管线，因此
 *       <b>逻辑服务端同样可用</b>（模型文件随模组 jar 打包，classpath 两侧可读）。</li>
 *   <li>{@code parent} 继承链通过 classpath 资源自行解析（child 定义 elements 时
 *       整体覆盖 parent，与原版语义一致），深度上限与循环检测同 core 库约定。</li>
 *   <li>简单版限制：element 上的 {@code rotation} 会被忽略，直接采用未旋转的
 *       {@code from}/{@code to}，因此带旋转部件的模型包围盒可能偏大。</li>
 * </ul>
 *
 * <p>使用示例（Usage）：
 * <pre>{@code
 * // reads /assets/catframe/models/block/my_block.json (+ parent chain)
 * ModelBounds.applyFromModel(myBlock, "catframe", "block/my_block");
 * }</pre>
 */
public final class ModelBounds {

    /** Maximum parent-chain depth, aligned with the core library convention. 父链深度上限。 */
    private static final int MAX_DEPTH = 16;

    /** Default namespace for unqualified parent references. 无限定命名空间的 parent 默认按 minecraft 解析。 */
    private static final String DEFAULT_NAMESPACE = "minecraft";

    private static final Gson GSON = new Gson();

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
        return boundsOf(model.elements);
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
        String root = namespace + ":" + modelPath;
        Set<String> visiting = new HashSet<>();
        List<Element> elements = collectElements(root, 0, visiting);
        return boundsOf(elements);
    }

    /**
     * Computes bounds from the given model and binds them to the block via
     * {@link Block#setBlockBounds}. No-op if the model yields no valid elements.
     * 计算模型边界并通过 {@link Block#setBlockBounds} 绑定到方块；模型无有效 elements 时不做任何修改。
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
     * Computes the union bounds of the given elements, or {@code null} if none is valid.
     * 计算 elements 的并集边界；无有效元素时返回 {@code null}。
     */
    private static float[] boundsOf(List<Element> elements) {
        if (elements == null || elements.isEmpty()) {
            return null;
        }
        float minX = 1f, minY = 1f, minZ = 1f;
        float maxX = 0f, maxY = 0f, maxZ = 0f;
        boolean any = false;

        for (Element e : elements) {
            if (e == null || e.from == null || e.to == null || e.from.length < 3 || e.to.length < 3) {
                continue;
            }
            any = true;
            minX = Math.min(minX, e.from[0]);
            minY = Math.min(minY, e.from[1]);
            minZ = Math.min(minZ, e.from[2]);
            maxX = Math.max(maxX, e.to[0]);
            maxY = Math.max(maxY, e.to[1]);
            maxZ = Math.max(maxZ, e.to[2]);
        }
        if (!any) {
            return null;
        }

        // 16 pixels per block unit, clamped to the block volume.
        // 像素换算为方块单位，并夹紧到方块体积内。
        return new float[]{
                clamp01(minX / 16f), clamp01(minY / 16f), clamp01(minZ / 16f),
                clamp01(maxX / 16f), clamp01(maxY / 16f), clamp01(maxZ / 16f)
        };
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }
}
