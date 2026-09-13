package decok.dfcdvadstf.catframe.compact.mcpatcher;

import com.falsepattern.mcpatcher.internal.modules.ctm.CTMInfo;
import com.falsepattern.mcpatcher.internal.modules.ctm.Method;
import decok.dfcdvadstf.catframe.compact.mixin.middle.accessor.CTMEngineAccessor;
import decok.dfcdvadstf.catframe.core.Direction;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.block.Block;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import java.util.Map;

/**
 * Pane-specific CTM index calculator that uses CatFrame's blockstate properties
 * (north/east/south/west connection states) instead of CTMEngine's generic neighbor detection.
 * <p>
 * This solves the "ANCP vs MNOP" corner connection issue: CTMEngine uses generic neighbor
 * detection which doesn't account for pane thin-plate geometry, while CatFrame's
 * VanillaBlockResolvers.PANE computes correct connection states via BlockPane.canPaneConnectToBlock.
 * <p>
 * The 47-tile CTM index calculation logic is replicated from CTMEngine.getConnectedTextureCtm,
 * but uses blockstateProps for borders[] instead of isNeighbour().
 */
public final class PaneCTMCalculator {

    private PaneCTMCalculator() {}

    /**
     * Get the CTM icon for a pane face using CatFrame's blockstate connection states.
     *
     * @param world           the world
     * @param block           the pane block
     * @param x               the x coordinate
     * @param y               the y coordinate
     * @param z               the z coordinate
     * @param face            the quad face direction (NORTH/SOUTH/EAST/WEST)
     * @param baseIcon        the base icon (block.getIcon(0, meta))
     * @param blockstateProps CatFrame's blockstate properties (north/east/south/west)
     * @return the CTM icon, or null if no CTM applies
     */
    public static IIcon getPaneCTMIcon(IBlockAccess world, Block block, int x, int y, int z,
                                       Direction face, IIcon baseIcon,
                                       Map<String, String> blockstateProps) {
        if (blockstateProps == null || face == null) {
            return null;
        }

        // Read pane connection states from blockstateProps
        boolean north = "true".equals(blockstateProps.get("north"));
        boolean east = "true".equals(blockstateProps.get("east"));
        boolean south = "true".equals(blockstateProps.get("south"));
        boolean west = "true".equals(blockstateProps.get("west"));

        // Calculate CTM index based on face and connection states
        int ctmIndex = calculatePaneCTMIndex(face, north, east, south, west, world, block, x, y, z);

        // Get CTMInfo for the block and retrieve tileIcons
        CTMInfo ctmInfo = getCTMInfo(block);
        if (ctmInfo == null) {
            return null;
        }
        IIcon[] tileIcons = ctmInfo.tileIcons;
        if (tileIcons == null || ctmIndex >= tileIcons.length) {
            return null;
        }

        return tileIcons[ctmIndex];
    }

