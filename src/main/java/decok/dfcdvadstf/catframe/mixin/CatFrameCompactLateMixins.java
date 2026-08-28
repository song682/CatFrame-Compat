package decok.dfcdvadstf.catframe.mixin;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;
import cpw.mods.fml.common.FMLCommonHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Late mixin registrar of CatFrameCompact.
 *
 * <p>The injection target {@code RenderJsonItemModel} is a class of CatFrame
 * (a dependency), which may only be loaded by the classloader during the FML
 * phase, so the late mixin channel must be used (injection after the mod
 * finished loading, before the target class's first load) instead of the early
 * config. The registrar is discovered automatically by UniMixins via the
 * {@link LateMixin} annotation.</p>
 *
 * <p>Render classes exist only in the client environment; no mixin is
 * registered on the server side.</p>
 */
@LateMixin
public class CatFrameCompactLateMixins implements ILateMixinLoader {

    @Override
    public String getMixinConfig() {
        return "mixins.catframe_compact.late.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedMods) {
        List<String> mixins = new ArrayList<>();
        // RenderJsonItemModel is a client-side render class: registering it on
        // a server would leave the injection target missing.
        if (FMLCommonHandler.instance().getSide().isClient()) {
            mixins.add("MixinRenderJsonItemModel");
        }
        return mixins;
    }
}
