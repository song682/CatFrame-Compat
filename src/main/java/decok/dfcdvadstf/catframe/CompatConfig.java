package decok.dfcdvadstf.catframe;

import net.minecraftforge.common.config.Configuration;

import java.io.File;

public class CompatConfig {

    public final Configuration config;
    public static boolean itemPhysicCompat;
    /** MCPatcher-style CTM resource pack support / MCPatcher 式 CTM 资源包支持 */
    public static boolean ctmEnabled;
    /** Log loaded CTM rules and unmatched textures / 记录 CTM 规则加载与未命中纹理 */
    public static boolean ctmDebugLog;
    /** MCPatcher-style Natural Textures support (RPMCP / OptiFuture) / MCPatcher 式 Natural Textures 支持（RPMCP / OptiFuture） */
    public static boolean naturalTexturesEnabled;
    /** Active UI theme id / 当前激活的 UI 主题 id */
    public static String activeTheme;
    public static boolean blockStateRotationFreely;

    public CompatConfig(File file) {
        config = new Configuration(file);
        config.load();
        loadOptions();
        save();
    }

    public void loadOptions() {
        itemPhysicCompat = config.getBoolean("enableItemPhysicCompat", Configuration.CATEGORY_GENERAL, false, "Enable the ItemPhysic compatibility layer: detection, rejection of the official ASM coremod, and drop-rotation injection for the Mixin rewrite. Set to false to bypass all ItemPhysic handling.");
        ctmEnabled = config.getBoolean("enableCtm", Configuration.CATEGORY_GENERAL, true, "MCPatcher-style CTM resource pack support: scan mcpatcher/ctm and optifine/ctm properties and route connected-texture selection through the CatFrame render pipeline. Set to false to bypass all CTM handling.");
        ctmDebugLog = config.getBoolean("ctmDebugLog", Configuration.CATEGORY_GENERAL, false, "Log loaded CTM rules, skipped invalid rules and unmatched textures. Only meaningful when enableCtm is true.");
        naturalTexturesEnabled = config.getBoolean("enableNaturalTextures", Configuration.CATEGORY_GENERAL, true, "MCPatcher-style Natural Textures support: route per-position UV rotation/flip (natural.properties) through the CatFrame uvOverride channel. Requires the mcpatcher (RPMCP) or OptiFuture mod. Set to false to bypass all Natural Textures handling.");
        activeTheme = config.getString("activeTheme", Configuration.CATEGORY_GENERAL, "catframe:vanilla", "Active UI theme id. Themes are loaded from assets/<namespace>/themes/<id>.json. Set to a registered theme id to change the look of CatFrame-based UIs.");
        blockStateRotationFreely = config.getBoolean("blockStateRotationFreely", Configuration.CATEGORY_GENERAL, false, "Active this to let BlockState rotating freely");
    }

    public void save() {
        if (config.hasChanged()) {
            config.save();
        }
    }

    /**
     * Persist the active theme id to disk and update the in-memory field.
     * <p>将当前激活主题 id 写入磁盘并更新内存字段。</p>
     *
     * @param themeId id of the theme to persist / 要持久化的主题 id
     */
    public void saveActiveTheme(String themeId) {
        activeTheme = themeId;
        config.get(Configuration.CATEGORY_GENERAL, "activeTheme", "catframe:vanilla").set(themeId);
        save();
    }
}
