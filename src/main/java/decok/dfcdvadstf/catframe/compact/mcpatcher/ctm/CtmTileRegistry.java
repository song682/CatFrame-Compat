package decok.dfcdvadstf.catframe.compact.mcpatcher.ctm;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import decok.dfcdvadstf.catframe.CatFrameCompact;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.IIcon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registers CTM tile textures on the vanilla block atlas (textureType 0).
 * <p>
 * Every tile of every rule is registered under a synthetic placeholder key
 * ({@code catframe_ctm/r<ruleIndex>/<tileIndex>}) during
 * {@link net.minecraftforge.client.event.TextureStitchEvent.Pre}. Atlas
 * loading then resolves the key through the resource manager, which would
 * fail for a nonexistent {@code blocks/catframe_ctm/...} file; the
 * {@code MixinTextureMap} redirect instead substitutes the real pack path
 * (e.g. {@code mcpatcher/ctm/glass/glass_0.png}) before the lookup, so the
 * actual CTM texture file is stitched into the atlas under the placeholder
 * key. After stitching ({@link net.minecraftforge.client.event.TextureStitchEvent.Post})
 * the {@link IIcon} of each placeholder is collected for the render stage.
 * <p>
 * Both tables are rebuilt from scratch on every stitch (Pre clears the key
 * table, Post clears the icon table), mirroring {@code VanillaTextureTracker}:
 * the atlas's registered-sprite map is cleared between stitches, and the
 * second stitch fired by F3+T / resource pack switching must pick up the new
 * atlas.
 */
@SideOnly(Side.CLIENT)
public final class CtmTileRegistry {

    /** Placeholder key prefix registered on the block atlas for every CTM tile. */
    public static final String PREFIX = "catframe_ctm/";

    private static final Map<String, String> PLACEHOLDER_TO_PACK_PATH = new HashMap<>();
    private static final Map<String, IIcon> PLACEHOLDER_TO_ICON = new HashMap<>();
    private static final List<String> PLACEHOLDER_KEYS = new ArrayList<>();

    private CtmTileRegistry() {
    }

    /**
     * Rebuild the placeholder table for the current rule set.
     * Call during TextureStitchEvent.Pre when {@code getTextureType() == 0}.
     *
     * @param rules the current rule snapshot (may be empty)
     */
    public static void buildPlaceholders(CtmRuleSet rules) {
        PLACEHOLDER_TO_PACK_PATH.clear();
        PLACEHOLDER_KEYS.clear();
        if (rules.isEmpty()) {
            return;
        }
        List<CtmProperties> ruleList = rules.rules();
        for (int ruleIndex = 0; ruleIndex < ruleList.size(); ruleIndex++) {
            CtmProperties rule = ruleList.get(ruleIndex);
            List<String> tiles = rule.tiles;
            for (int tileIndex = 0; tileIndex < tiles.size(); tileIndex++) {
                String tile = tiles.get(tileIndex);
                // "<skip>"/"<default>" are placeholders, not texture files
                if (tile.startsWith("<")) {
                    continue;
                }
                String key = PREFIX + "r" + ruleIndex + "/" + tileIndex;
                String packPath = rule.basePath + "/" + tile
                        + (tile.endsWith(".png") ? "" : ".png");
                PLACEHOLDER_TO_PACK_PATH.put(key, packPath);
                PLACEHOLDER_KEYS.add(key);
            }
        }
        if (CatFrameCompact.config.ctmDebugLog) {
            CatFrameCompact.logger.debug("CTM: built {} tile placeholders from {} rules",
                    PLACEHOLDER_KEYS.size(), ruleList.size());
        }
    }

    /**
     * Resolve a placeholder key to the real pack texture path (relative to
     * {@code assets/minecraft/}), or {@code null} when the key is not a
     * registered placeholder.
     * <p>
     * Called by {@code MixinTextureMap} during atlas loading to substitute the
     * real CTM texture file.
     */
    public static String resolvePackPath(String placeholderKey) {
        return PLACEHOLDER_TO_PACK_PATH.get(placeholderKey);
    }

    /**
     * Collect the stitched {@link IIcon} for every placeholder.
     * Call during TextureStitchEvent.Post when {@code getTextureType() == 0}.
     * Rebuilds the icon table fully, so a second stitch picks up the new
     * atlas (double-stitch safe).
     */
    public static void collectIcons(TextureMap map) {
        PLACEHOLDER_TO_ICON.clear();
        if (PLACEHOLDER_KEYS.isEmpty()) {
            return;
        }
        IIcon missing = map.getAtlasSprite("missingno");
        int collected = 0;
        int missed = 0;
        for (String key : PLACEHOLDER_KEYS) {
            IIcon icon = map.getAtlasSprite(key);
            if (icon != null && icon != missing) {
                PLACEHOLDER_TO_ICON.put(key, icon);
                collected++;
            } else {
                missed++;
                if (CatFrameCompact.config.ctmDebugLog) {
                    CatFrameCompact.logger.debug("CTM: tile '{}' not uploaded to block atlas (texture file missing)", key);
                }
            }
        }
        if (missed == 0) {
            CatFrameCompact.logger.info("CTM: all {} tile placeholders stitched into block atlas", collected);
        } else {
            CatFrameCompact.logger.warn("CTM: {}/{} tile placeholders stitched into block atlas, {} missing",
                    collected, PLACEHOLDER_KEYS.size(), missed);
        }
    }

    /** Collected {@link IIcon} for a placeholder key, or {@code null} when not yet stitched. */
    public static IIcon getIcon(String placeholderKey) {
        return PLACEHOLDER_TO_ICON.get(placeholderKey);
    }

    /** Ordered list of all placeholder keys (diagnostics / iteration). */
    public static List<String> keys() {
        return Collections.unmodifiableList(PLACEHOLDER_KEYS);
    }

    /** Number of placeholders registered for the current stitch. */
    public static int size() {
        return PLACEHOLDER_KEYS.size();
    }
}
