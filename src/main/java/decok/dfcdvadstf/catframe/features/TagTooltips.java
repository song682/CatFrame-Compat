package decok.dfcdvadstf.catframe.features;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import decok.dfcdvadstf.catframe.CompatConfig;
import decok.dfcdvadstf.catframe.tags.TagLoader;
import decok.dfcdvadstf.catframe.tags.TagManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Appends the CatFrame item tags of the hovered item to its tooltip:
 * <pre>
 * Tag(s):
 * #forge:planks
 * #minecraft:planks
 * </pre>
 * <p>
 * The names are read straight from CatFrame's own tag system
 * ({@link TagManager#itemLoader()}), so every item tag the game knows about is
 * covered: built-in data pack tags, tags contributed by other packs and tags
 * registered in code. PineappleTags needs no special handling here - a
 * PineappleTags tag only shows up once that tag exists as a CatFrame tag.
 * </p>
 * <p>
 * 1.7.10 tags live at registry-object level (no meta/NBT), so a stack always
 * resolves to its {@link Item}. The tag set is queried on every tooltip instead
 * of being cached, which keeps the display correct across
 * {@code /datapack reload}; the scan is bounded by the number of registered
 * item tags and every lookup is an identity comparison against a tag's
 * contents.
 * </p>
 */
public class TagTooltips {

    private static final String HEADER = "Tag(s):";

    private static final String TAG_PREFIX = "#";

    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent event) {
        if (!CompatConfig.tagTooltips) return;

        ItemStack stack = event.itemStack;
        if (stack == null) return;

        Item item = stack.getItem();
        if (item == null) return;

        List<String> tags = collectItemTags(item);
        if (tags.isEmpty()) return;

        List<String> lines = event.toolTip;
        lines.add(HEADER);
        for (String tag : tags) {
            lines.add(TAG_PREFIX + tag);
        }
    }

    /**
     * Collects the ids ({@code namespace:path}) of every item tag containing the
     * given item, sorted alphabetically for a stable tooltip order.
     *
     * @param item the item to look up
     * @return the matching tag ids, or an empty list when the item belongs to none
     */
    private static List<String> collectItemTags(Item item) {
        TagLoader<Item> loader = TagManager.itemLoader();
        Set<ResourceLocation> knownTags = loader.getAllTagNames();
        if (knownTags.isEmpty()) return Collections.emptyList();

        List<String> names = new ArrayList<String>();
        for (ResourceLocation tag : knownTags) {
            if (loader.is(item, tag)) {
                names.add(tag.toString());
            }
        }
        if (names.size() > 1) {
            Collections.sort(names);
        }
        return names;
    }
}
