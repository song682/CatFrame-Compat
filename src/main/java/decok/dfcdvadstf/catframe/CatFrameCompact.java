package decok.dfcdvadstf.catframe;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import decok.dfcdvadstf.catframe.compact.ItemPhysic;
import decok.dfcdvadstf.catframe.compact.tags.PineTags;
import net.minecraftforge.common.config.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

@Mod(
        modid = Tags.MODID,
        name = Tags.NAME,
        version = Tags.VERSION,
        useMetadata = true,
        dependencies = "required-after:catframe@[0.6.7,);required-after:jarutils@[0.0.2,);after:ingameime;after:pineapple_tag"
)
public class CatFrameCompact {
    public static Logger logger = LogManager.getLogger(Tags.NAME);

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // Master switch for the ItemPhysic compatibility layer: when disabled,
        // all detection, crash rejection and rotation injection are bypassed.
        Configuration config = new Configuration(event.getSuggestedConfigurationFile());
        boolean itemPhysicCompat = config.getBoolean("enableItemPhysicCompat", Configuration.CATEGORY_GENERAL, false, "Enable the ItemPhysic compatibility layer: detection, rejection of the official ASM coremod, and drop-rotation injection for the Mixin rewrite. Set to false to bypass all ItemPhysic handling.");
        // Master switch for the PineappleTags compatibility layer: when disabled,
        // tag-pool synchronization and tag queries are bypassed entirely.
        boolean pineTagsCompat = config.getBoolean("enablePineTagsCompat", Configuration.CATEGORY_GENERAL, true, "Enable the PineappleTags compatibility layer: converts PineappleTags' runtime tag pool into CatFrame tags at post-init so CatFrame tag queries and item detection cover PineappleTags content. Set to false to disable the integration.");
        if (config.hasChanged()) {
            config.save();
        }
        ItemPhysic.setEnabled(itemPhysicCompat);
        PineTags.setEnabled(pineTagsCompat);

        // The mods directory sits next to the config directory; this also holds in dev.
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

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        // Sync after all mods finished init: PineappleTags-based registrations
        // are done by consumer mods, and post-init is the first safe point where
        // the tag pool is (almost) complete. Re-call PineTags.syncTags() manually
        // if a mod registers pineapple tags even later.
        // 放在 postInit 同步：各模组对 PineappleTags 的注册发生在 init 阶段，
        // postInit 是标签池基本完整的最早安全时机；若有模组注册得更晚，
        // 可手动再次调用 PineTags.syncTags() 刷新。
        if (PineTags.isEnabled()) {
            int synced = PineTags.syncTags();
            if (synced > 0) {
                logger.info("PineappleTags compatibility: synced {} tag entries into the CatFrame tag system.", synced);
            }
        }
    }
}