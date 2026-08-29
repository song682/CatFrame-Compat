package decok.dfcdvadstf.catframe.compact.mcpatcher.ctm;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import decok.dfcdvadstf.catframe.core.Direction;
import decok.dfcdvadstf.catframe.model.ModelRegistry;
import decok.dfcdvadstf.catframe.model.core.baking.JsonModelBake.BakedQuad;
import decok.dfcdvadstf.catframe.model.state.BlockStateModel;
import decok.dfcdvadstf.catframe.model.state.BlockStateModelPart;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockPane;
import net.minecraft.block.BlockWall;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Selects the CTM tile icon for a world quad (method=ctm, MCPatcher/OptiFine
 * 47-tile layout).
 * <p>
 * The neighbour probing and the tile index mapping are a direct port of
 * OptiFine 1.17.1 {@code ConnectedTextures.getConnectedTextureCtmIndex}
 * (fancy mode, innerSeams off): four cardinal connections are probed in a
 * per-face order and mapped to a base tile index, then four diagonal gaps
 * refine corner/edge tiles. The neighbour direction order per face matches
 * OptiFine's side table, so tiles line up with stock MCPatcher texture
 * sheets regardless of the rendered face.
 * <p>
 * Thread safety: the selector is stateless; it reads only the immutable
 * {@link CtmRuleSet} snapshot and the {@link CtmTileRegistry} icon table
 * (rebuilt per stitch, read-only afterwards), so parallel chunk compilation
 * may call it freely. It allocates nothing per quad.
 * <p>
 * Scope: only {@code method=ctm} is selected here; the other methods
 * (horizontal/vertical/random/...) land in a later stage and fall through
 * to the vanilla texture.
 * <p>
 * P3 (pane specialisation):
 * <ul>
 *   <li>Neighbours of pane-like connectors ({@link BlockPane}, {@link BlockWall},
 *       {@link BlockFence}) must also pass the block's own geometric connection
 *       check ({@code canPaneConnectTo} / {@code canConnectWallTo} /
 *       {@code canConnectFenceTo}, mirroring
 *       {@code VanillaBlockResolvers.PANE}), otherwise a diagonal pane would
 *       count as connected through air;</li>
 *   <li>UP/DOWN quads of panes skip CTM painting on the connecting bars and the
 *       centre post, mirroring OptiFine 1.17.1
 *       {@code ConnectedTextures.skipConnectedTexture}; the core 1.7.10 pane
 *       templates use the same local UVs as the modern originals, so no UV
 *       remapping is needed for parity with OptiFine standalone.</li>
 * </ul>
 */
@SideOnly(Side.CLIENT)
public final class CtmTileSelector {

    private CtmTileSelector() {
    }

    /**
     * Pick the tile icon for a block-world quad, or {@code null} when no
     * rule applies (caller keeps the vanilla texture).
     *
     * @param world        block world of the quad
     * @param x, y, z      block position
     * @param block        the block being rendered
     * @param meta         block metadata
     * @param side         rendered face (Direction ordinal = OptiFine side)
     * @param baseIconName flat base texture name of the quad icon, may be null
     * @param quad         the rendered quad (pane UP/DOWN skip inspection)
     * @return the tile icon to override with, or null
     */
    @Nullable
    public static IIcon select(IBlockAccess world, int x, int y, int z,
                               Block block, int meta, Direction side, String baseIconName,
                               BakedQuad quad) {
        CtmRuleSet rules = CtmManager.getRuleSet();
        if (rules.isEmpty()) {
            return null;
        }
        String baseKey = baseIconName != null ? CtmRuleSet.flatten(baseIconName) : null;
        // Tile-class rules win over block-class rules (MCPatcher format spec).
        if (baseKey != null) {
            for (int ruleIndex : rules.tileRules(baseKey)) {
                IIcon icon = tryRule(rules, ruleIndex, world, x, y, z, block, meta, side, baseKey, quad);
                if (icon != null) {
                    return icon;
                }
            }
        }
        String blockKey = CtmRuleSet.flatten(Block.blockRegistry.getNameForObject(block));
        for (int ruleIndex : rules.blockRules(blockKey)) {
            IIcon icon = tryRule(rules, ruleIndex, world, x, y, z, block, meta, side, baseKey, quad);
            if (icon != null) {
                return icon;
            }
        }
        return null;
    }

