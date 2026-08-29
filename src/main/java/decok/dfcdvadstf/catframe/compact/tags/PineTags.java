package decok.dfcdvadstf.catframe.compact.tags;

import club.someoneice.togocup.tags.Tag;
import club.someoneice.togocup.tags.TagsManager;
import decok.dfcdvadstf.catframe.compact.CompactBase;
import decok.dfcdvadstf.catframe.tags.impl.CatFrameTags;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * PineappleTags compatibility layer (modid: pineapple_tag).
 * <p>
 * PineappleTags and CatFrame's tag system are two independent runtime tag
 * implementations. This layer converts the whole PineappleTags tag pool into
 * CatFrame tags at post-init, so that CatFrame tag queries and automatic item
 * detection transparently cover everything registered through PineappleTags.</p>
 *
 * <p>Implementation notes:</p>
 * <ul>
 *   <li>The tag pool container is private with no public enumeration method —
 *       the only reflection point of this class: looked up by field name first,
 *       then by the "static Map field" type signature as fallback. Once the
 *       pool is obtained, tag contents are read via the public
 *       {@code Tag#getList()}.</li>
 *   <li>ItemStack entries are merged by their Item, since CatFrame tags live
 *       at registry-object level (no meta/NBT).</li>
 *   <li>Names containing ":" are split into namespace/path; others fall under
 *       the default namespace.</li>
 * </ul>
 */
public class PineTags {

    /**
     * Default CatFrame namespace for pineapple tags without a ":" separator.
     */
    public static final String DEFAULT_NAMESPACE = "pineapple_tags";

    /** PineappleTags Tag pool private field name ({@code TagsManager.tags}). */
    private static final String TAG_POOL_FIELD = "tags";

    /** Cached handle to the private tag pool. */
    private static Field tagPoolField;

    /** The total number of elements that were last synced into CatFrame (for logging and diagnostics). */
    private static int lastSyncedCount;

    /**
     * Converts the whole PineappleTags tag pool into CatFrame's Tag system.
     * <p>Should be called at post-init (mod registrations into PineappleTags
     * are done by then); if a mod registers tags later, call this method again
     * to refresh — CatFrame tag sets use Set semantics, so repeated syncs are
     * idempotent.</p>
     * <p>Silently skipped when disabled, not installed, or reflection fails;
     * game startup is unaffected.</p>
     *
     * @return the number of elements synced into CatFrame this run
     */
    public static int syncTags() {
        if (!CompactBase.isWolfTagInstalled()) return 0;

        Map<String, ?> pool = obtainTagPool();
        if (pool == null) return 0;

        int count = 0;
        for (Map.Entry<String, ?> entry : pool.entrySet()) {
            if (!(entry.getValue() instanceof Tag)) continue;

            String[] location = splitTagName(entry.getKey());
            String namespace = location[0];
            String path = location[1];

            for (Object element : ((Tag<?>) entry.getValue()).getList()) {
                if (element instanceof Item) {
                    CatFrameTags.add(namespace, path, (Item) element);
                    count++;
                } else if (element instanceof Block) {
                    CatFrameTags.add(namespace, path, (Block) element);
                    count++;
                } else if (element instanceof ItemStack) {
                    // CatFrame tags live at registry-object level: merge ItemStacks
                    // by their Item; meta/NBT differences are dropped.
                    CatFrameTags.add(namespace, path, ((ItemStack) element).getItem());
                    count++;
                }
                // Non-registry elements (Class etc.) cannot map into CatFrame tags; skip them.
            }
        }

        lastSyncedCount = count;
        return count;
    }

    /**
     * Checks whether an item belongs to the given PineappleTags tag (queried via
     * the synced CatFrame tags).
     *
     * @param item    the item to query
     * @param tagName PineappleTags tag name (may use "namespace:path" form)
     */
    public static boolean is(Item item, String tagName) {
        if (!CompactBase.isWolfTagInstalled() || item == null || tagName == null) return false;
        String[] location = splitTagName(tagName);
        return CatFrameTags.is(item, location[0], location[1]);
    }

    /**
     * Checks whether a block belongs to the given PineappleTags tag (queried via
     * the synced CatFrame tags).
     */
    public static boolean is(Block block, String tagName) {
        if (!CompactBase.isWolfTagInstalled() || block == null || tagName == null) return false;
        String[] location = splitTagName(tagName);
        return CatFrameTags.is(block, location[0], location[1]);
    }

    /**
     * Returns the raw PineappleTags tag names of an object (straight through its
     * public API), e.g. for tooltip display; returns an empty list when disabled
     * or the API is unavailable.
     *
     * @param object any object (usually an Item / ItemStack)
     */
    public static List<String> getPineappleTagNames(Object object) {
        if (!CompactBase.isWolfTagInstalled() || object == null) return Collections.emptyList();
        try {
            return TagsManager.manager().getTagsFromObjects(object);
        } catch (Throwable ignored) {
            // PineappleTags API drift: degrade silently.
            return Collections.emptyList();
        }
    }

    /**
     * The number of elements converted into CatFrame by the last {@link #syncTags()}.
     */
    public static int lastSyncedCount() {
        return lastSyncedCount;
    }

    /**
     * Reads PineappleTags' private tag pool {@code TagsManager.tags} via reflection:
     * located by field name first; if renamed, falls back to the declared field
     * that is "static and of type Map" (the pool is the only static Map field of
     * TagsManager). Returns {@code null} and degrades silently on failure
     * (not installed / structure changed).
     */
    @SuppressWarnings("unchecked")
    private static Map<String, ?> obtainTagPool() {
        try {
            if (tagPoolField == null) {
                try {
                    tagPoolField = TagsManager.class.getDeclaredField(TAG_POOL_FIELD);
                } catch (NoSuchFieldException e) {
                    // Field renamed: fall back to the "static Map field" signature.
                    for (Field candidate : TagsManager.class.getDeclaredFields()) {
                        if (Modifier.isStatic(candidate.getModifiers())
                                && Map.class.isAssignableFrom(candidate.getType())) {
                            tagPoolField = candidate;
                            break;
                        }
                    }
                }
                if (tagPoolField == null) return null;
                tagPoolField.setAccessible(true);
            }
            Object pool = tagPoolField.get(null);
            return pool instanceof Map ? (Map<String, ?>) pool : null;
        } catch (Throwable ignored) {
            // PineappleTags not installed or its internals changed: degrade silently.
            return null;
        }
    }

    /**
     * Splits a PineappleTags tag name into [namespace, path]: ":" is the
     * separator when present; otherwise the tag falls under
     * {@value #DEFAULT_NAMESPACE}.
     */
    private static String[] splitTagName(String tagName) {
        int colon = tagName.indexOf(':');
        if (colon > 0 && colon < tagName.length() - 1) {
            return new String[] { tagName.substring(0, colon), tagName.substring(colon + 1) };
        }
        return new String[] { DEFAULT_NAMESPACE, tagName };
    }
}
