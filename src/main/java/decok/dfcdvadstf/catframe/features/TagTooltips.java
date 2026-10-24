package decok.dfcdvadstf.catframe.features;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import decok.dfcdvadstf.catframe.CompatConfig;
import decok.dfcdvadstf.catframe.compact.CompactBase;
import decok.dfcdvadstf.catframe.compact.tags.PineTags;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Appends the tags of the hovered item to its tooltip:
 * <pre>
 * Tag(s):
 * #modid:tags
 * #modid2:tags3
 * </pre>
 * Tag names come from PineappleTags via {@link PineTags}. Names without a namespace
 * fall under {@link PineTags#DEFAULT_NAMESPACE}, matching the CatFrame tag mapping.
 */
public class TagTooltips {

    private static final String HEADER = "Tag(s):";

    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent event) {
        if (!CompatConfig.tagTooltips || !CompactBase.isWolfTagInstalled()) return;

        ItemStack stack = event.itemStack;
        if (stack == null || stack.getItem() == null) return;

        Set<String> names = collectTags(stack);
        if (names.isEmpty()) return;

        List<String> lines = event.toolTip;
        lines.add(HEADER);
        for (String name : names) {
            lines.add("#" + name);
        }
    }

    /**
     * Collects tag names for the stack, querying with the ItemStack first (meta/NBT-specific tags)
     * and then with the bare Item, merging results while keeping order and removing duplicates.
     */
    private static Set<String> collectTags(ItemStack stack) {
        Set<String> result = new LinkedHashSet<String>();
        for (String raw : PineTags.getPineappleTagNames(stack)) {
            addName(result, raw);
        }
        Item item = stack.getItem();
        for (String raw : PineTags.getPineappleTagNames(item)) {
            addName(result, raw);
        }
        return result;
    }

    private static void addName(Set<String> out, String raw) {
        if (raw == null || raw.isEmpty()) return;
        out.add(raw.indexOf(':') > 0 ? raw : PineTags.DEFAULT_NAMESPACE + ":" + raw);
    }
}