    /**
     * Calculate the 47-tile CTM index for a pane face.
     * <p>
     * For panes, the CTM pattern depends on:
     * - The connection state of the current face (which direction the pane extends)
     * - The connection state of adjacent faces (corner connections)
     * <p>
     * This is a simplified version of CTMEngine.getConnectedTextureCtm that uses
     * blockstate connection states instead of generic neighbor detection.
     */
    private static int calculatePaneCTMIndex(Direction face, boolean north, boolean east,
                                             boolean south, boolean west,
                                             IBlockAccess world, Block block, int x, int y, int z) {
        // For pane faces, we need to determine borders[] based on the pane's connection pattern
        // Pane connections are: north, east, south, west (horizontal only)
        //
        // For each face, the borders[] array represents:
        // borders[0] = "right" neighbor (when looking at the face from outside)
        // borders[1] = "left" neighbor
        // borders[2] = "bottom" neighbor
        // borders[3] = "top" neighbor
        //
        // But for panes, the "neighbors" are actually the pane's own connection states
        // projected onto the face's local coordinate system.

        boolean[] borders = new boolean[4];
        boolean[] edges = new boolean[4];

        // Map pane connections to face-local borders
        // This is the key difference from CTMEngine: we use the pane's actual connection states
        switch (face) {
            case NORTH: // ZNeg face (looking in +Z direction)
                // Right = West, Left = East, Bottom = Down, Top = Up
                borders[0] = west;
                borders[1] = east;
                borders[2] = false; // Down - pane doesn't connect vertically
                borders[3] = false; // Up - pane doesn't connect vertically
                // Edges are diagonal neighbors
                edges[0] = !(west && south);  // bottom-right: SW corner
                edges[1] = !(east && south);  // bottom-left: SE corner
                edges[2] = !(west && north);  // top-right: NW corner
                edges[3] = !(east && north);  // top-left: NE corner
                break;
            case SOUTH: // ZPos face (looking in -Z direction)
                borders[0] = east;
                borders[1] = west;
                borders[2] = false;
                borders[3] = false;
                edges[0] = !(east && north);
                edges[1] = !(west && north);
                edges[2] = !(east && south);
                edges[3] = !(west && south);
                break;
            case WEST: // XNeg face (looking in +X direction)
                borders[0] = south;
                borders[1] = north;
                borders[2] = false;
                borders[3] = false;
                edges[0] = !(south && east);
                edges[1] = !(north && east);
                edges[2] = !(south && west);
                edges[3] = !(north && west);
                break;
            case EAST: // XPos face (looking in -X direction)
                borders[0] = north;
                borders[1] = south;
                borders[2] = false;
                borders[3] = false;
                edges[0] = !(north && west);
                edges[1] = !(south && west);
                edges[2] = !(north && east);
                edges[3] = !(south && east);
                break;
            default:
                return 0; // UP/DOWN should be skipped
        }

        // Calculate base index from borders (same logic as CTMEngine)
        int index = 0;
        if (borders[0] && !borders[1] && !borders[2] && !borders[3]) {
            index = 3;
        } else if (!borders[0] && borders[1] && !borders[2] && !borders[3]) {
            index = 1;
        } else if (!borders[0] && !borders[1] && borders[2] && !borders[3]) {
            index = 12;
        } else if (!borders[0] && !borders[1] && !borders[2] && borders[3]) {
            index = 36;
        } else if (borders[0] && borders[1] && !borders[2] && !borders[3]) {
            index = 2;
        } else if (!borders[0] && !borders[1] && borders[2] && borders[3]) {
            index = 24;
        } else if (borders[0] && !borders[1] && borders[2] && !borders[3]) {
            index = 15;
        } else if (borders[0] && !borders[1] && !borders[2] && borders[3]) {
            index = 39;
        } else if (!borders[0] && borders[1] && borders[2] && !borders[3]) {
            index = 13;
        } else if (!borders[0] && borders[1] && !borders[2] && borders[3]) {
            index = 37;
        } else if (!borders[0] && borders[1] && borders[2] && borders[3]) {
            index = 25;
        } else if (borders[0] && !borders[1] && borders[2] && borders[3]) {
            index = 27;
        } else if (borders[0] && borders[1] && !borders[2] && borders[3]) {
            index = 38;
        } else if (borders[0] && borders[1] && borders[2] && !borders[3]) {
            index = 14;
        } else if (borders[0] && borders[1] && borders[2] && borders[3]) {
            index = 26;
        }

        if (index == 0) {
            return 0;
        }

        // Apply edge refinements (same logic as CTMEngine)
        if (index == 13 && edges[0]) {
            index = 4;
        } else if (index == 15 && edges[1]) {
            index = 5;
        } else if (index == 37 && edges[2]) {
            index = 16;
        } else if (index == 39 && edges[3]) {
            index = 17;
        } else if (index == 14 && edges[0] && edges[1]) {
            index = 7;
        } else if (index == 25 && edges[0] && edges[2]) {
            index = 6;
        } else if (index == 27 && edges[3] && edges[1]) {
            index = 19;
        } else if (index == 38 && edges[3] && edges[2]) {
            index = 18;
        } else if (index == 14 && !edges[0] && edges[1]) {
            index = 31;
        } else if (index == 25 && edges[0] && !edges[2]) {
            index = 30;
        } else if (index == 27 && !edges[3] && edges[1]) {
            index = 41;
        } else if (index == 38 && edges[3] && !edges[2]) {
            index = 40;
        } else if (index == 14 && edges[0] && !edges[1]) {
            index = 29;
        } else if (index == 25 && !edges[0] && edges[2]) {
            index = 28;
        } else if (index == 27 && edges[3] && !edges[1]) {
            index = 43;
        } else if (index == 38 && !edges[3] && edges[2]) {
            index = 42;
        } else if (index == 26 && edges[0] && edges[1] && edges[2] && edges[3]) {
            index = 46;
        } else if (index == 26 && !edges[0] && edges[1] && edges[2] && edges[3]) {
            index = 9;
        } else if (index == 26 && edges[0] && !edges[1] && edges[2] && edges[3]) {
            index = 21;
        } else if (index == 26 && edges[0] && edges[1] && !edges[2] && edges[3]) {
            index = 8;
        } else if (index == 26 && edges[0] && edges[1] && edges[2] && !edges[3]) {
            index = 20;
        } else if (index == 26 && edges[0] && edges[1] && !edges[2] && !edges[3]) {
            index = 11;
        } else if (index == 26 && !edges[0] && !edges[1] && edges[2] && edges[3]) {
            index = 22;
        } else if (index == 26 && !edges[0] && edges[1] && !edges[2] && edges[3]) {
            index = 23;
        } else if (index == 26 && edges[0] && !edges[1] && edges[2] && !edges[3]) {
            index = 10;
        } else if (index == 26 && edges[0] && !edges[1] && !edges[2] && edges[3]) {
            index = 34;
        } else if (index == 26 && !edges[0] && edges[1] && edges[2] && !edges[3]) {
            index = 35;
        } else if (index == 26 && edges[0] && !edges[1] && !edges[2] && !edges[3]) {
            index = 32;
        } else if (index == 26 && !edges[0] && edges[1] && !edges[2] && !edges[3]) {
            index = 33;
        } else if (index == 26 && !edges[0] && !edges[1] && edges[2] && !edges[3]) {
            index = 44;
        } else if (index == 26 && !edges[0] && !edges[1] && !edges[2] && edges[3]) {
            index = 45;
        }

        return index;
    }

    /**
     * Get CTMInfo for the current block via CTMEngineAccessor.
     * <p>
     * Uses blockProperties lookup (block → CTMInfo list), then finds the first
     * CTMInfo with method=CTM (the standard 47-tile connected texture method).
     *
     * @return the CTMInfo, or null if not found
     */
    private static CTMInfo getCTMInfo(Block block) {
        try {
            Object2ObjectMap<Block, ObjectList<CTMInfo>> blockProps = CTMEngineAccessor.getBlockProperties();
            if (blockProps == null) {
                return null;
            }
            ObjectList<CTMInfo> infos = blockProps.get(block);
            if (infos == null || infos.isEmpty()) {
                return null;
            }
            // Find the first CTMInfo with method=CTM (standard connected texture)
            for (CTMInfo info : infos) {
                if (info != null && info.method() == Method.Ctm) {
                    return info;
                }
            }
            // Fallback: try Compact method
            for (CTMInfo info : infos) {
                if (info != null && info.method() == Method.Compact) {
                    return info;
                }
            }
        } catch (Exception e) {
            // Reflection failure or accessor not initialized; silently ignore
        }
        return null;
    }
}
