package decok.dfcdvadstf.catframe.compact;

import decok.dfcdvadstf.catframe.mixin.late.MixinRenderJsonItemModel;
import io.qzz.dfdvdsf.jarfile.JarContents;
import io.qzz.dfdvdsf.jarfile.JarNames;
import io.qzz.dfdvdsf.jarfile.JarVersionGuesser;
import io.qzz.dfdvdsf.jarfile.ModVersions;
import net.minecraft.entity.item.EntityItem;

import java.io.File;
import java.lang.reflect.Method;

/**
 * ItemPhysic 兼容工具类。
 * <p>
 * ItemPhysic 有两个互不兼容的发行版，modid 都是 {@code itemphysic}，
 * 无法用 modid 区分，必须按 jar 内容判定：
 * <ul>
 *   <li><b>官方版</b>（CreativeMD，ASM coremod）：全量替换 {@code RenderItem.doRender}，
 *       与 CatFrame 的 Forge IItemRenderer 接管机制互斥，CatFrame 检测到即拒绝启动。</li>
 *   <li><b>Mixin 版</b>（kotmatross，依赖 UniMixins）：细粒度注入原版方法并主动给
 *       第三方 IItemRenderer 让路，可共存；其掉落物空翻动画由
 *       {@code ClientPhysic.applyRotations} 驱动，本类通过反射调用（编译期零依赖）。</li>
 * </ul>
 *
 * <p>版本判定复用 jar-utils 的 jar 内容扫描能力
 * （{@link JarContents#findClassEntries}）：纯文件系统操作，不依赖类加载器状态；
 * 官方版独有类 {@code ItemPatchingLoader} 与 Mixin 版独有类 {@code ClientPhysic}
 * 作为区分锚点。兼容策略：官方版直接崩溃提示移除；Mixin 版放行，并在 CatFrame 渲染器
 * （{@link MixinRenderJsonItemModel}）中复刻其旋转物理。</p>
 */
public class ItemPhysic {

    /** 官方版（CreativeMD ASM coremod）独有类：doRender 整体替换的补丁入口。 */
    private static final String OFFICIAL_CORE_LOADER = "com.creativemd.itemphysic.ItemPatchingLoader";

    /** Mixin 版独有类：旋转/流体/蛛网减速逻辑所在（1.3.1 kotmatross edition）。 */
    private static final String MIXIN_CLIENT_PHYSIC = "com.creativemd.itemphysic.physics.ClientPhysic";

    /**
     * Mixin 版最低版本号：官方版从未发布过 ≥ 此版本的发行（其版本线止步于
     * 更低的版本号），因此 Forge 层面读到 ≥ 1.2.6 即可直接确认 Mixin 版，
     * 无需再扫描 jar 内容。
     */
    private static final String MIXIN_MIN_VERSION = "1.2.6";

    /** 缓存的反射句柄，避免每帧反射查找。 */
    private static Method applyRotationsMethod;

    // === === === 扫描结果（scan 一次，缓存全部判定） === === ===

    private static boolean scanned;
    private static boolean official;
    private static boolean mixin;
    /** 命中的 itemphysic jar 文件名（不含扩展名，用于日志与版本提示）。 */
    private static String detectedJarName;

    /**
     * 兼容层总开关（由主类读 config 后调用 {@link #setEnabled} 设置）：
     * 关闭后检测与注入全部失效，等价于完全无视 ItemPhysic。
     */
    private static boolean enabled = true;

    private ItemPhysic() {}

    /**
     * 启用或禁用整个 ItemPhysic 兼容层（config 开关）。
     * 关闭后 {@link #isInstalled()} / {@link #isOfficialInstalled()} /
     * {@link #isMixinInstalled()} 一律返回 {@code false}，
     * 崩溃拒绝与旋转注入均不再生效。
     *
     * @param enabled {@code true} 启用（默认）；{@code false} 完全绕过
     */
    public static void setEnabled(boolean enabled) {
        ItemPhysic.enabled = enabled;
    }

