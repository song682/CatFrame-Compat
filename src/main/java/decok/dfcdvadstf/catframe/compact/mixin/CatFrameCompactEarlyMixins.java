package decok.dfcdvadstf.catframe.compact.mixin;

import com.gtnewhorizon.gtnhmixins.IEarlyMixinLoader;
import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Early mixin loader of CatFrameCompact.
 *
 * <p>Early mixins are the ones whose targets are vanilla or Forge classes, which
 * are loaded long before FML finishes loading mods - a late mixin would arrive
 * after the class has been transformed. The only such target here is
 * {@code GuiScreen} (the vanilla tooltip funnel). Working classes such as
 * {@code RenderJsonItemModel} of CatFrame stay in the late channel
 * ({@link CatFrameCompactLateMixins}).</p>
 *
 * <p>Mirrors the layout proven by Chromatic Tooltips: the class is an
 * {@link IFMLLoadingPlugin} (declared as {@code FMLCorePlugin} in the jar manifest)
 * so FML instantiates it before any game class loads, and GTNHMixins picks up the
 * {@link IEarlyMixinLoader} implementation from the coremod list at injection time.
 * The config returned by {@link #getMixinConfig()} must <b>not</b> be listed in the
 * manifest {@code MixinConfigs} - just like the late config, it is queued here and
 * registered by GTNHMixins itself.</p>
 *
 * <p>The {@code refmap} declared by that config is
 * {@code mixins.catframe_compat.refmap.json}: the compiler's {@code -AoutRefMapFile}
 * (i.e. {@code mixins.<modId>.refmap.json} with the Gradle {@code modId}), not the
 * config file name. The early mixin remaps a vanilla target
 * ({@code GuiScreen#renderToolTip}), so it genuinely depends on that file, unlike the
 * late mixins which all target catframe classes with {@code remap = false}.</p>
 *
 * <p>Mixin selection is static: {@link IMixins#getEarlyMixins} runs while coremods
 * are being injected, i.e. before the mod list and before the config file exist,
 * so no entry of {@link Mixins} may gate an early mixin on either. The gating that
 * depends on them (config switch, NEI / Chromatic Tooltips precedence) is applied
 * inside the injected methods instead.</p>
 */
@IFMLLoadingPlugin.MCVersion("1.7.10")
public class CatFrameCompactEarlyMixins implements IFMLLoadingPlugin, IEarlyMixinLoader {

    /** Mixin config queued for the early phase; its package holds {@code mixin/early}. */
    private static final String MIXIN_CONFIG = "mixins.catframe_compat.early.json";

    @Override
    public String[] getASMTransformerClass() {
        return null;
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

    @Override
    public String getMixinConfig() {
        return MIXIN_CONFIG;
    }

    @Override
    public List<String> getMixins(Set<String> loadedCoreMods) {
        return IMixins.getEarlyMixins(Mixins.class, loadedCoreMods);
    }
}
