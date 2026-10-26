package decok.dfcdvadstf.catframe.config;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One category of a config document, identified by its dotted qualified name.
 * <p>
 * Sub categories are stored as flat qualified names ({@code "ai.pathing"})
 * rather than as a tree: the plain text formats render the dots themselves, and
 * a parent category without entries does not have to exist.
 */
public final class ConfigCategoryNode {

    private final String qualifiedName;
    private final Map<String, ConfigEntry> entries = new LinkedHashMap<String, ConfigEntry>();

    ConfigCategoryNode(String qualifiedName) {
        this.qualifiedName = qualifiedName;
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

    /**
     * @return last segment of the qualified name
     */
    public String getSimpleName() {
        int split = qualifiedName.lastIndexOf('.');
        return split < 0 ? qualifiedName : qualifiedName.substring(split + 1);
    }

    /**
     * Returns the entry with the given key, creating it when absent.
     *
     * @param key entry name
     * @return entry bound to this category
     */
    public ConfigEntry entry(String key) {
        ConfigEntry entry = entries.get(key);
        if (entry == null) {
            entry = new ConfigEntry(qualifiedName, key);
            entries.put(key, entry);
        }
        return entry;
    }

    /**
     * @param key entry name
     * @return entry or {@code null} when the key was never declared
     */
    public ConfigEntry find(String key) {
        return entries.get(key);
    }

    public Collection<ConfigEntry> getEntries() {
        return Collections.unmodifiableCollection(entries.values());
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }
}
