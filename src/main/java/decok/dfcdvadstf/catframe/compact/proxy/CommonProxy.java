package decok.dfcdvadstf.catframe.compact.proxy;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import decok.dfcdvadstf.catframe.CompatConfig;
import decok.dfcdvadstf.catframe.compact.CompactBase;
import decok.dfcdvadstf.catframe.compact.physic.ItemPhysic;
import decok.dfcdvadstf.catframe.compact.tags.PineTags;
import decok.dfcdvadstf.catframe.compact.notification.NotificationCommand;
import decok.dfcdvadstf.catframe.compact.notification.NotificationDefinitions;
import decok.dfcdvadstf.catframe.compact.notification.network.NotificationNetwork;
import net.minecraft.server.MinecraftServer;

import java.io.File;

import static decok.dfcdvadstf.catframe.CatFrameCompat.logger;

public class CommonProxy  {

    public void preInit(FMLPreInitializationEvent event) {
        // ── Notification network: register the server→client channel ──
        NotificationNetwork.init();

        // Master switch for the ItemPhysic compatibility layer: when disabled,
        // all detection, crash rejection and rotation injection are bypassed.
        // Master switch for the PineappleTags compatibility layer: when disabled,
        // tag-pool synchronization and tag queries are bypassed entirely.
        ItemPhysic.setEnabled(CompatConfig.itemPhysicCompat);

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

    public void init(FMLInitializationEvent event) {
    }

    /**
     * Register the {@code /notification} command and reload notification definitions
     * from {@code catframenotifications.json} on every server start.
     * <p>注册 {@code /notification} 命令，并在每次服务器启动时从
     * {@code catframenotifications.json} 重新加载通知定义。</p>
     */
    public void onServerStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new NotificationCommand());
        NotificationDefinitions.reload(MinecraftServer.getServer());
    }

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