    /**
     * 扫描 mods 目录中的 jar，按内容判定 itemphysic 发行版。
     * <p>判定分两层：</p>
     * <ul>
     *   <li><b>第一层（Forge 层面）</b>：经 {@link ModVersions#versionMatches} 读取
     *       已加载 mod 的版本号，≥ {@value #MIXIN_MIN_VERSION} 即确认 Mixin 版——
     *       官方版从未发布过这么高的版本，无需扫描 jar。</li>
     *   <li><b>第二层（jar 内容）</b>：版本低于 {@value #MIXIN_MIN_VERSION} 或读不到
     *       版本时，回退到类条目扫描：官方版含 {@code ItemPatchingLoader}，
     *       Mixin 版含 {@code physics/ClientPhysic}。</li>
     * </ul>
     * 仅扫描一次，重复调用无副作用；目录缺失或不可读时静默跳过（视为未安装）。
     *
     * @param modsDir mods 目录（可由 preInit 事件的配置目录推导）
     */
    public static void scan(File modsDir) {
        if (scanned) return;
        scanned = true;
        if (!enabled) return;

        // Tier 1: Forge-level version check — a version this high can only be
        // the Mixin rewrite, since the official line never reached it.
        // 第一层：Forge 层面版本判定——如此高的版本号只可能是 Mixin 版，
        // 因为官方版从未更到这么高。
        if (ModVersions.versionMatches(null, "itemphysic", ">=" + MIXIN_MIN_VERSION)) {
            mixin = true;
            return;
        }

        // Tier 2: jar content scan, only when the version is unknown or below the Mixin floor.
        // 第二层：jar 内容扫描，仅当版本未知或低于 Mixin 版下限时执行。
        if (modsDir == null || !modsDir.isDirectory()) return;

        File[] files = modsDir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (!JarNames.isJarFile(file)) continue;
            boolean hasOfficial = !JarContents.findClassEntries(file, OFFICIAL_CORE_LOADER).isEmpty();
            boolean hasMixin = !JarContents.findClassEntries(file, MIXIN_CLIENT_PHYSIC).isEmpty();
            if (hasOfficial || hasMixin) {
                official |= hasOfficial;
                mixin |= hasMixin;
                if (detectedJarName == null) {
                    // 记录首个命中的 jar（文件名版本猜测仅用于日志展示）
                    JarVersionGuesser.Guess guess = JarVersionGuesser.guess(file);
                    detectedJarName = guess.name() + (guess.hasVersion() ? "-" + guess.version() : "");
                }
            }
        }
    }

    /**
     * 是否检测到任何版本的 ItemPhysic（基于 {@link #scan(File)} 的 jar 内容判定）。
     */
    public static boolean isInstalled() {
        return enabled && (official || mixin);
    }

    /**
     * 是否官方版（ASM coremod）。
     * <p>官方版与 CatFrame 的掉落物接管机制互斥（doRender 整体替换，
     * 无条件短路 ForgeHooksClient），需要拒绝启动。</p>
     */
    public static boolean isOfficialInstalled() {
        return enabled && official;
    }

    /**
     * 是否 Mixin 版（kotmatross）。
     * <p>该版本细粒度注入原版渲染并主动给第三方 IItemRenderer 让路，
     * 与 CatFrame 结构上兼容。</p>
     */
    public static boolean isMixinInstalled() {
        return enabled && mixin;
    }

    /**
     * 命中的 itemphysic jar 名（文件名版本猜测，仅用于日志）。
     */
    public static String detectedJarName() {
        return detectedJarName;
    }

    /**
     * 反射调用 Mixin 版的 {@code ClientPhysic.applyRotations(EntityItem)}，
     * 更新掉落物的 {@code rotationPitch}（下落翻滚 / 落地归零 / 流体与蛛网减速）。
     * <p>编译期零依赖 ItemPhysic：未安装 Mixin 版、类不存在或方法签名变化时
     * 静默降级（抛掉异常），不影响 CatFrame 自身渲染。</p>
     *
     * @param item 正在渲染的掉落物实体
     */
    public static void applyRotations(EntityItem item) {
        try {
            if (applyRotationsMethod == null) {
                Class<?> clazz = Class.forName(MIXIN_CLIENT_PHYSIC);
                applyRotationsMethod = clazz.getMethod("applyRotations", EntityItem.class);
            }
            applyRotationsMethod.invoke(null, item);
        } catch (Throwable ignored) {
            // 未安装 Mixin 版或 API 变动：静默降级
        }
    }
}
