package decok.dfcdvadstf.catframe.compact.mixin;

import cpw.mods.fml.common.Loader;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Mixin config plugin that conditionally loads mixins based on mod presence.
 * <p>
 * RPMCP (mcpatcher) is a soft dependency, so mixins targeting RPMCP classes
 * must only be loaded when RPMCP is present.
 */
public class CatFrameCompactMixinPlugin implements IMixinConfigPlugin {

    private static final String RPMCP_MODID = "mcpatcher";

    @Override
    public void onLoad(String mixinPackage) {
        // No initialization needed
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        // CTMEngineAccessor targets RPMCP's CTMEngine, only load when RPMCP is present
        if (mixinClassName.contains(".accessor.CTMEngineAccessor")) {
            return Loader.isModLoaded(RPMCP_MODID);
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherUnmatchedTargets) {
        // No action needed
    }

    @Override
    public List<String> getMixins() {
        // Dynamically add mixins that should be loaded
        List<String> mixins = new ArrayList<>();

        // Add CTMEngineAccessor when RPMCP is present
        if (Loader.isModLoaded(RPMCP_MODID)) {
            mixins.add("accessor.CTMEngineAccessor");
        }

        return mixins.isEmpty() ? null : mixins;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // No action needed
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // No action needed
    }
}