    /** Apply one rule: filters, pane skip, 47-layout selection, placeholder lookup. */
    @Nullable
    private static IIcon tryRule(CtmRuleSet rules, int ruleIndex, IBlockAccess world,
                                 int x, int y, int z, Block block, int meta,
                                 Direction side, String baseKey, BakedQuad quad) {
        CtmProperties rule = rules.rules().get(ruleIndex);
        if (!"ctm".equals(rule.method)) {
            return null; // other methods are handled by a later stage
        }
        if (!rule.matchesMetadata(meta) || !rule.matchesFace(side.ordinal())) {
            return null;
        }
        if (skipPaneQuad(block, quad, world, x, y, z, side)) {
            return null; // pane bars/post keep their edge texture (OptiFine skipConnectedTexture)
        }
        int tileIndex = connectedTile(rule, world, x, y, z, side, block, meta, baseKey);
        return CtmTileRegistry.getIcon(CtmTileRegistry.PREFIX + "r" + ruleIndex + "/" + tileIndex);
    }

    /**
     * Compute the 47-layout tile index for a quad face (OptiFine mapping).
     * <p>
     * Cardinal connections are probed per face in OptiFine's order
     * (side 0: [W,E,N,S], 1: [W,E,S,N], 2: [E,W,B,A], 3: [W,E,B,A],
     * 4: [N,S,B,A], 5: [S,N,B,A]), mapped through the base index chain, then
     * the four diagonal gaps refine corners (fancy pass, always on here).
     */
    private static int connectedTile(CtmProperties rule, IBlockAccess world,
                                     int x, int y, int z, Direction side,
                                     Block block, int meta, String baseKey) {
        boolean a0, a1, a2, a3;
        switch (side.ordinal()) {
            case 0: // DOWN: [W, E, N, S]
                a0 = isNeighbour(rule, world, x - 1, y, z, side, block, meta, baseKey);
                a1 = isNeighbour(rule, world, x + 1, y, z, side, block, meta, baseKey);
                a2 = isNeighbour(rule, world, x, y, z - 1, side, block, meta, baseKey);
                a3 = isNeighbour(rule, world, x, y, z + 1, side, block, meta, baseKey);
                break;
            case 1: // UP: [W, E, S, N]
                a0 = isNeighbour(rule, world, x - 1, y, z, side, block, meta, baseKey);
                a1 = isNeighbour(rule, world, x + 1, y, z, side, block, meta, baseKey);
                a2 = isNeighbour(rule, world, x, y, z + 1, side, block, meta, baseKey);
                a3 = isNeighbour(rule, world, x, y, z - 1, side, block, meta, baseKey);
                break;
            case 2: // NORTH: [E, W, B, A]
                a0 = isNeighbour(rule, world, x + 1, y, z, side, block, meta, baseKey);
                a1 = isNeighbour(rule, world, x - 1, y, z, side, block, meta, baseKey);
                a2 = isNeighbour(rule, world, x, y - 1, z, side, block, meta, baseKey);
                a3 = isNeighbour(rule, world, x, y + 1, z, side, block, meta, baseKey);
                break;
            case 3: // SOUTH: [W, E, B, A]
                a0 = isNeighbour(rule, world, x - 1, y, z, side, block, meta, baseKey);
                a1 = isNeighbour(rule, world, x + 1, y, z, side, block, meta, baseKey);
                a2 = isNeighbour(rule, world, x, y - 1, z, side, block, meta, baseKey);
                a3 = isNeighbour(rule, world, x, y + 1, z, side, block, meta, baseKey);
                break;
            case 4: // WEST: [N, S, B, A]
                a0 = isNeighbour(rule, world, x, y, z - 1, side, block, meta, baseKey);
                a1 = isNeighbour(rule, world, x, y, z + 1, side, block, meta, baseKey);
                a2 = isNeighbour(rule, world, x, y - 1, z, side, block, meta, baseKey);
                a3 = isNeighbour(rule, world, x, y + 1, z, side, block, meta, baseKey);
                break;
            default: // EAST: [S, N, B, A]
                a0 = isNeighbour(rule, world, x, y, z + 1, side, block, meta, baseKey);
                a1 = isNeighbour(rule, world, x, y, z - 1, side, block, meta, baseKey);
                a2 = isNeighbour(rule, world, x, y - 1, z, side, block, meta, baseKey);
                a3 = isNeighbour(rule, world, x, y + 1, z, side, block, meta, baseKey);
                break;
        }
        int i = mainIndex(a0, a1, a2, a3);
        if (i == 0) {
            return 0;
        }
        // Fancy pass: diagonal gaps decide corner vs. edge tiles.
        switch (side.ordinal()) {
            case 0: // [E+N, W+N, E+S, W+S]
                a0 = !isNeighbour(rule, world, x + 1, y, z - 1, side, block, meta, baseKey);
                a1 = !isNeighbour(rule, world, x - 1, y, z - 1, side, block, meta, baseKey);
                a2 = !isNeighbour(rule, world, x + 1, y, z + 1, side, block, meta, baseKey);
                a3 = !isNeighbour(rule, world, x - 1, y, z + 1, side, block, meta, baseKey);
                break;
            case 1: // [E+S, W+S, E+N, W+N]
                a0 = !isNeighbour(rule, world, x + 1, y, z + 1, side, block, meta, baseKey);
                a1 = !isNeighbour(rule, world, x - 1, y, z + 1, side, block, meta, baseKey);
                a2 = !isNeighbour(rule, world, x + 1, y, z - 1, side, block, meta, baseKey);
                a3 = !isNeighbour(rule, world, x - 1, y, z - 1, side, block, meta, baseKey);
                break;
            case 2: // [W+B, E+B, W+A, E+A]
                a0 = !isNeighbour(rule, world, x - 1, y - 1, z, side, block, meta, baseKey);
                a1 = !isNeighbour(rule, world, x + 1, y - 1, z, side, block, meta, baseKey);
                a2 = !isNeighbour(rule, world, x - 1, y + 1, z, side, block, meta, baseKey);
                a3 = !isNeighbour(rule, world, x + 1, y + 1, z, side, block, meta, baseKey);
                break;
            case 3: // [E+B, W+B, E+A, W+A]
                a0 = !isNeighbour(rule, world, x + 1, y - 1, z, side, block, meta, baseKey);
                a1 = !isNeighbour(rule, world, x - 1, y - 1, z, side, block, meta, baseKey);
                a2 = !isNeighbour(rule, world, x + 1, y + 1, z, side, block, meta, baseKey);
                a3 = !isNeighbour(rule, world, x - 1, y + 1, z, side, block, meta, baseKey);
                break;
            case 4: // [B+S, B+N, A+S, A+N]
                a0 = !isNeighbour(rule, world, x, y - 1, z + 1, side, block, meta, baseKey);
                a1 = !isNeighbour(rule, world, x, y - 1, z - 1, side, block, meta, baseKey);
                a2 = !isNeighbour(rule, world, x, y + 1, z + 1, side, block, meta, baseKey);
                a3 = !isNeighbour(rule, world, x, y + 1, z - 1, side, block, meta, baseKey);
                break;
            default: // EAST: [B+N, B+S, A+N, A+S]
                a0 = !isNeighbour(rule, world, x, y - 1, z - 1, side, block, meta, baseKey);
                a1 = !isNeighbour(rule, world, x, y - 1, z + 1, side, block, meta, baseKey);
                a2 = !isNeighbour(rule, world, x, y + 1, z - 1, side, block, meta, baseKey);
                a3 = !isNeighbour(rule, world, x, y + 1, z + 1, side, block, meta, baseKey);
                break;
        }
        return cornerIndex(i, a0, a1, a2, a3);
    }

