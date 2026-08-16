package decok.dfcdvadstf.catframe;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import decok.dfcdvadstf.catframe.compact.ItemPhysic;
import net.minecraftforge.common.config.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

@Mod(
        modid = Tags.MODID,
        name = Tags.NAME,
        version = Tags.VERSION,
        useMetadata = true,
        dependencies = "required-after:catframe@[0.6.7,);required-after:jarutils@[0.0.2,);after:ingameime"
)
public class CatFrameCompact {
    public static Logger logger = LogManager.getLogger(Tags.NAME);

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // Master switch for the ItemPhysic compatibility layer: when disabled,
        // all detection, crash rejection and rotation injection are bypassed.
        // ItemPhysic 兼容层总开关：关闭后检测、崩溃拒绝与旋转注入全部绕过。
        Configuration config = new Configuration(event.getSuggestedConfigurationFile());
        boolean itemPhysicCompat = config.getBoolean(
                "enableItemPhysicCompat",
                Configuration.CATEGORY_GENERAL,
                true,
                "Enable the ItemPhysic compatibility layer: detection, rejection "
                + "of the official ASM coremod, and drop-rotation injection for the "
                + "Mixin rewrite. Set to false to bypass all ItemPhysic handling.\n"
                + "是否启用 ItemPhysic 兼容层：检测、拒绝官方 ASM coremod，并为 Mixin 版注入"
                + "掉落旋转。设为 false 可完全绕过 ItemPhysic 相关处理。");
        if (config.hasChanged()) {
            config.save();
        }
        ItemPhysic.setEnabled(itemPhysicCompat);

        // The mods directory sits next to the config directory; this also holds in dev.
        // mods 目录位于配置目录的平级位置；该推导在开发环境下同样成立。
        ItemPhysic.scan(new File(event.getModConfigurationDirectory().getParentFile(), "mods"));

        // Reject the official ASM-coremod ItemPhysic: it wholesale-replaces
        // RenderItem.doRender and unconditionally short-circuits ForgeHooksClient,
        // which is mutually exclusive with CatFrame's IItemRenderer takeover.
        // The kotmatross Mixin rewrite yields to third-party renderers and coexists.
        if (ItemPhysic.isOfficialInstalled()) {
            throw new RuntimeException(
                    "CatFrame is incompatible with the official ItemPhysic (ASM coremod): "
                    + "it fully replaces RenderItem.doRender and short-circuits ForgeHooksClient, "
                    + "conflicting with CatFrame's item renderer. "
                    + "Please remove ItemPhysic, or switch to the kotmatross Mixin rewrite "
                    + "(ItemPhysic-Unofficial, modrinth: itemphysic-1.7.10-unofficial).");
        }
        if (ItemPhysic.isMixinInstalled()) {
            logger.info("ItemPhysic (Mixin rewrite) detected - enabling drop animation compatibility.");
        }
    }
}