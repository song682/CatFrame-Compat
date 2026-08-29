package decok.dfcdvadstf.catframe.compact.mcpatcher.ctm;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable snapshot of all parsed CTM rules for the current resource reload.
 * <p>
 * The whole snapshot is replaced on reload (volatile swap in {@link CtmManager}),
 * so render threads may read it without locks. Two-level indexing by tile name
 * and block name (built once in the constructor, tile-class rules checked
 * before block-class rules by the selector) keeps the render hot path
 * allocation-free.
 */
@SideOnly(Side.CLIENT)
public final class CtmRuleSet {

    /** Shared empty rule set used before the first scan and when CTM is disabled. */
    public static final CtmRuleSet EMPTY = new CtmRuleSet(Collections.<CtmProperties>emptyList());

    private final List<CtmProperties> rules;
    /** Flat base texture name -&gt; rule indices whose matchTiles contain it. */
    private final Map<String, List<Integer>> byTile;
    /** Flat block registry name -&gt; rule indices whose matchBlocks contain it. */
    private final Map<String, List<Integer>> byBlock;

    public CtmRuleSet(List<CtmProperties> rules) {
        this.rules = Collections.unmodifiableList(new ArrayList<>(rules));
        this.byTile = new HashMap<>();
        this.byBlock = new HashMap<>();
        List<CtmProperties> list = this.rules;
        for (int i = 0; i < list.size(); i++) {
            CtmProperties rule = list.get(i);
            for (String tile : rule.matchTiles) {
                addIndex(byTile, flatten(tile), i);
            }
            for (String block : rule.matchBlocks) {
                addIndex(byBlock, flatten(block), i);
            }
        }
    }

    /** No rules loaded: the render extension short-circuits entirely. */
    public boolean isEmpty() {
        return rules.isEmpty();
    }

    /** Number of valid rules in this snapshot. */
    public int size() {
        return rules.size();
    }

    /** Read-only view of the rules. */
    public List<CtmProperties> rules() {
        return rules;
    }

    /**
     * Rule indices whose matchTiles contain the flat base icon name
     * (empty list when none). Tile-class rules take priority over
     * block-class rules per the MCPatcher format spec.
     */
    public List<Integer> tileRules(String baseName) {
        List<Integer> list = byTile.get(baseName);
        return list != null ? list : Collections.<Integer>emptyList();
    }

    /** Rule indices whose matchBlocks contain the flat block registry name (empty when none). */
    public List<Integer> blockRules(String blockName) {
        List<Integer> list = byBlock.get(blockName);
        return list != null ? list : Collections.<Integer>emptyList();
    }

    /**
     * Strip a "namespace:" prefix for flat-key comparison
     * (e.g. "minecraft:glass" -&gt; "glass"). Null passes through.
     */
    public static String flatten(String key) {
        if (key == null) {
            return null;
        }
        int colon = key.indexOf(':');
        return colon >= 0 ? key.substring(colon + 1) : key;
    }

    private static void addIndex(Map<String, List<Integer>> index, String key, int ruleIndex) {
        if (key == null || key.isEmpty()) {
            return;
        }
        List<Integer> list = index.get(key);
        if (list == null) {
            list = new ArrayList<>();
            index.put(key, list);
        }
        list.add(ruleIndex);
    }
}
