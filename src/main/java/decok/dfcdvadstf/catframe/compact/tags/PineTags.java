package decok.dfcdvadstf.catframe.compact.tags;

import club.someoneice.togocup.tags.Tag;
import club.someoneice.togocup.tags.TagsManager;
import decok.dfcdvadstf.catframe.compact.CompactBase;
import decok.dfcdvadstf.catframe.tags.impl.CatFrameTags;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * PineappleTags 兼容层（modid: pineapple_tag）。
 * <p>
 * PineappleTags 与 CatFrame 的 Tags 系统是两套相互独立的运行时标签实现：
 * 前者以 {@code TagsManager} 的内存标签池存储（{@code Tag<E>}，元素可为
 * Item / ItemStack / Block / Class 等），后者以 {@code TagLoader} 按
 * ResourceLocation 组织。本兼容层在启动后期（postInit）把 PineappleTags
 * 标签池中的内容整体转换为 CatFrame 标签，使 CatFrame 的标签查询与物品
 * 自动检测能够覆盖所有经由 PineappleTags 注册的内容，两边生态互通。</p>
 * <p>
 * PineappleTags compatibility layer (modid: pineapple_tag).
 * PineappleTags and CatFrame's tag system are two independent runtime tag
 * implementations. This layer converts the whole PineappleTags tag pool into
 * CatFrame tags at post-init, so that CatFrame tag queries and automatic item
 * detection transparently cover everything registered through PineappleTags.</p>
 *
 * <p>实现要点 / Implementation notes：</p>
 * <ul>
 *   <li>标签池容器 {@code TagsManager.tags} 为私有且无任何公开枚举方法，
 *       这是全类唯一需要反射的位置：先按字段名查找，失败则回退为按
 *       “静态 Map 字段”类型特征扫描；取得池后，每个标签的内容读取均使用
 *       公开方法 {@code Tag#getList()}。
 *       The tag pool container is private with no public enumeration method —
 *       the only reflection point of this class: looked up by field name first,
 *       then by the "static Map field" type signature as fallback. Once the
 *       pool is obtained, tag contents are read via the public
 *       {@code Tag#getList()}.</li>
 *   <li>ItemStack 元素按所属 Item 归并（CatFrame 标签是注册表对象级别，
 *       不携带 meta/NBT 信息）。ItemStack entries are merged by their Item,
 *       since CatFrame tags live at registry-object level (no meta/NBT).</li>
 *   <li>标签名含 ":" 时按其自身拆分命名空间，否则统一归入
 *       {@value #DEFAULT_NAMESPACE}。Names containing ":" are split into
 *       namespace/path; others fall under the default namespace.</li>
 * </ul>
 */
public class PineTags {

    /**
     * 无命名空间的 PineappleTags 标签统一归入的 CatFrame 命名空间。
     * Default CatFrame namespace for pineapple tags without a ":" separator.
     */
    public static final String DEFAULT_NAMESPACE = "pineapple_tags";

    /** PineappleTags 标签池私有字段名（{@code TagsManager.tags}）。 */
    private static final String TAG_POOL_FIELD = "tags";

    /** 标签池字段的反射句柄缓存，避免重复查找。Cached handle to the private tag pool. */
    private static Field tagPoolField;

    /** 最近一次同步转换进 CatFrame 的元素总数（用于日志与诊断）。 */
    private static int lastSyncedCount;

    /**
     * 兼容层总开关（由主类读 config 后调用 {@link #setEnabled} 设置）：
     * 关闭后同步与查询全部失效，等价于完全无视 PineappleTags。
     * 初始值跟随 pineapple_tag 实际安装状态（未装则默认不启用）。
     */
    private static boolean enabled = CompactBase.isWolfTagInstalled();

    private PineTags() {}

    /**
     * 启用或禁用整个 PineappleTags 兼容层（config 开关）。
     * 实际生效需要 config 开关与 pineapple_tag 安装状态同时满足。
     *
     * @param configEnabled {@code true} 启用；{@code false} 完全绕过
     */
    public static void setEnabled(boolean configEnabled) {
        enabled = configEnabled && CompactBase.isWolfTagInstalled();
    }

    /**
     * 兼容层是否处于启用状态（config 允许且已安装 pineapple_tag）。
     */
    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * 把 PineappleTags 标签池中的全部内容转换进 CatFrame 的 Tag 系统。
     * <p>应在 postInit 阶段调用（此时各模组对 PineappleTags 的注册已完成）；
     * 若有模组在更晚的时机注册标签，可再次调用本方法刷新，
     * CatFrame 标签集合为 Set 语义，重复同步幂等。</p>
     * <p>未启用、未安装或反射失败时静默跳过，不影响游戏启动。</p>
     *
     * @return 本次同步转换进 CatFrame 的元素个数
     */
    public static int syncTags() {
        if (!enabled) return 0;

        Map<String, ?> pool = obtainTagPool();
        if (pool == null) return 0;

        int count = 0;
        for (Map.Entry<String, ?> entry : pool.entrySet()) {
            if (!(entry.getValue() instanceof Tag)) continue;

            String[] location = splitTagName(entry.getKey());
            String namespace = location[0];
            String path = location[1];

            for (Object element : ((Tag<?>) entry.getValue()).getList()) {
                if (element instanceof Item) {
                    CatFrameTags.add(namespace, path, (Item) element);
                    count++;
                } else if (element instanceof Block) {
                    CatFrameTags.add(namespace, path, (Block) element);
                    count++;
                } else if (element instanceof ItemStack) {
                    // CatFrame 标签为注册表对象级别：ItemStack 按 Item 归并，
                    // meta/NBT 差异不保留。Merged by Item; meta/NBT is dropped.
                    CatFrameTags.add(namespace, path, ((ItemStack) element).getItem());
                    count++;
                }
                // Class 等非注册表元素无法映射进 CatFrame 标签，直接跳过。
            }
        }

        lastSyncedCount = count;
        return count;
    }

    /**
     * 检查物品是否属于指定的 PineappleTags 标签（经由已同步的 CatFrame 标签查询）。
     *
     * @param item    待查询物品
     * @param tagName PineappleTags 标签名（可含 "namespace:path" 形式）
     */
    public static boolean is(Item item, String tagName) {
        if (!enabled || item == null || tagName == null) return false;
        String[] location = splitTagName(tagName);
        return CatFrameTags.is(item, location[0], location[1]);
    }

    /**
     * 检查方块是否属于指定的 PineappleTags 标签（经由已同步的 CatFrame 标签查询）。
     */
    public static boolean is(Block block, String tagName) {
        if (!enabled || block == null || tagName == null) return false;
        String[] location = splitTagName(tagName);
        return CatFrameTags.is(block, location[0], location[1]);
    }

    /**
     * 取得物件在 PineappleTags 侧的原始标签名列表（直通其公开 API），
     * 可用于悬浮文本等展示场景；未启用或 API 不可用时返回空列表。
     *
     * @param object 任意物件（通常为 Item / ItemStack）
     */
    public static List<String> getPineappleTagNames(Object object) {
        if (!enabled || object == null) return Collections.emptyList();
        try {
            return TagsManager.manager().getTagsFromObjects(object);
        } catch (Throwable ignored) {
            // PineappleTags API 变动：静默降级。API drift: degrade silently.
            return Collections.emptyList();
        }
    }

    /**
     * 最近一次 {@link #syncTags()} 转换进 CatFrame 的元素个数。
     */
    public static int lastSyncedCount() {
        return lastSyncedCount;
    }

    /**
     * 反射读取 PineappleTags 的私有标签池 {@code TagsManager.tags}：
     * 优先按字段名定位；若字段改名，则回退为查找“静态且类型为 Map”的
     * 声明字段（该池是 TagsManager 中唯一的静态 Map 字段）。
     * 失败（未安装 / 结构变动）时返回 {@code null} 静默降级。
     */
    @SuppressWarnings("unchecked")
    private static Map<String, ?> obtainTagPool() {
        try {
            if (tagPoolField == null) {
                try {
                    tagPoolField = TagsManager.class.getDeclaredField(TAG_POOL_FIELD);
                } catch (NoSuchFieldException e) {
                    // 字段改名回退：按“静态 Map 字段”类型特征定位。
                    // Fallback: locate by the "static Map field" signature.
                    for (Field candidate : TagsManager.class.getDeclaredFields()) {
                        if (Modifier.isStatic(candidate.getModifiers())
                                && Map.class.isAssignableFrom(candidate.getType())) {
                            tagPoolField = candidate;
                            break;
                        }
                    }
                }
                if (tagPoolField == null) return null;
                tagPoolField.setAccessible(true);
            }
            Object pool = tagPoolField.get(null);
            return pool instanceof Map ? (Map<String, ?>) pool : null;
        } catch (Throwable ignored) {
            // 未安装 PineappleTags 或其内部结构变动：静默降级。
            return null;
        }
    }

    /**
     * 拆分 PineappleTags 标签名为 [命名空间, 路径]：
     * 含 ":" 时以其为界，否则归入 {@value #DEFAULT_NAMESPACE}。
     */
    private static String[] splitTagName(String tagName) {
        int colon = tagName.indexOf(':');
        if (colon > 0 && colon < tagName.length() - 1) {
            return new String[] { tagName.substring(0, colon), tagName.substring(colon + 1) };
        }
        return new String[] { DEFAULT_NAMESPACE, tagName };
    }
}
