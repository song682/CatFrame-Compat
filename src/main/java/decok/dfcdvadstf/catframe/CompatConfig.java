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
    }

    public void save() {
        if (config.hasChanged()) {
            config.save();
        }
    }
}
