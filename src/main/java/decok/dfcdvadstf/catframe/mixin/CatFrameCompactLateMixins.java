package decok.dfcdvadstf.catframe.mixin;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;
import cpw.mods.fml.common.FMLCommonHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * CatFrameCompact 的 late mixin 注册器。
 *
 * <p>注入目标 {@code RenderJsonItemModel} 是 CatFrame（前置）的类，
 * 在 FML 阶段才可能被类加载器加载，因此必须走 late mixin 通道
 * （在 mod 加载完成后、目标类首次加载时注入），而不是 early 配置。
 * 该加载器由 UniMixins 通过 {@link LateMixin} 注解自动发现。</p>
 *
 * <p>渲染类仅存在于客户端环境，服务器端不注册任何 mixin。</p>
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
        // RenderJsonItemModel 是客户端渲染类：服务器端注册会导致注入目标缺失
        if (FMLCommonHandler.instance().getSide().isClient()) {
            mixins.add("MixinRenderJsonItemModel");
        }
        return mixins;
    }
}
