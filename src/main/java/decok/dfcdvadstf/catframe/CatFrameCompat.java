package decok.dfcdvadstf.catframe;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import decok.dfcdvadstf.catframe.compact.Tags;
import decok.dfcdvadstf.catframe.compact.proxy.CommonProxy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
        modid = decok.dfcdvadstf.catframe.compact.Tags.MODID,
        name = decok.dfcdvadstf.catframe.compact.Tags.NAME,
        version = decok.dfcdvadstf.catframe.compact.Tags.VERSION,
        useMetadata = true,
        dependencies = "required-after:catframe@[0.9.4,);required-after:jarutils@[0.0.2,);after:ingameime;after:pineapple_tag@[1.5.2,);after:optifuture@[1.2.4,);after:angelica;after:notfine;after:itemphysic;after:mcpatcher@[0.3.0,)",
        customProperties = {
                @Mod.CustomProperty(k = "license", v = "MIT"),
                @Mod.CustomProperty(k = "issueTrackerUrl", v = "https://github.com/song682/CatFrame-Compat/issues"),
                @Mod.CustomProperty(k = "iconFile", v = "assets/catframe/logo.png"),
                @Mod.CustomProperty(k = "backgroundFile", v = "assets/catalogue/background.png")
        }
)
public class CatFrameCompat {

    public static Logger logger = LogManager.getLogger(decok.dfcdvadstf.catframe.compact.Tags.NAME);

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

        proxy.preInit(event);
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @EventHandler
    public void onServerStarting(FMLServerStartingEvent event) {
        proxy.onServerStarting(event);
    }
}