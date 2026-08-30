package decok.dfcdvadstf.catframe.compact.proxy;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import decok.dfcdvadstf.catframe.compact.CompactBase;
import decok.dfcdvadstf.catframe.compact.ItemPhysic;
import decok.dfcdvadstf.catframe.compact.tags.PineTags;

import java.io.File;

import static decok.dfcdvadstf.catframe.CatFrameCompat.config;
import static decok.dfcdvadstf.catframe.CatFrameCompat.logger;

public class CommonProxy  {

    public void preInit(FMLPreInitializationEvent event) {
        // Master switch for the ItemPhysic compatibility layer: when disabled,
        // all detection, crash rejection and rotation injection are bypassed.
        // Master switch for the PineappleTags compatibility layer: when disabled,
        // tag-pool synchronization and tag queries are bypassed entirely.
        ItemPhysic.setEnabled(config.itemPhysicCompat);

        // The mods directory sits next to the config directory; this also holds in dev.
        ItemPhysic.scan(new File(event.getModConfigurationDirectory().getParentFile(), "mods"));

    }

    public void init(FMLInitializationEvent event) {
    }

    public void postinit(FMLPostInitializationEvent event) {
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
