package decok.dfcdvadstf.catframe.config;

/**
 * Marks a class as a config file holder.
 * <p>
 * A config class declares its file through {@link Configure} and reads its
 * values in {@link #load(IConfigScope)}. Both are required:
 * {@link ConfigManager#register(Class)} refuses classes that only implement
 * this interface or only carry the annotation.
 */
public interface IConfig {

    /**
     * Registers every config entry of this class and applies the resolved value
     * to its fields.
     * <p>
     * The scope performs both jobs in one call: when the entry already exists in
     * the file the parsed value is returned, otherwise the default value is
     * returned and the file is generated or completed afterwards. Implementations
     * therefore never have to distinguish "existing file" from "new file".
     *
     * @param scope value accessor bound to this config file
     */
    void load(IConfigScope scope);
}
