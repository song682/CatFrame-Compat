package decok.dfcdvadstf.catframe.config;

import java.util.Arrays;

/**
 * A single config entry: its declared kind, comment, value and the raw value
 * read from the file.
 * <p>
 * The raw value is kept apart from the resolved value so the document can tell
 * "key missing from the file" (fall back to the default) from "key present but
 * unparsable" (fall back to the default and rewrite the file).
 */
public final class ConfigEntry {

    private final String key;
    private final String category;

    private ConfigEntryType type;
    private String comment;
    private Object defaultValue;
    private Object value;

    private Object rawValue;
    private boolean presentInFile;

    ConfigEntry(String category, String key) {
        this.category = category;
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public String getCategory() {
        return category;
    }

    public ConfigEntryType getType() {
        return type;
    }

    public String getComment() {
        return comment;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public Object getValue() {
        return value;
    }

    /**
     * Records a raw value parsed from the file.
     *
     * @param rawValue parsed value, {@code null} when the key is absent
     */
    public void setRawValue(Object rawValue) {
        this.rawValue = rawValue;
        this.presentInFile = rawValue != null;
    }

    public boolean isPresentInFile() {
        return presentInFile;
    }

    /**
     * Applies the declaration coming from the config class and resolves the value
     * to expose.
     *
     * @param type declared kind
     * @param defaultValue value used when the file has no usable value
     * @param comment entry comment
     * @return {@code true} when the on-disk representation has to be rewritten
     */
    public boolean define(ConfigEntryType type, Object defaultValue, String comment) {
        boolean changed = false;

        if (this.type != type) {
            this.type = type;
            changed = true;
        }
        if (!valuesEqual(this.comment, comment)) {
            this.comment = comment;
            changed = true;
        }

        this.defaultValue = defaultValue;

        Object resolved = coerce(rawValue, type);
        if (resolved == null) {
            resolved = defaultValue;
            // Present but unparsable: the file needs to be repaired.
            if (presentInFile) {
                changed = true;
            }
        }

        if (!valuesEqual(this.value, resolved)) {
            this.value = resolved;
            // A value that came from the file and is unchanged must not mark the
            // document dirty, otherwise every launch rewrites the file.
            if (!presentInFile) {
                changed = true;
            }
        }

        return changed;
    }

    /**
     * Replaces the current value, keeping the declared kind.
     *
     * @param newValue new value
     * @return {@code true} when the value actually changed
     */
    public boolean updateValue(Object newValue) {
        Object coerced = coerce(newValue, type);
        if (coerced == null) {
            throw new ConfigException(
                    "Value for entry '" + key + "' in category '" + category + "' is not a " + type);
        }
        if (valuesEqual(this.value, coerced)) {
            return false;
        }
        this.value = coerced;
        this.presentInFile = true;
        return true;
    }

    /**
     * Converts a raw value into the requested kind.
     *
     * @param raw raw value
     * @param type target kind
     * @return converted value, or {@code null} when the raw value is missing or
     *         cannot be converted
     */
    public static Object coerce(Object raw, ConfigEntryType type) {
        if (raw == null || type == null) {
            return null;
        }
        try {
            switch (type) {
                case BOOLEAN:
                    if (raw instanceof Boolean) {
                        return raw;
                    }
                    if (raw instanceof String) {
                        String text = ((String) raw).trim();
                        if ("true".equalsIgnoreCase(text)) {
                            return Boolean.TRUE;
                        }
                        if ("false".equalsIgnoreCase(text)) {
                            return Boolean.FALSE;
                        }
                    }
                    return null;
                case INTEGER:
                    if (raw instanceof Number) {
                        return ((Number) raw).intValue();
                    }
                    if (raw instanceof String) {
                        return Integer.parseInt(((String) raw).trim());
                    }
                    return null;
                case LONG:
                    if (raw instanceof Number) {
                        return ((Number) raw).longValue();
                    }
                    if (raw instanceof String) {
                        return Long.parseLong(((String) raw).trim());
                    }
                    return null;
                case DOUBLE:
                    if (raw instanceof Number) {
                        return ((Number) raw).doubleValue();
                    }
                    if (raw instanceof String) {
                        return Double.parseDouble(((String) raw).trim());
                    }
                    return null;
                case STRING:
                    if (raw instanceof Object[]) {
                        return null;
                    }
                    return String.valueOf(raw);
                case STRING_LIST:
                    if (raw instanceof String[]) {
                        return raw;
                    }
                    if (raw instanceof String) {
                        return new String[] { (String) raw };
                    }
                    if (raw instanceof Iterable) {
                        java.util.List<String> collected = new java.util.ArrayList<String>();
                        for (Object element : (Iterable<?>) raw) {
                            collected.add(element == null ? "" : String.valueOf(element));
                        }
                        return collected.toArray(new String[0]);
                    }
                    return null;
                default:
                    return null;
            }
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Compares two config values, treating array values element-wise.
     *
     * @param left first value
     * @param right second value
     * @return {@code true} when both describe the same value
     */
    public static boolean valuesEqual(Object left, Object right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        if (left instanceof String[] && right instanceof String[]) {
            return Arrays.equals((String[]) left, (String[]) right);
        }
        if (left instanceof String[] || right instanceof String[]) {
            return false;
        }
        return left.equals(right);
    }
}
