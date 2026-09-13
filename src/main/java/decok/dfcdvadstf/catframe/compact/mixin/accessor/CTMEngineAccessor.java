package decok.dfcdvadstf.catframe.compact.mixin.accessor;

import com.falsepattern.mcpatcher.internal.modules.ctm.CTMInfo;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Mixin accessor for accessing RPMCP's {@code CTMEngine} private static fields.
 * <p>
 * This enables pane-specific CTM index calculation using CatFrame's blockstate
 * connection states, solving the "ANCP vs MNOP" corner connection issue.
 * <p>
 * <b>Class-loading isolation</b>: This interface must only be loaded when RPMCP
 * is present. The accessor methods are used by {@code PaneCTMCalculator} which
 * is only invoked from {@code RpmcpRenderExtension} (already gated by mod detection).
 */
@Mixin(targets = "com.falsepattern.mcpatcher.internal.modules.ctm.CTMEngine", remap = false)
public interface CTMEngineAccessor {

    /**
     * Access CTMEngine.tileProperties: icon-based CTM lookup table.
     * <p>
     * Maps texture icon index → list of CTMInfo entries that match that icon.
     * Used to find the correct tileIcons[] array for a given base icon.
     *
     * @return the tile properties map (icon index → CTMInfo list)
     */
    @Accessor(value = "tileProperties", remap = false)
    static ObjectList<ObjectList<CTMInfo>> getTileProperties() {
        throw new AssertionError("Mixin accessor not initialized");
    }

    /**
     * Access CTMEngine.blockProperties: block-based CTM lookup table.
     * <p>
     * Maps Block → list of CTMInfo entries that match that block.
     * Fallback lookup when tileProperties doesn't have a match.
     *
     * @return the block properties map (Block → CTMInfo list)
     */
    @Accessor(value = "blockProperties", remap = false)
    static Object2ObjectMap<Block, ObjectList<CTMInfo>> getBlockProperties() {
        throw new AssertionError("Mixin accessor not initialized");
    }
}
