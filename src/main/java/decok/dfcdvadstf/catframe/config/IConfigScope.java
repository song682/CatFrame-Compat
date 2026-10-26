package decok.dfcdvadstf.catframe.config;

/**
 * Backend accessor used by {@link ConfigManager} to reach one backing file.
 * <p>
 * One implementation per storage model: {@link ForgeConfigScope} for
 * {@link ConfigType#FORGE_CFG}, {@link DocumentConfigScope} for the plain text
 * formats. Every getter takes its category explicitly, because the category is
 * declared once on {@link Configure#category()} and shared by all fields of that
 * class; the {@code general} level is omitted by the plain text formats when it is
 * the only one in use.
 */
public interface IConfigScope {

    /**
     * Category used when a config class does not override
     * {@link Configure#category()}.
     */
    String GENERAL = "general";

    /**
     * @param category owning category, dots create sub categories
     * @param key entry name
     * @param defaultValue value used when the file does not provide one
     * @param comment entry comment, written back to the file
     * @return resolved value
     */
    boolean getBoolean(String category, String key, boolean defaultValue, String comment);

    int getInt(String category, String key, int defaultValue, String comment);

    long getLong(String category, String key, long defaultValue, String comment);

    double getDouble(String category, String key, double defaultValue, String comment);

    String getString(String category, String key, String defaultValue, String comment);

    String[] getStringList(String category, String key, String[] defaultValue, String comment);

    /**
     * Updates an entry and marks the file as changed. Does not write the file, call
     * {@link #save()} for that.
     *
     * @param category owning category
     * @param key entry name
     * @param value new value, must match the declared entry kind
     */
    void set(String category, String key, Object value);

    /**
     * Writes pending changes back to disk.
     */
    void save();
}
