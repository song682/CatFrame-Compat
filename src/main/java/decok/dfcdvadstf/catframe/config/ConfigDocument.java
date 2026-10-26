package decok.dfcdvadstf.catframe.config;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * In-memory representation of one config file, shared by the plain text formats
 * (JSON, properties, TOML).
 * <p>
 * Category and entry order follows declaration order, so a generated file reads
 * the same way as the config class.
 */
public final class ConfigDocument {

    private final Map<String, ConfigCategoryNode> categories = new LinkedHashMap<String, ConfigCategoryNode>();

    /**
     * Values parsed from the file, keyed by category and entry name. They are
     * consumed when the config class declares the matching entry; anything left
     * over belongs to a key the class no longer declares and is dropped on write.
     */
    private final Map<String, Object> rawValues = new LinkedHashMap<String, Object>();

    private boolean dirty;
    private boolean fileExisted;

    /**
     * Returns the category with the given qualified name, creating it when absent.
     *
     * @param qualifiedName dotted category name, empty or {@code null} meaning {@link IConfigScope#GENERAL}
     * @return category node
     */
    public ConfigCategoryNode category(String qualifiedName) {
        String name = normalize(qualifiedName);
        ConfigCategoryNode node = categories.get(name);
        if (node == null) {
            node = new ConfigCategoryNode(name);
            categories.put(name, node);
        }
        return node;
    }

    /**
     * Returns the entry for the given category and key, creating both when absent.
     *
     * @param qualifiedName dotted category name
     * @param key entry name
     * @return entry node
     */
    public ConfigValue entry(String qualifiedName, String key) {
        String name = normalize(qualifiedName);
        ConfigCategoryNode node = category(name);
        ConfigValue entry = node.find(key);
        if (entry == null) {
            entry = node.entry(key);
            entry.setRawValue(rawValues.remove(rawKey(name, key)));
        }
        return entry;
    }

    /**
     * Stores a value parsed from the file. Parsers call this instead of creating
     * entries directly, so the document order stays driven by the config class
     * rather than by the file.
     *
     * @param qualifiedName dotted category name
     * @param key entry name
     * @param value parsed value
     */
    public void putRawValue(String qualifiedName, String key, Object value) {
        rawValues.put(rawKey(normalize(qualifiedName), key), value);
    }

    /**
     * @return keys present in the file but never declared by the config class
     */
    public java.util.Set<String> getUndeclaredKeys() {
        return Collections.unmodifiableSet(rawValues.keySet());
    }

    private static String rawKey(String category, String key) {
        return category + '\u0000' + key;
    }

    /**
     * @param qualifiedName dotted category name
     * @return category node or {@code null}
     */
    public ConfigCategoryNode find(String qualifiedName) {
        return categories.get(normalize(qualifiedName));
    }

    public Collection<ConfigCategoryNode> getCategories() {
        return Collections.unmodifiableCollection(categories.values());
    }

    /**
     * Whether every entry lives in {@link IConfigScope#GENERAL}. In that case the
     * plain text formats omit the category level entirely.
     *
     * @return {@code true} when only {@code general} exists
     */
    public boolean hasOnlyGeneral() {
        return categories.size() == 1 && categories.containsKey(IConfigScope.GENERAL);
    }

    public boolean isEmpty() {
        for (ConfigCategoryNode node : categories.values()) {
            if (!node.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void markDirty() {
        this.dirty = true;
    }

    /**
     * Clears the pending write flag, called after the document was persisted.
     */
    public void markClean() {
        this.dirty = false;
        this.fileExisted = true;
    }

    public boolean isFileExisted() {
        return fileExisted;
    }

    public void setFileExisted(boolean fileExisted) {
        this.fileExisted = fileExisted;
    }

    /**
     * @return whether the document has to be written back to disk
     */
    public boolean needsWrite() {
        return dirty || !fileExisted;
    }

    /**
     * @param rawKey key produced by the parser
     * @return category part of a raw key
     */
    public static String rawCategoryOf(String rawKey) {
        int split = rawKey.indexOf('\u0000');
        return split < 0 ? rawKey : rawKey.substring(0, split);
    }

    /**
     * @param rawKey key produced by the parser
     * @return entry name part of a raw key
     */
    public static String rawEntryOf(String rawKey) {
        int split = rawKey.indexOf('\u0000');
        return split < 0 ? rawKey : rawKey.substring(split + 1);
    }

    /**
     * Normalizes a category name: {@code null}/empty becomes {@code general}, and
     * names are lower cased to match Forge's default case-insensitive categories.
     *
     * @param qualifiedName raw category name
     * @return normalized qualified name
     */
    public static String normalize(String qualifiedName) {
        if (qualifiedName == null || qualifiedName.trim().isEmpty()) {
            return IConfigScope.GENERAL;
        }
        return qualifiedName.trim().toLowerCase(Locale.ENGLISH);
    }
}
