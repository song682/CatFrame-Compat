package decok.dfcdvadstf.catframe;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import decok.dfcdvadstf.catframe.compact.CompactBase;
import decok.dfcdvadstf.catframe.compact.ItemPhysic;
import decok.dfcdvadstf.catframe.compact.mcpatcher.shared.CtmRenderExtension;
import decok.dfcdvadstf.catframe.compact.tags.PineTags;
import decok.dfcdvadstf.catframe.model.render.api.ModelRenderExtensions;
import io.qzz.dfdvdsf.jarfile.ModVersions;
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

    public static Config config;
    public static final String OPTIFUTURE_MIN_VER = "1.2.3";

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        config = new Config(event.getSuggestedConfigurationFile());
        logger = event.getModLog();
        // Master switch for the ItemPhysic compatibility layer: when disabled,
        // all detection, crash rejection and rotation injection are bypassed.
        // Master switch for the PineappleTags compatibility layer: when disabled,
        // tag-pool synchronization and tag queries are bypassed entirely.
        ItemPhysic.setEnabled(config.itemPhysicCompat);

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

        // MCPF-heritage CTM bridge: route connected-texture selection to the
        // installed mod's CTMUtils (7-parameter family: OptiFuture/Angelica/NotFine).
        // The legacy 8-parameter MCPatcherForge signature is not supported.
        if (event.getSide().isClient()
                && (CompactBase.isOptiFutureInstalled() || CompactBase.isAngelicaInstalled() || CompactBase.isNotFineInstalled())
                && (CompactBase.isOptiFutureInstalled()
                && ModVersions.versionMatches("OptiFuture","optifutrue", "<=" + OPTIFUTURE_MIN_VER))) {
            logger.info("MCPF-heritage CTM bridge enabled (OptiFuture/Angelica/NotFine detected).");
            ModelRenderExtensions.register(CtmRenderExtension.INSTANCE);
        }
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        // Sync after all mods finished init: PineappleTags-based registrations
        // are done by consumer mods, and post-init is the first safe point where
        // the tag pool is (almost) complete. Re-call PineTags.syncTags() manually
        // if a mod registers pineapple tags even later.
        if (CompactBase.isWolfTagInstalled()) {
            int synced = PineTags.syncTags();
            if (synced > 0) {
                logger.info("PineappleTags compatibility: synced {} tag entries into the CatFrame tag system.", synced);
            }
        }
    }
}