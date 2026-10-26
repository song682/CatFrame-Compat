package decok.dfcdvadstf.catframe.config;

/**
 * Value accessor handed to {@link IConfig#load(IConfigScope)}.
 * <p>
 * Every getter comes in two flavours:
 * <ul>
 * <li>without a category - the entry is stored in {@link #GENERAL};</li>
 * <li>with a category - the entry is stored in that category, using dots for
 * sub categories ({@code "ai.pathing"}).</li>
 * </ul>
 * For {@link ConfigType#FORGE_CFG} the category argument is passed straight to
 * the Forge {@code Configuration}, so a native config is expected to name its
 * categories explicitly. The plain text formats keep the same tree, but omit the
 * {@code general} level entirely when every entry lives there.
 */
public interface IConfigScope {

    /**
     * Category used by the getters that take no category argument.
     */
    String GENERAL = "general";

    /**
     * @param key entry name
     * @param defaultValue value used when the file does not provide one
     * @param comment entry comment, written back to the file
     * @return resolved value
     */
    boolean getBoolean(String key, boolean defaultValue, String comment);

    /**
     * @param category owning category, dots create sub categories
     * @param key entry name
     * @param defaultValue value used when the file does not provide one
     * @param comment entry comment, written back to the file
     * @return resolved value
     */
    boolean getBoolean(String category, String key, boolean defaultValue, String comment);

    int getInt(String key, int defaultValue, String comment);

    int getInt(String category, String key, int defaultValue, String comment);

    long getLong(String key, long defaultValue, String comment);

    long getLong(String category, String key, long defaultValue, String comment);

    double getDouble(String key, double defaultValue, String comment);

    double getDouble(String category, String key, double defaultValue, String comment);

    String getString(String key, String defaultValue, String comment);

    String getString(String category, String key, String defaultValue, String comment);

    String[] getStringList(String key, String[] defaultValue, String comment);

    String[] getStringList(String category, String key, String[] defaultValue, String comment);

    /**
     * Updates an entry in {@link #GENERAL} and marks the file as changed. Does not
     * write the file, call {@link #save()} for that.
     *
     * @param key entry name
     * @param value new value, must match the declared entry kind
     */
    void set(String key, Object value);

    /**
     * Updates an entry in the given category and marks the file as changed. Does
     * not write the file, call {@link #save()} for that.
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
