package decok.dfcdvadstf.catframe.compact.mcpatcher.ctm;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable snapshot of all parsed CTM rules for the current resource reload.
 * <p>
 * The whole snapshot is replaced on reload (volatile swap in {@link CtmManager}),
 * so render threads may read it without locks. Two-level indexing by tile name
 * and block name is added in P2 when the selector lands.
 */
@SideOnly(Side.CLIENT)
public final class CtmRuleSet {

    /** Shared empty rule set used before the first scan and when CTM is disabled. */
    public static final CtmRuleSet EMPTY = new CtmRuleSet(Collections.<CtmProperties>emptyList());

    private final List<CtmProperties> rules;

    public CtmRuleSet(List<CtmProperties> rules) {
        this.rules = Collections.unmodifiableList(new ArrayList<>(rules));
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
}