    /** Base tile index from the four cardinal connections (OptiFine chain). */
    private static int mainIndex(boolean a0, boolean a1, boolean a2, boolean a3) {
        if (!a0 && !a1 && !a2 && !a3) {
            return 0;
        }
        if (!a0 && !a1 && !a2 && a3) {
            return 3;
        }
        if (!a0 && !a1 && a2 && !a3) {
            return 1;
        }
        if (!a0 && !a1 && a2 && a3) {
            return 2;
        }
        if (!a0 && a1 && !a2 && !a3) {
            return 12;
        }
        if (!a0 && a1 && !a2 && a3) {
            return 37;
        }
        if (!a0 && a1 && a2 && !a3) {
            return 13;
        }
        if (!a0 && a1 && a2 && a3) {
            return 25;
        }
        if (a0 && !a1 && !a2 && !a3) {
            return 36;
        }
        if (a0 && !a1 && !a2 && a3) {
            return 39;
        }
        if (a0 && !a1 && a2 && !a3) {
            return 15;
        }
        if (a0 && !a1 && a2 && a3) {
            return 27;
        }
        if (a0 && a1 && !a2 && !a3) {
            return 24;
        }
        if (a0 && a1 && !a2 && a3) {
            return 38;
        }
        if (a0 && a1 && a2 && !a3) {
            return 14;
        }
        return 26;
    }

