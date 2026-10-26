package decok.dfcdvadstf.catframe.config.format;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;

/**
 * Small UTF-8 file helpers shared by the plain text config formats.
 */
final class ConfigFiles {

    static final Charset UTF_8 = Charset.forName("UTF-8");

    private ConfigFiles() {
    }

    /**
     * Reads a whole file as UTF-8, dropping a leading byte order mark if present.
     *
     * @param file source file
     * @return file content
     * @throws IOException when the file cannot be read
     */
    static String read(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), UTF_8));
        try {
            StringBuilder builder = new StringBuilder();
            char[] buffer = new char[4096];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                builder.append(buffer, 0, read);
            }
            if (builder.length() > 0 && builder.charAt(0) == '\uFEFF') {
                builder.deleteCharAt(0);
            }
            return builder.toString();
        } finally {
            reader.close();
        }
    }

    /**
     * Writes text as UTF-8, creating parent directories when needed.
     *
     * @param file target file
     * @param content file content
     * @throws IOException when the file cannot be written
     */
    static void write(File file, String content) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs() && !parent.isDirectory()) {
            throw new IOException("Could not create config directory " + parent.getAbsolutePath());
        }
        Writer writer = new OutputStreamWriter(new FileOutputStream(file), UTF_8);
        try {
            writer.write(content);
        } finally {
            writer.close();
        }
    }

    /**
     * @return platform line separator
     */
    static String newline() {
        return System.getProperty("line.separator");
    }
}
