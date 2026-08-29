package decok.dfcdvadstf.catframe.compact.mcpatcher.ctm;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import decok.dfcdvadstf.catframe.CatFrameCompact;
import decok.dfcdvadstf.catframe.model.render.ModelRenderRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraftforge.common.MinecraftForge;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Lifecycle hub of the CTM compatibility layer: registers the render extension
 * and the resource reload listener, and rebuilds the {@link CtmRuleSet} on
 * every resource reload.
 * <p>
 * Registration reuses the deferred pattern of
 * {@code ResourcePackModelDetector.register()}: when the resource manager is
 * not ready yet, a one-shot client tick defers the registration.
 * <p>
 * The rule set is swapped as a whole (volatile reference to an immutable
 * snapshot), so render threads may read it without locks. A pack added via
 * F3+T (or resource pack switching) is picked up on the next reload.
 */
@SideOnly(Side.CLIENT)
public final class CtmManager implements IResourceManagerReloadListener {

    private static final CtmManager INSTANCE = new CtmManager();
    private static volatile CtmRuleSet ruleSet = CtmRuleSet.EMPTY;
    private static boolean initialized = false;

    private CtmManager() {
    }

    /** Current rule set snapshot (empty before the first scan). */
    public static CtmRuleSet getRuleSet() {
        return ruleSet;
    }

    /**
     * Register the render extension and the reload listener.
     * Client-side only (called from the main mod under an isClient guard);
     * safe to call once.
     */
    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        if (!CatFrameCompact.config.ctmEnabled) {
            CatFrameCompact.logger.info("CTM compatibility layer disabled by config.");
            return;
        }
        ModelRenderRegistry.register(new CtmRenderExtension());
        // Atlas stitch hook: registers CTM tile placeholders (Pre) and
        // collects the stitched IIcons (Post) on the block atlas.
        MinecraftForge.EVENT_BUS.register(new CtmTextureStitch());
        registerReloadListener();
    }

    @Override
    public void onResourceManagerReload(IResourceManager manager) {
        List<CtmPackScanner.CtmPackFile> packs = CtmPackScanner.scan();
        List<CtmProperties> rules = new ArrayList<>();
        int skipped = 0;
        for (CtmPackScanner.CtmPackFile pack : packs) {
            for (String path : pack.properties) {
                try (InputStream in = pack.open(path)) {
                    CtmProperties cp = CtmProperties.parse(path, in);
                    if (cp.valid) {
                        rules.add(cp);
                        if (CatFrameCompact.config.ctmDebugLog) {
                            CatFrameCompact.logger.debug("CTM: rule '{}' method={} tiles={} matchBlocks={} matchTiles={}",
                                    cp.name, cp.method, cp.tiles.size(), cp.matchBlocks, cp.matchTiles);
                        }
                    } else {
                        skipped++;
                        if (CatFrameCompact.config.ctmDebugLog) {
                            CatFrameCompact.logger.debug("CTM: skipped {}: {}", path, cp.invalidReason);
                        }
                    }
                } catch (Exception e) {
                    skipped++;
                    CatFrameCompact.logger.debug("CTM: failed to read {} ({}): {}", path, pack.packName, e.toString());
                }
            }
        }
        ruleSet = new CtmRuleSet(rules);
        CatFrameCompact.logger.info("CTM: loaded {} rules from {} packs ({} properties files, {} skipped)",
                rules.size(), packs.size(), rules.size() + skipped, skipped);
    }

    /** Register the reload listener, deferring via a one-shot client tick if needed. */
    private static void registerReloadListener() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null && mc.getResourceManager() instanceof IReloadableResourceManager) {
            ((IReloadableResourceManager) mc.getResourceManager()).registerReloadListener(INSTANCE);
            CatFrameCompact.logger.info("CTM: registered with resource manager");
        } else {
            CatFrameCompact.logger.debug("CTM: resource manager not ready, deferring registration");
            MinecraftForge.EVENT_BUS.register(new Object() {
                @SubscribeEvent
                public void onClientTick(TickEvent.ClientTickEvent event) {
                    if (event.phase != TickEvent.Phase.END) {
                        return;
                    }
                    Minecraft mc2 = Minecraft.getMinecraft();
                    if (mc2 != null && mc2.getResourceManager() instanceof IReloadableResourceManager) {
                        ((IReloadableResourceManager) mc2.getResourceManager()).registerReloadListener(INSTANCE);
                        CatFrameCompact.logger.info("CTM: registered with resource manager (deferred)");
                        MinecraftForge.EVENT_BUS.unregister(this);
                    }
                }
            });
        }
    }
}
