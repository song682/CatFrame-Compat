package decok.dfcdvadstf.catframe.config;

/**
 * Backing file format of a {@link Configure}d config file.
 * <p>
 * The file extension is implied by the format, so a config declared with
 * {@code @Configure(modid = "catframe", type = ConfigType.TOML)} is stored as
 * {@code config/catframe.toml}.
 */
public enum ConfigType {

    /**
     * Native Forge configuration format. Parsed and written through
     * {@link net.minecraftforge.common.config.Configuration}, which keeps the
     * category tree and per-entry comments.
     */
    FORGE_CFG("cfg"),

    /**
     * JSON with per-entry {@code {"comment": ..., "value": ...}} objects. Comments
     * travel inside the document because plain JSON has no comment syntax.
     */
    JSON("json"),

    /**
     * Java properties style {@code key = value} lines with {@code #} comments.
     */
    PROPERTIES("properties"),

    /**
     * TOML with {@code [category]} tables, {@code key = value} pairs and {@code #} comments.
     */
    TOML("toml");

    private final String extension;

    ConfigType(String extension) {
        this.extension = extension;
    }

    /**
     * @return file extension of this format, without the leading dot
     */
    public String getExtension() {
        return extension;
    }
}
