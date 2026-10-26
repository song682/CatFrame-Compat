package decok.dfcdvadstf.catframe.config.format;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import decok.dfcdvadstf.catframe.config.ConfigCategoryNode;
import decok.dfcdvadstf.catframe.config.ConfigDocument;
import decok.dfcdvadstf.catframe.config.ConfigEntry;
import decok.dfcdvadstf.catframe.config.ConfigException;
import decok.dfcdvadstf.catframe.config.IConfigScope;

/**
 * Java properties style config format.
 * <p>
 * Categories are expressed as a dotted key prefix ({@code ai.pathing.depth}) and
 * the {@code general} level is omitted when every entry lives there. Comments are
 * {@code #} lines written above the entry. List values are stored as a JSON array
 * literal ({@code ["a", "b"]}) so elements may contain commas.
 */
public final class PropertiesConfigFormat implements ConfigFormat {

    @Override
    public void read(File file, ConfigDocument document) throws IOException {
        document.setFileExisted(file.isFile());
        if (!file.isFile()) {
            return;
        }

        String content = ConfigFiles.read(file);
        for (String line : content.split("\r?\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) {
                continue;
            }
            int separator = findSeparator(line);
            if (separator < 0) {
                throw new ConfigException(
                        "Malformed properties line in " + file.getAbsolutePath() + ": " + trimmed);
            }
            String rawKey = line.substring(0, separator).trim();
            String rawValue = line.substring(separator + 1).trim();
            if (rawKey.isEmpty()) {
                throw new ConfigException(
                        "Properties line without a key in " + file.getAbsolutePath() + ": " + trimmed);
            }
            int split = rawKey.lastIndexOf('.');
            String category = split < 0 ? IConfigScope.GENERAL : rawKey.substring(0, split);
            String key = split < 0 ? rawKey : rawKey.substring(split + 1);
            document.putRawValue(category, key, parseValue(rawValue));
        }
    }

    @Override
    public void write(File file, ConfigDocument document) throws IOException {
        boolean omitGeneral = document.hasOnlyGeneral();
        StringBuilder out = new StringBuilder();

        for (ConfigCategoryNode category : document.getCategories()) {
            if (category.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(ConfigFiles.newline());
            }
            for (ConfigEntry entry : category.getEntries()) {
                appendComment(out, entry.getComment());
                String key = omitGeneral ? entry.getKey()
                        : category.getQualifiedName() + "." + entry.getKey();
                out.append(key).append(" = ").append(formatValue(entry.getValue())).append(ConfigFiles.newline());
            }
        }

        ConfigFiles.write(file, out.toString());
    }

    private static void appendComment(StringBuilder out, String comment) {
        if (comment == null || comment.isEmpty()) {
            return;
        }
        for (String line : comment.split("\r?\n")) {
            out.append("# ").append(line).append(ConfigFiles.newline());
        }
    }

    /**
     * Locates the first unescaped {@code =} or {@code :} acting as the key/value
     * separator.
     */
    private static int findSeparator(String line) {
        boolean escaped = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (escaped) {
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '=' || c == ':') {
                return i;
            }
        }
        return -1;
    }

    private static Object parseValue(String rawValue) {
        if (rawValue.length() >= 2 && rawValue.charAt(0) == '[' && rawValue.charAt(rawValue.length() - 1) == ']') {
            try {
                JsonElement parsed = new JsonParser().parse(rawValue);
                if (parsed.isJsonArray()) {
                    JsonArray array = parsed.getAsJsonArray();
                    List<String> values = new ArrayList<String>();
                    for (JsonElement element : array) {
                        values.add(element == null || element.isJsonNull() ? "" : element.getAsString());
                    }
                    return values;
                }
            } catch (RuntimeException ignored) {
                // Not an array literal after all, keep the raw text below.
            }
        }
        return unescape(rawValue);
    }

    private static String unescape(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c != '\\' || i + 1 >= text.length()) {
                out.append(c);
                continue;
            }
            char next = text.charAt(++i);
            switch (next) {
                case 'n':
                    out.append('\n');
                    break;
                case 'r':
                    out.append('\r');
                    break;
                case 't':
                    out.append('\t');
                    break;
                case '\\':
                    out.append('\\');
                    break;
                default:
                    out.append('\\').append(next);
                    break;
            }
        }
        return out.toString();
    }

    private static String formatValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof Number) {
            return value.toString();
        }
        if (value instanceof String[]) {
            StringBuilder out = new StringBuilder("[");
            String[] elements = (String[]) value;
            for (int i = 0; i < elements.length; i++) {
                if (i > 0) {
                    out.append(", ");
                }
                out.append('"').append(escape(elements[i])).append('"');
            }
            return out.append(']').toString();
        }
        return escape(String.valueOf(value));
    }

    private static String escape(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '\\':
                    out.append("\\\\");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                case '"':
                    out.append("\\\"");
                    break;
                default:
                    out.append(c);
                    break;
            }
        }
        return out.toString();
    }
}
