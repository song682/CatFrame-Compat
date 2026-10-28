package decok.dfcdvadstf.catframe;

import decok.dfcdvadstf.catframe.compact.Tags;
import decok.dfcdvadstf.catframe.config.ConfigEntry;
import decok.dfcdvadstf.catframe.config.ConfigManager;
import decok.dfcdvadstf.catframe.config.ConfigType;
import decok.dfcdvadstf.catframe.config.Configure;
import decok.dfcdvadstf.catframe.config.IConfigScope;

/**
 * Compatibility layer options, backed by {@code config/CatFrameCompat.cfg} (the
 * same file name the old Forge {@code Configuration} used, so existing installs
 * keep their file).
 * <p>
 * Registered through {@link ConfigManager} in {@code CatFrameCompat#preInit}; every
 * entry is a {@code static} field carrying exactly one kind annotation, so reading
 * an option is a plain static field access ({@code CompatConfig.ctmEnabled}).
 */
@Configure(modid = Tags.MODID, type = ConfigType.FORGE_CFG, name = "CatFrameCompat")
public final class CompatConfig {

    private CompatConfig() {
    }

    @ConfigEntry.Comment("Enable the ItemPhysic compatibility layer: detection, rejection of the official ASM coremod, and drop-rotation injection for the Mixin rewrite. Set to false to bypass all ItemPhysic handling.")
    @ConfigEntry.Name("enableItemPhysicCompat")
    @ConfigEntry.Boolean(false)
    public static boolean itemPhysicCompat;

    /** Backhand offhand left-hand display transform support / Backhand 副手左手 display 变换支持 */
    @ConfigEntry.Comment("Enable the Backhand compatibility layer: apply the model's left-hand display transforms (firstperson_lefthand / thirdperson_lefthand, or the vanilla-convention defaults) to the offhand item renders instead of the right-hand ones. Requires the Backhand mod. Set to false to bypass all Backhand handling.")
    @ConfigEntry.Name("enableBackhandCompat")
    @ConfigEntry.Boolean(true)
    public static boolean backhandCompat;

    /** MCPatcher-style CTM resource pack support / MCPatcher 式 CTM 资源包支持 */
    @ConfigEntry.Comment("MCPatcher-style CTM resource pack support: scan mcpatcher/ctm and optifine/ctm properties and route connected-texture selection through the CatFrame render pipeline. Set to false to bypass all CTM handling.")
    @ConfigEntry.Name("enableCtm")
    @ConfigEntry.Boolean(true)
    public static boolean ctmEnabled;

    /** Log loaded CTM rules and unmatched textures / 记录 CTM 规则加载与未命中纹理 */
    @ConfigEntry.Comment("Log loaded CTM rules, skipped invalid rules and unmatched textures. Only meaningful when enableCtm is true.")
    @ConfigEntry.Boolean(false)
    public static boolean ctmDebugLog;

    /** MCPatcher-style Natural Textures support (RPMCP / OptiFuture) / MCPatcher 式 Natural Textures 支持（RPMCP / OptiFuture） */
    @ConfigEntry.Comment("MCPatcher-style Natural Textures support: route per-position UV rotation/flip (natural.properties) through the CatFrame uvOverride channel. Requires the mcpatcher (RPMCP) or OptiFuture mod. Set to false to bypass all Natural Textures handling.")
    @ConfigEntry.Name("enableNaturalTextures")
    @ConfigEntry.Boolean(true)
    public static boolean naturalTexturesEnabled;

    /** Active UI theme id / 当前激活的 UI 主题 id */
    @ConfigEntry.Comment("Active UI theme id. Themes are loaded from assets/<namespace>/themes/<id>.json. Set to a registered theme id to change the look of CatFrame-based UIs.")
    @ConfigEntry.Text("catframe:vanilla")
    public static String activeTheme;

    @ConfigEntry.Comment("Active this to let BlockState rotating freely")
    @ConfigEntry.Boolean(false)
    public static boolean blockStateRotationFreely;

    /** Chromatic Tooltips rendering for CatFrame-based UIs / CatFrame UI 的 Chromatic Tooltips 渲染支持 */
    @ConfigEntry.Comment("Route tooltips of CatFrame-based UIs (widget tooltips and hovered-slot item tooltips) through the Chromatic Tooltips renderer when it is installed, replacing CatFrame's built-in tooltip background/text rendering with its themed multi-section rendering. Requires the Chromatic Tooltips mod. Set to false to keep CatFrame's own tooltip rendering.")
    @ConfigEntry.Name("enableChromaticTooltipsCompat")
    @ConfigEntry.Boolean(true)
    public static boolean chromaticTooltipsCompat;

    /** Show item tags in item tooltips / 在物品提示框中显示物品标签 */
    @ConfigEntry.Comment("Append a 'Tag(s):' section listing the item's tags (#namespace:path) to its tooltip, shown only while advanced tooltips are on (F3+H). The names come from CatFrame's own tag system, so they cover data pack item tags and in-code tag registrations alike. Set to false to hide tags in tooltips.")
    @ConfigEntry.Name("enableTagTooltips")
    @ConfigEntry.Boolean(true)
    public static boolean tagTooltips;

    /** NEI container-tooltip coexistence / 与 NEI 容器 tooltip 共存 */
    @ConfigEntry.Comment("Coexist with NotEnoughItems' container tooltip renderer: CatFrame stops drawing its own hovered-slot item tooltip and hands its structured tooltip components (the bundle grid, for example) to NEI as CodeChickenLib line handlers, so NEI's frame keeps its paging, count details and other mods' handlers while CatFrame's image components stay visible - in every GUI container, not only CatFrame screens. When Chromatic Tooltips is installed it takes precedence and NEI's own item tooltip is suppressed on CatFrame screens instead. Requires NotEnoughItems. Set to false to keep CatFrame's own tooltip rendering (which leaves NEI running a second frame).")
    @ConfigEntry.Name("enableNeiTooltipsCompat")
    @ConfigEntry.Boolean(true)
    public static boolean neiTooltipsCompat;

    /** CatFrame tooltip rendering on vanilla-style screens / 原版式界面的 CatFrame tooltip 渲染 */
    @ConfigEntry.Comment("Render tooltips of vanilla-style screens - any screen that keeps the vanilla renderToolTip/drawHoveringText path, e.g. other mods' container GUIs, chat hover text, creative tab names - with CatFrame's themed tooltip, including structured components and TOOLTIP_STYLE. Only takes effect when neither NotEnoughItems nor Chromatic Tooltips is installed: both take the vanilla tooltip path over wholesale, and this layer steps aside then. Set to false to keep the vanilla tooltip.")
    @ConfigEntry.Name("enableVanillaTooltipsCompat")
    @ConfigEntry.Boolean(false)
    public static boolean vanillaTooltipsCompat;

    /**
     * Persist the active theme id to disk and update the in-memory field.
     * <p>将当前激活主题 id 写入磁盘并更新内存字段。</p>
     *
     * @param themeId id of the theme to persist / 要持久化的主题 id
     */
    public static void saveActiveTheme(String themeId) {
        activeTheme = themeId;
        IConfigScope scope = ConfigManager.getScope(CompatConfig.class);
        scope.set(IConfigScope.GENERAL, "activeTheme", themeId);
        scope.save();
    }
}
