package decok.dfcdvadstf.catframe.compact.mcpatcher.shared;

import com.prupe.mcpatcher.ctm.CTMUtils;
import decok.dfcdvadstf.catframe.compact.CompactBase;
import decok.dfcdvadstf.catframe.model.render.IModelRenderExtension;
import decok.dfcdvadstf.catframe.model.render.api.RenderContext;
import decok.dfcdvadstf.catframe.model.render.api.RenderPhase;
import net.minecraft.block.Block;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

/**
 * Render extension bridging the MCPF-heritage CTM entry point into the
 * CatFrame pipeline: for every world quad it asks the installed CTM mod
 * ({@link #getBlockIcon}) for a replacement icon and writes
 * it into {@link RenderContext#iconOverride}.
 * <p>
 * Stateless: safe for parallel chunk compilation. The CTM mod's own state
 * (override tables, iterators) lives on its side; CatFrame only supplies the
 * call site that the VMM pipeline would otherwise skip.
 */
public class CtmRenderExtension implements IModelRenderExtension {

    public static final CtmRenderExtension INSTANCE = new CtmRenderExtension();

    @Override
    public void apply(RenderContext ctx) {
        if (ctx.phase != RenderPhase.BLOCK_WORLD) {
            return;
        }
        // Direction-less quads (cross etc.) have no face to query.
        if (ctx.world == null || ctx.block == null || ctx.quad.face == null) {
            return;
        }
        IIcon base = ctx.quad.icon;
        if (base == null) {
            return;
        }
        IIcon ctm = getBlockIcon(base, ctx.block, ctx.world,
                ctx.x, ctx.y, ctx.z, ctx.quad.face.ordinal());
        if (ctm != null && ctm != base) {
            ctx.iconOverride = ctm;
        }
    }

    public static IIcon getBlockIcon(IIcon icon, Block block,
                                     IBlockAccess blockAccess, int i, int j, int k, int face) {
        return CTMUtils.getBlockIcon(icon, block, blockAccess, i, j, k, face);
    }
}
