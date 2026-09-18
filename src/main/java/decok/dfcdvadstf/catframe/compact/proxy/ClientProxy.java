package decok.dfcdvadstf.catframe.compact.proxy;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import decok.dfcdvadstf.catframe.CompatConfig;
import decok.dfcdvadstf.catframe.compact.CompactBase;
import decok.dfcdvadstf.catframe.compact.mcpatcher.CtmRenderExtension;
import decok.dfcdvadstf.catframe.compact.mcpatcher.RpmcpRenderExtension;
import decok.dfcdvadstf.catframe.model.render.api.ModelRenderExtensions;
import decok.dfcdvadstf.catframe.resources.builtin.BuiltinPackDescriptor;
import decok.dfcdvadstf.catframe.resources.builtin.BuiltinPackRegistry;
import decok.dfcdvadstf.catframe.ui.extended.components.notifications.NotificationManager;
import decok.dfcdvadstf.catframe.ui.extended.theme.JsonThemeLoader;
import decok.dfcdvadstf.catframe.ui.extended.theme.ThemeManager;
import io.qzz.dfdvdsf.jarfile.ModVersions;

import static decok.dfcdvadstf.catframe.CatFrameCompat.logger;

public class ClientProxy extends CommonProxy {

    public static final String OPTIFUTURE_MIN_VER = "1.2.3";
    public final BuiltinPackDescriptor FIX_GLASS_PANE = new BuiltinPackDescriptor("glass_pane_fix", "pack.glass_pane_fix.title", "pack.glass_pane_fix.description");

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);

        // ── Theme system: load JSON themes and activate the configured one ──
        JsonThemeLoader.loadThemes();
        ThemeManager.getInstance().setActive(CompatConfig.activeTheme);

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
                BuiltinPackRegistry.register(FIX_GLASS_PANE);
            }

            // RPMCP (Right Proper MCPatcher) CTM bridge: separate extension class
            // (com.falsepattern classes) registered only when the mcpatcher mod is
            // present, so the class is never loaded without it.
            if (CompactBase.isRightProperMCPatcherInstalled()) {
                ModelRenderExtensions.register(RpmcpRenderExtension.INSTANCE);
                logger.info("RPMCP CTM bridge enabled (Right Proper MCPatcher detected).");
                BuiltinPackRegistry.register(FIX_GLASS_PANE);
            }


        }

        // ── Notification system: register the manager as a HUD+SCREEN overlay ──
        // The CatFrame core ClientOverlayHandler already drives OverlayManager tick/render
        // via Forge events; we only need to register our notification overlay.
        NotificationManager.INSTANCE.register();
    }

}