    /** Corner refinement from the four diagonal gaps (OptiFine chain). */
    private static int cornerIndex(int i, boolean a0, boolean a1, boolean a2, boolean a3) {
        if (i == 13 && a0) {
            return 4;
        }
        if (i == 15 && a1) {
            return 5;
        }
        if (i == 37 && a2) {
            return 16;
        }
        if (i == 39 && a3) {
            return 17;
        }
        if (i == 14 && a0 && a1) {
            return 7;
        }
        if (i == 25 && a0 && a2) {
            return 6;
        }
        if (i == 27 && a3 && a1) {
            return 19;
        }
        if (i == 38 && a3 && a2) {
            return 18;
        }
        if (i == 14 && !a0 && a1) {
            return 31;
        }
        if (i == 25 && a0 && !a2) {
            return 30;
        }
        if (i == 27 && !a3 && a1) {
            return 41;
        }
        if (i == 38 && a3 && !a2) {
            return 40;
        }
        if (i == 14 && a0 && !a1) {
            return 29;
        }
        if (i == 25 && !a0 && a2) {
            return 28;
        }
        if (i == 27 && a3 && !a1) {
            return 43;
        }
        if (i == 38 && !a3 && a2) {
            return 42;
        }
        if (i == 26 && a0 && a1 && a2 && a3) {
            return 46;
        }
        if (i == 26 && !a0 && a1 && a2 && a3) {
            return 9;
        }
        if (i == 26 && a0 && !a1 && a2 && a3) {
            return 21;
        }
        if (i == 26 && a0 && a1 && !a2 && a3) {
            return 8;
        }
        if (i == 26 && a0 && a1 && a2 && !a3) {
            return 20;
        }
        if (i == 26 && a0 && a1 && !a2 && !a3) {
            return 11;
        }
        if (i == 26 && !a0 && !a1 && a2 && a3) {
            return 22;
        }
        if (i == 26 && !a0 && a1 && !a2 && a3) {
            return 23;
        }
        if (i == 26 && a0 && !a1 && a2 && !a3) {
            return 10;
        }
        if (i == 26 && a0 && !a1 && !a2 && a3) {
            return 34;
        }
        if (i == 26 && !a0 && a1 && a2 && !a3) {
            return 35;
        }
        if (i == 26 && a0 && !a1 && !a2 && !a3) {
            return 32;
        }
        if (i == 26 && !a0 && a1 && !a2 && !a3) {
            return 33;
        }
        if (i == 26 && !a0 && !a1 && a2 && !a3) {
            return 44;
        }
        if (i == 26 && !a0 && !a1 && !a2 && a3) {
            return 45;
        }
        return i;
    }

    /**
     * Connect semantics for one neighbour position (OptiFine isNeighbour):
     * <ul>
     *   <li>block: same Block instance;</li>
     *   <li>tile: same flat base texture name on the neighbouring face
     *       (resolved through the core model registry, falling back to
     *       {@link Block#getIcon(int, int)});</li>
     *   <li>material: same material;</li>
     *   <li>state: same block and same metadata.</li>
     * </ul>
     * Air is never a neighbour.
     */
    private static boolean isNeighbour(CtmProperties rule, IBlockAccess world,
                                       int x, int y, int z, Direction side,
                                       Block selfBlock, int selfMeta, String selfBaseKey) {
        Block nb = world.getBlock(x, y, z);
        if (nb == null || nb.isAir(world, x, y, z)) {
            return false;
        }
        if (isPaneConnector(selfBlock)) {
            // P3: pane-like connectors (glass panes, iron bars, fences, walls)
            // require a geometric connection (mirroring VanillaBlockResolvers.PANE),
            // so a diagonal pane never counts as connected through air.
            return canConnect(selfBlock, world, x, y, z,
                    ForgeDirection.getOrientation(side.ordinal()));
        }
        switch (rule.connectType) {
            case 1:
                return nb == selfBlock;
            case 2:
                return selfBaseKey != null && selfBaseKey.equals(neighbourBaseKey(world, x, y, z, side, nb));
            case 3:
                return nb.getMaterial() == selfBlock.getMaterial();
            case 4:
                return nb == selfBlock && world.getBlockMetadata(x, y, z) == selfMeta;
            default:
                return false;
        }
    }

