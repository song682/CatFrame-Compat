package decok.dfcdvadstf.catframe.config;

/**
 * Thrown when a config class is declared incorrectly or when a config file
 * cannot be parsed or written.
 * <p>
 * Config errors are considered programming errors on the mod side (missing
 * annotation, missing interface, malformed file), so this is unchecked.
 */
public class ConfigException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ConfigException(String message) {
        super(message);
    }

    public ConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
