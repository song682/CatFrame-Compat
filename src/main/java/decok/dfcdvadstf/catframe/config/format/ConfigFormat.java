package decok.dfcdvadstf.catframe.config.format;

import java.io.File;
import java.io.IOException;

import decok.dfcdvadstf.catframe.config.ConfigDocument;

/**
 * Parser and writer for one plain text config format.
 * <p>
 * Implementations only move values between a file and a {@link ConfigDocument};
 * the tree shape, the default values and the comments come from the config
 * class through {@link decok.dfcdvadstf.catframe.config.IConfigScope}.
 */
public interface ConfigFormat {

    /**
     * Reads the file into the document.
     * <p>
     * A missing file is not an error: the document is left without raw values and
     * marked as not existing, so the caller generates it after the config class
     * declared its entries.
     *
     * @param file file to read
     * @param document document to fill
     * @throws IOException when the file exists but cannot be read
     */
    void read(File file, ConfigDocument document) throws IOException;

    /**
     * Writes the document to the file, creating parent directories as needed.
     *
     * @param file file to write
     * @param document document to persist
     * @throws IOException when the file cannot be written
     */
    void write(File file, ConfigDocument document) throws IOException;
}