    /**
     * True when the block is a pane-like connector whose neighbours must pass
     * a geometric connection check ({@code BlockPane}/{@code BlockWall}/
     * {@code BlockFence} and their subclasses, i.e. everything the core
     * {@code VanillaBlockResolvers.PANE} multipart resolver covers).
     */
    private static boolean isPaneConnector(Block block) {
        return block instanceof BlockPane || block instanceof BlockWall || block instanceof BlockFence;
    }

    /**
     * Geometric connection check, mirroring {@code VanillaBlockResolvers.PANE}:
     * the neighbouring position {@code (x, y, z)} must be connectable for the
     * self block's own type (wall/fence/pane dispatch).
     */
    private static boolean canConnect(Block self, IBlockAccess world,
                                      int x, int y, int z, ForgeDirection dir) {
        if (self instanceof BlockWall) {
            return ((BlockWall) self).canConnectWallTo(world, x, y, z);
        }
        if (self instanceof BlockFence) {
            return ((BlockFence) self).canConnectFenceTo(world, x, y, z);
        }
        if (!(self instanceof BlockPane)) {
            return false;
        }
        return ((BlockPane) self).canPaneConnectTo(world, x, y, z, dir);
    }

    /**
     * Pane UP/DOWN quad skip (OptiFine 1.17.1 skipConnectedTexture port):
     * quads of the connecting bars are skipped while the corresponding
     * direction is connected, and the centre post is always skipped, so the
     * CTM tile never paints the pane top view. The core 1.7.10 pane templates
     * produce only full-face UP/DOWN quads, so OptiFine's isFaceQuad guard is
     * omitted. Connections are probed geometrically (same as the core
     * multipart resolver) rather than decoded from metadata.
     */
    private static boolean skipPaneQuad(Block selfBlock, BakedQuad quad, IBlockAccess world,
                                        int x, int y, int z, Direction side) {
        if (!(selfBlock instanceof BlockPane)) {
            return false;
        }
        if (side != Direction.UP && side != Direction.DOWN) {
            return false;
        }
        double midX = quadMid(quad, true);
        if (midX < 0.4) {
            return canConnect(selfBlock, world, x - 1, y, z, ForgeDirection.WEST);
        } else if (midX > 0.6) {
            return canConnect(selfBlock, world, x + 1, y, z, ForgeDirection.EAST);
        }
        double midZ = quadMid(quad, false);
        if (midZ < 0.4) {
            return canConnect(selfBlock, world, x, y, z - 1, ForgeDirection.NORTH);
        } else if (midZ > 0.6) {
            return canConnect(selfBlock, world, x, y, z + 1, ForgeDirection.SOUTH);
        }
        return true; // centre post: never CTM-painted
    }

    /** Mid-point of the quad's X or Z extent, without allocation. */
    private static double quadMid(BakedQuad quad, boolean xAxis) {
        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            double v = xAxis ? quad.vertices[i].x : quad.vertices[i].z;
            if (v < min) {
                min = v;
            }
            if (v > max) {
                max = v;
            }
        }
        return (min + max) / 2.0;
    }

    /**
     * Flat base texture name of the neighbour's icon on the given face:
     * core model quads first (the authoritative texture for model-overridden
     * blocks), {@link Block#getIcon(int, int)} as fallback.
     */
    @Nullable
    private static String neighbourBaseKey(IBlockAccess world, int x, int y, int z,
                                           Direction side, Block nb) {
        IIcon icon = null;
        if (ModelRegistry.hasModel(nb)) {
            BlockStateModel model = ModelRegistry.getBlockModel(nb);
            BlockStateModelPart part = model != null
                    ? model.collectParts(world, x, y, z, world.getBlockMetadata(x, y, z)) : null;
            if (part != null) {
                List<BakedQuad> quads = part.getQuads(side);
                if (quads != null) {
                    for (BakedQuad quad : quads) {
                        if (quad.icon != null) {
                            icon = quad.icon;
                            break;
                        }
                    }
                }
            }
        }
        if (icon == null) {
            icon = nb.getIcon(side.ordinal(), world.getBlockMetadata(x, y, z));
        }
        return icon != null ? CtmRuleSet.flatten(icon.getIconName()) : null;
    }
}
