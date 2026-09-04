package decok.dfcdvadstf.catframe.compact.late;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;
import cpw.mods.fml.common.FMLCommonHandler;
import decok.dfcdvadstf.catframe.compact.physic.ItemPhysic;

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
 *
 * <p>This loader class must NOT live inside a package declared by any mixin
 * config: UniMixins loads it directly during {@code beforeConstructing}, and
 * the mixin transformer refuses direct references into declared mixin
 * packages (IllegalClassLoadError). The actual mixin classes stay in
 * {@code compact.mixin.*}, outside this package.</p>
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
        if (FMLCommonHandler.instance().getSide().isClient()) {
            // Allow arbitrary blockstate rotation angles (not just 0/90/180/270);
            // bypasses CatFrame core's BlockstateKeyValidator.validateRotations()
            // which would otherwise replace non-90° variants with builtin/missing.
            mixins.add("late.MixinBlockstateKeyValidator");

            // RenderJsonItemModel is a client-side render class: only when
            // the Mixin edition of ItemPhysic is installed.
            if (ItemPhysic.isMixinInstalled()) {
                mixins.add("late.MixinRenderJsonItemModel");
            }
        }
        return mixins;
    }
}
