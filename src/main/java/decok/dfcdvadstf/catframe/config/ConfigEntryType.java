package decok.dfcdvadstf.catframe.config;

/**
 * Value kind of a single config entry.
 * <p>
 * The kind is declared by the config class itself (through
 * {@link IConfigScope#getInt(String, String, int, String)} and friends), so a
 * malformed value in the file is reported against a known expected type instead
 * of being guessed from the file content.
 */
public enum ConfigEntryType {

    BOOLEAN,
    INTEGER,
    LONG,
    DOUBLE,
    STRING,
    STRING_LIST;

    /**
     * Resolves the kind of a default value.
     *
     * @param value default value passed by the config class
     * @return matching kind
     * @throws ConfigException if the value type is not supported
     */
    public static ConfigEntryType ofDefault(Object value) {
        if (value instanceof Boolean) {
            return BOOLEAN;
        }
        if (value instanceof Integer) {
            return INTEGER;
        }
        if (value instanceof Long) {
            return LONG;
        }
        if (value instanceof Double || value instanceof Float) {
            return DOUBLE;
        }
        if (value instanceof String) {
            return STRING;
        }
        if (value instanceof String[]) {
            return STRING_LIST;
        }
        throw new ConfigException("Unsupported config value type: " + (value == null ? "null" : value.getClass().getName()));
    }

    /**
     * @return whether values of this kind are stored as arrays
     */
    public boolean isList() {
        return this == STRING_LIST;
    }
}
