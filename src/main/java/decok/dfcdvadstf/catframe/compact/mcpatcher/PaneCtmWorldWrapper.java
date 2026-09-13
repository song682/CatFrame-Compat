package decok.dfcdvadstf.catframe.compact.mcpatcher;

import decok.dfcdvadstf.catframe.core.Direction;
import net.minecraft.block.Block;
import net.minecraft.block.BlockPane;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * IBlockAccess wrapper that fixes pane CTM corner connections by intercepting
 * diagonal neighbor lookups from the CTM engine.
 * <p>
 * <b>Problem</b>: MCPatcher-family CTM engines check all 8 neighbors uniformly.
 * For diagonal positions, they only verify that the diagonal block is the same type
 * as the center pane. They do not account for the pane's thin-plate geometry —
 * a pane at a diagonal position may not actually extend its thin geometry into
 * that corner if one of the two adjacent horizontal neighbors is absent.
 * This causes incorrect corner variants (e.g., ANCP instead of MNOP).
 * <p>
 * <b>Fix</b>: When the CTM engine queries a diagonal position on the face plane,
 * this wrapper checks whether both adjacent horizontal straight neighbors are
 * pane-connectable (via {@link BlockPane#canPaneConnectToBlock}). If either is
 * not, the wrapper returns air for that diagonal, causing the CTM engine to
 * naturally skip the corner connection.
 * <p>
 * Straight neighbor queries are passed through unchanged — the CTM engine's
 * straight neighbor results are correct for panes.
 * <p>
 * Verified viable: all MCPatcher-family engines (mcpatcherforge, Angelica,
 * OptiFuture, RPMCP) reach diagonal neighbor blocks exclusively through
 * {@code IBlockAccess.getBlock(x,y,z)}.
 */
public class PaneCtmWorldWrapper implements IBlockAccess {

    private final IBlockAccess delegate;
    private final int cx, cy, cz;
    private final BlockPane pane;
    private final Direction face;

    public PaneCtmWorldWrapper(IBlockAccess delegate, int cx, int cy, int cz,
                               BlockPane pane, Direction face) {
        this.delegate = delegate;
        this.cx = cx;
        this.cy = cy;
        this.cz = cz;
        this.pane = pane;
        this.face = face;
    }

    @Override
    public Block getBlock(int x, int y, int z) {
        Block actual = delegate.getBlock(x, y, z);
        if (actual instanceof BlockPane && isDiagonal(x, y, z)) {
            Direction[] pair = getDirectionPair();
            if (!canConnectHorizontally(pair[0]) || !canConnectHorizontally(pair[1])) {
                return Blocks.air;
            }
        }
        return actual;
    }

    @Override
    public int getBlockMetadata(int x, int y, int z) {
        return delegate.getBlockMetadata(x, y, z);
    }

    @Override
    public int isBlockProvidingPowerTo(int x, int y, int z, int direction) {
        return delegate.isBlockProvidingPowerTo(x, y, z, direction);
    }

    @Override
    public boolean isAirBlock(int x, int y, int z) {
        return getBlock(x, y, z).isAir(this, x, y, z);
    }

    @Override
    public TileEntity getTileEntity(int x, int y, int z) {
        return delegate.getTileEntity(x, y, z);
    }

    @Override
    public int getHeight() {
        return delegate.getHeight();
    }

    @Override
    public int getLightBrightnessForSkyBlocks(int x, int y, int z, int lightValue) {
        return delegate.getLightBrightnessForSkyBlocks(x, y, z, lightValue);
    }

    @Override
    public boolean isSideSolid(int x, int y, int z, ForgeDirection side, boolean _default) {
        return delegate.isSideSolid(x, y, z, side, _default);
    }

    @Override
    public boolean extendedLevelsInChunkCache() {
        return delegate.extendedLevelsInChunkCache();
    }

    @Override
    public BiomeGenBase getBiomeGenForCoords(int x, int z) {
        return delegate.getBiomeGenForCoords(x, z);
    }

    // ── internal helpers ──────────────────────────────────────────────

    /**
     * A position is diagonal on the face plane when exactly two of the three
     * coordinate offsets from center are non-zero. This works uniformly for
     * all six face orientations.
     */
    private boolean isDiagonal(int x, int y, int z) {
        int dx = x - cx;
        int dy = y - cy;
        int dz = z - cz;
        int nonZero = (dx != 0 ? 1 : 0) + (dy != 0 ? 1 : 0) + (dz != 0 ? 1 : 0);
        return nonZero == 2;
    }

    /**
     * Returns the two horizontal cardinal directions that define the face plane.
     * For NORTH/SOUTH faces the plane spans EAST–WEST and NORTH–SOUTH;
     * for WEST/EAST faces it spans NORTH–SOUTH and EAST–WEST.
     */
    private Direction[] getDirectionPair() {
        switch (face) {
            case NORTH:
            case SOUTH:
                return new Direction[]{Direction.EAST, Direction.SOUTH};
            case WEST:
            case EAST:
                return new Direction[]{Direction.NORTH, Direction.EAST};
            default:
                // UP/DOWN should not occur for pane CTM (filtered by caller),
                // but provide a safe fallback.
                return new Direction[]{Direction.EAST, Direction.SOUTH};
        }
    }

    /**
     * Check if the center pane connects in the given horizontal direction
     * using the vanilla pane connectivity rule.
     */
    private boolean canConnectHorizontally(Direction dir) {
        int nx = cx + dir.getStepX();
        int ny = cy + dir.getStepY();
        int nz = cz + dir.getStepZ();
        Block neighbor = delegate.getBlock(nx, ny, nz);
        return pane.canPaneConnectToBlock(neighbor);
    }
}
