package decok.dfcdvadstf.catframe.compact.proxy;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import decok.dfcdvadstf.catframe.CompatConfig;
import decok.dfcdvadstf.catframe.compact.CompactBase;
import decok.dfcdvadstf.catframe.compact.mcpatcher.CtmRenderExtension;
import decok.dfcdvadstf.catframe.compact.mcpatcher.RpmcpRenderExtension;
import decok.dfcdvadstf.catframe.model.render.api.ModelRenderExtensions;
import io.qzz.dfdvdsf.jarfile.ModVersions;

import static decok.dfcdvadstf.catframe.CatFrameCompat.logger;

public class ClientProxy extends CommonProxy {

    public static final String OPTIFUTURE_MIN_VER = "1.2.3";

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);

        if (CompatConfig.ctmEnabled) {
            // MCPF-heritage CTM bridge: route connected-texture selection to the
            // installed mod's CTMUtils (7-parameter family: OptiFuture/Angelica/NotFine).
            // The legacy 8-parameter MCPatcherForge signature is not supported.
            // OptiFuture requires a version whose CTMUtils is 7-parameter (>= MIN_VER);
            // Angelica and NotFine always expose that family.
            if ((CompactBase.isOptiFutureInstalled()
                    && ModVersions.versionMatches("OptiFuture", "optifuture", ">=" + OPTIFUTURE_MIN_VER))
                    || CompactBase.isAngelicaInstalled() || CompactBase.isNotFineInstalled()) {
                logger.info("MCPF-heritage CTM bridge enabled (OptiFuture/Angelica/NotFine detected).");
                ModelRenderExtensions.register(CtmRenderExtension.INSTANCE);
            }

            // RPMCP (Right Proper MCPatcher) CTM bridge: separate extension class
            // (com.falsepattern classes) registered only when the mcpatcher mod is
            // present, so the class is never loaded without it.
            if (CompactBase.isRightProperMCPatcherInstalled()) {
                ModelRenderExtensions.register(RpmcpRenderExtension.INSTANCE);
                logger.info("RPMCP CTM bridge enabled (Right Proper MCPatcher detected).");
            }
        }
    }

}
