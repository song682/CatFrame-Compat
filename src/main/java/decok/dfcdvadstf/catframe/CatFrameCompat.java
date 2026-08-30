package decok.dfcdvadstf.catframe;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import decok.dfcdvadstf.catframe.compact.ItemPhysic;
import decok.dfcdvadstf.catframe.compact.proxy.CommonProxy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
        modid = Tags.MODID,
        name = Tags.NAME,
        version = Tags.VERSION,
        useMetadata = true,
        dependencies = "required-after:catframe@[0.6.7,);required-after:jarutils@[0.0.2,);after:ingameime;after:pineapple_tag@[1.5.2,);after:optifutrue@[1.2.4,);after:angelica;after:notfine;after:itemphysic;after:mcpatcher[0.3.0,)")
public class CatFrameCompat {

    public static Logger logger = LogManager.getLogger(Tags.NAME);

    public static CompatConfig config;

    @SidedProxy(
            serverSide = "decok.dfcdvadstf.catframe.compact.proxy.CommonProxy",
            clientSide = "decok.dfcdvadstf.catframe.compact.proxy.ClientProxy",
            modId = Tags.MODID
    )
    public static CommonProxy proxy;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        config = new CompatConfig(event.getSuggestedConfigurationFile());
        logger = event.getModLog();


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

    }
}