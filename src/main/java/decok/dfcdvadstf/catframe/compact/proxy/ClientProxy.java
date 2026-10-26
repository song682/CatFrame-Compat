package decok.dfcdvadstf.catframe.compact.proxy;

import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import decok.dfcdvadstf.catframe.CompatConfig;
import decok.dfcdvadstf.catframe.compact.CompactBase;
import decok.dfcdvadstf.catframe.features.TagTooltips;
import decok.dfcdvadstf.catframe.compact.mcpatcher.ctm.CTMRenderExtension;
import decok.dfcdvadstf.catframe.compact.mcpatcher.ctm.RPCTMRenderExtension;
import decok.dfcdvadstf.catframe.compact.mcpatcher.natural.NaturalExtension;
import decok.dfcdvadstf.catframe.compact.mcpatcher.natural.RPNaturalExtension;
import decok.dfcdvadstf.catframe.compact.tooltips.nei.NeiTooltipBridge;
import decok.dfcdvadstf.catframe.compact.offhand.BackhandDisplayExtension;
import decok.dfcdvadstf.catframe.model.extended.json.transformation.BlockstateTransformExtension;
import decok.dfcdvadstf.catframe.model.render.api.ModelRenderExtensions;
import decok.dfcdvadstf.catframe.resources.builtin.BuiltinPackDescriptor;
import decok.dfcdvadstf.catframe.resources.builtin.BuiltinPackRegistry;
import decok.dfcdvadstf.catframe.ui.extended.components.notifications.NotificationOverlay;
import decok.dfcdvadstf.catframe.ui.extended.theme.JsonThemeLoader;
import decok.dfcdvadstf.catframe.ui.extended.theme.ThemeCommand;
import decok.dfcdvadstf.catframe.ui.extended.theme.ThemeKeyHandler;
import decok.dfcdvadstf.catframe.ui.extended.theme.ThemeManager;
import io.qzz.dfdvdsf.jarfile.ModVersions;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;

import static decok.dfcdvadstf.catframe.CatFrameCompat.logger;

public class ClientProxy extends CommonProxy {

    public CompatConfig config;
    public static final String OPTIFUTURE_MIN_VER = "1.2.3";
    public final BuiltinPackDescriptor FIX_GLASS_PANE = new BuiltinPackDescriptor("glass_pane_fix", "pack.glass_pane_fix.title", "pack.glass_pane_fix.description");

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);

        // ── Theme system: load JSON themes and activate the configured one ──
        JsonThemeLoader.loadThemes();
        ThemeManager.getInstance().setActive(CompatConfig.activeTheme);

        // ── Theme selection entry points: hotkey + client command ──
        ClientCommandHandler.instance.registerCommand(new ThemeCommand());
        ThemeKeyHandler.register();

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
                ModelRenderExtensions.register(CTMRenderExtension.INSTANCE);
                BuiltinPackRegistry.register(FIX_GLASS_PANE);
            }

            // RPMCP (Right Proper MCPatcher) CTM bridge: separate extension class
            // (com.falsepattern classes) registered only when the mcpatcher mod is
            // present, so the class is never loaded without it.
            if (CompactBase.isRightProperMCPatcherInstalled()) {
                ModelRenderExtensions.register(RPCTMRenderExtension.INSTANCE);
                logger.info("RPMCP CTM bridge enabled (Right Proper MCPatcher detected).");
                BuiltinPackRegistry.register(FIX_GLASS_PANE);
            }
        }

        // ── Backhand left-hand display bridge ──
        // Backhand replays the whole first/third-person hand pass under a mirrored
        // GL state, under which the vanilla/Forge anchor chain auto-conjugates into
        // the left-hand anchor; this bridge then replaces the builtin
        // DisplayTransformExtension's right-hand matrix with the model's authored
        // left-hand entry (firstperson_lefthand / thirdperson_lefthand, or the
        // vanilla-convention default) for the offhand passes only - everything else
        // (animations, arm anchor, preTransform cancellation) stays Backhand's and
        // CatFrame's. Registered at the mod default priority 0, i.e. after the
        // builtin chain head, so it overwrites the right-hand matrix (last writer
        // wins). Class-loading isolation: registered only when Backhand is present,
        // so the linked BackhandUtils API is never touched without it.
        if (CompatConfig.backhandCompat && CompactBase.isBackhandInstalled()) {
            ModelRenderExtensions.register(BackhandDisplayExtension.INSTANCE);
            logger.info("Backhand left-hand display bridge enabled (Backhand detected).");
        }

        // RPMCP (Right Proper MCPatcher) Natural Textures bridge: writes per-position
        // UV rotation/flip into the native RenderContext.uvOverride channel (CatFrame
        // >= 0.9.4). Same class-loading isolation and detection gate as the CTM bridge;
        // registered after it so the chain order is Leaves -> CTM -> Natural
        // (last writer wins, Natural reads whatever iconOverride is already set).
        if (CompatConfig.naturalTexturesEnabled && CompactBase.isRightProperMCPatcherInstalled()) {
            ModelRenderExtensions.register(RPNaturalExtension.INSTANCE);
            logger.info("RPMCP Natural Textures bridge enabled (Right Proper MCPatcher detected).");
        }

        // OptiFuture Natural Textures bridge: the MCPatcher-heritage sibling of the
        // RPMCP bridge above, linking the com.prupe.mcpatcher family instead. Same
        // class-loading isolation; the capability probe (class presence) keeps the
        // link safe on OptiFuture builds that predate the natural module. Note both
        // natural bridges write uvOverride, so installing RPMCP and OptiFuture
        // together stacks their transforms - pick one MCPatcher-family mod.
        if (CompatConfig.naturalTexturesEnabled && CompactBase.isOptiFutureNaturalAvailable()) {
            ModelRenderExtensions.register(NaturalExtension.INSTANCE);
            logger.info("OptiFuture Natural Textures bridge enabled (OptiFuture detected).");
        }

        // ── Forge model-level transform bridge (arbitrary-angle rotation) ──
        // Reads the Forge `transform` field (translation / rotation / scale around an
        // optional pivot) from the raw JSON CatFrame keeps for compatibility layers -
        // the variant's rawJson first, the model's unrecognized top-level keys second -
        // and installs the resulting block-space matrix through the native
        // RenderContext.displayTransform channel. Registered unconditionally: the
        // extension stays inert unless a model actually declares the field.
        ModelRenderExtensions.register(BlockstateTransformExtension.INSTANCE);

        // ── Chromatic Tooltips rendering bridge ──
        // The actual redirection lives in the late mixins (MixinGuiGraphicsExtractor /
        // MixinAbstractContainerScreen) gated by the same detection + config; this only
        // reports activation so the state is visible in the log like the other bridges.
        if (CompatConfig.chromaticTooltipsCompat && CompactBase.isChromaticTooltipsInstalled()) {
            logger.info("Chromatic Tooltips rendering bridge enabled (Chromatic Tooltips detected).");
        }

        // ── Notification system: register the overlay that wraps the manager ──
        // The CatFrame core ClientOverlayHandler already drives OverlayManager tick/render
        // via Forge events; we only need to register our notification overlay.
        NotificationOverlay.INSTANCE.register();

        // ── Item tag tooltips: append the item's CatFrame item tags to its tooltip ──
        // ItemTooltipEvent is client-only; the handler checks the config itself.
        MinecraftForge.EVENT_BUS.register(new TagTooltips());
    }

    /**
     * Late wiring for handlers that must run after every mod finished its own
     * initialization.
     * <p>
     * The NEI bridge is registered here rather than in {@link #preInit} because
     * {@code GuiContainerManager} adds its own default handlers in its static
     * initializer: touching it earlier would freeze NEI's setup mid-boot, and being
     * registered last is also what puts CatFrame's component row below the item name.
     * The reference sits behind the detection gate so the NEI-linked class is never
     * loaded when NotEnoughItems is absent.
     * </p>
     */
    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);

        // ── NEI container-tooltip coexistence ──
        // NEI replaces the renderToolTip call site, so CatFrame's deferred tooltip
        // would otherwise be painted as a second frame. The bridge hands CatFrame's
        // structured components to NEI as line handlers and flags the frame as
        // NEI-owned for this mod's late mixin.
        if (CompactBase.isNEIInstalled() && CompatConfig.neiTooltipsCompat) {
            NeiTooltipBridge.register();
        }
    }

}
