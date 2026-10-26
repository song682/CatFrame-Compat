package decok.dfcdvadstf.catframe.config.format;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import decok.dfcdvadstf.catframe.config.ConfigCategoryNode;
import decok.dfcdvadstf.catframe.config.ConfigDocument;
import decok.dfcdvadstf.catframe.config.ConfigValue;
import decok.dfcdvadstf.catframe.config.ConfigException;
import decok.dfcdvadstf.catframe.config.IConfigScope;

/**
 * TOML config format.
 * <p>
 * Categories are TOML tables ({@code [ai.pathing]}), entries are {@code key = value}
 * pairs and comments are {@code #} lines written above the entry. When every entry
 * lives in {@code general} no table header is written at all and the pairs sit at
 * the top of the file.
 * <p>
 * The parser covers the subset a config file needs: basic and literal strings,
 * integers (including {@code 0x}/{@code 0o}/{@code 0b} and {@code _} separators),
 * floats, booleans, single line arrays and dotted keys. Multi line strings, inline
 * tables and arrays of tables are rejected with a clear error instead of being
 * silently misread.
 */
public final class TomlConfigFormat implements ConfigFormat {

    @Override
    public void read(File file, ConfigDocument document) throws IOException {
        document.setFileExisted(file.isFile());
        if (!file.isFile()) {
            return;
        }

        String content = ConfigFiles.read(file);
        String[] lines = content.split("\r?\n");
        String currentCategory = IConfigScope.GENERAL;

        for (int i = 0; i < lines.length; i++) {
            String line = stripComment(lines[i]).trim();
            if (line.isEmpty()) {
                continue;
            }

            if (line.charAt(0) == '[') {
                currentCategory = parseTableHeader(line, file);
                continue;
            }

            int separator = findKeyValueSeparator(line);
            if (separator < 0) {
                throw new ConfigException(
                        "Malformed TOML line in " + file.getAbsolutePath() + ": " + line);
            }

            String rawKey = line.substring(0, separator).trim();
            String valueText = line.substring(separator + 1).trim();

            if (valueText.startsWith("[") && bracketBalance(valueText) > 0) {
                StringBuilder joined = new StringBuilder(valueText);
                while (i + 1 < lines.length && bracketBalance(joined.toString()) > 0) {
                    i++;
                    String next = stripComment(lines[i]).trim();
                    if (!next.isEmpty()) {
                        joined.append(' ').append(next);
                    }
                }
                valueText = joined.toString();
            }

            boolean quotedKey = rawKey.startsWith("\"") || rawKey.startsWith("'");
            String key = unquoteKey(rawKey);
            String category = currentCategory;
            String entryKey = key;

            int dot = quotedKey ? -1 : key.lastIndexOf('.');
            if (dot > 0) {
                String prefix = key.substring(0, dot);
                category = IConfigScope.GENERAL.equals(currentCategory) ? prefix : currentCategory + "." + prefix;
                entryKey = key.substring(dot + 1);
            }

            document.putRawValue(category, entryKey, parseValue(valueText, file));
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
            if (!omitGeneral) {
                if (out.length() > 0) {
                    out.append(ConfigFiles.newline());
                }
                out.append('[').append(category.getQualifiedName()).append(']').append(ConfigFiles.newline());
            }
            for (ConfigValue entry : category.getEntries()) {
                appendComment(out, entry.getComment());
                out.append(formatKey(entry.getKey()))
                        .append(" = ")
                        .append(formatValue(entry.getValue()))
                        .append(ConfigFiles.newline());
            }
        }

        ConfigFiles.write(file, out.toString());
    }

    private static String parseTableHeader(String line, File file) {
        if (line.startsWith("[[")) {
            throw new ConfigException(
                    "Arrays of tables are not supported in config file " + file.getAbsolutePath() + ": " + line);
        }
        int close = line.lastIndexOf(']');
        if (close < 0) {
            throw new ConfigException("Malformed TOML table header in " + file.getAbsolutePath() + ": " + line);
        }
        String name = line.substring(1, close).trim();
        if (name.isEmpty()) {
            throw new ConfigException("Empty TOML table name in " + file.getAbsolutePath());
        }
        StringBuilder normalized = new StringBuilder();
        for (String part : name.split("\\.")) {
            if (normalized.length() > 0) {
                normalized.append('.');
            }
            normalized.append(unquoteKey(part.trim()));
        }
        return normalized.toString();
    }

    private static String unquoteKey(String rawKey) {
        if (rawKey.length() < 2) {
            return rawKey;
        }
        char first = rawKey.charAt(0);
        if (first == '"' && rawKey.charAt(rawKey.length() - 1) == '"') {
            return unescapeBasic(rawKey.substring(1, rawKey.length() - 1));
        }
        if (first == '\'' && rawKey.charAt(rawKey.length() - 1) == '\'') {
            return rawKey.substring(1, rawKey.length() - 1);
        }
        return rawKey;
    }

    private static Object parseValue(String text, File file) {
        if (text.isEmpty()) {
            throw new ConfigException("Missing TOML value in " + file.getAbsolutePath());
        }
        char first = text.charAt(0);
        if (first == '"') {
            return parseBasicString(text, file);
        }
        if (first == '\'') {
            return parseLiteralString(text, file);
        }
        if (first == '[') {
            return parseArray(text, file);
        }
        if (first == '{') {
            throw new ConfigException(
                    "Inline TOML tables are not supported in config file " + file.getAbsolutePath());
        }
        if ("true".equals(text)) {
            return Boolean.TRUE;
        }
        if ("false".equals(text)) {
            return Boolean.FALSE;
        }
        return parseNumber(text, file);
    }

    private static String parseBasicString(String text, File file) {
        int end = text.lastIndexOf('"');
        if (end <= 0) {
            throw new ConfigException("Unterminated TOML string in " + file.getAbsolutePath() + ": " + text);
        }
        return unescapeBasic(text.substring(1, end));
    }

    private static String parseLiteralString(String text, File file) {
        int end = text.lastIndexOf('\'');
        if (end <= 0) {
            throw new ConfigException("Unterminated TOML string in " + file.getAbsolutePath() + ": " + text);
        }
        return text.substring(1, end);
    }

    private static Object parseArray(String text, File file) {
        int close = text.lastIndexOf(']');
        if (close < 0) {
            throw new ConfigException("Unterminated TOML array in " + file.getAbsolutePath() + ": " + text);
        }
        String body = text.substring(1, close).trim();
        List<Object> values = new ArrayList<Object>();
        if (body.isEmpty()) {
            return values;
        }
        for (String part : splitTopLevel(body)) {
            String element = part.trim();
            if (element.isEmpty()) {
                continue;
            }
            values.add(parseValue(element, file));
        }
        return values;
    }

    private static Object parseNumber(String text, File file) {
        String normalized = text.replace("_", "");
        try {
            if (normalized.startsWith("0x") || normalized.startsWith("0X")) {
                return Long.parseLong(normalized.substring(2), 16);
            }
            if (normalized.startsWith("0o") || normalized.startsWith("0O")) {
                return Long.parseLong(normalized.substring(2), 8);
            }
            if (normalized.startsWith("0b") || normalized.startsWith("0B")) {
                return Long.parseLong(normalized.substring(2), 2);
            }
            if (normalized.indexOf('.') >= 0 || normalized.indexOf('e') >= 0 || normalized.indexOf('E') >= 0) {
                return Double.valueOf(normalized);
            }
            return Long.valueOf(normalized);
        } catch (NumberFormatException e) {
            throw new ConfigException(
                    "Unsupported TOML value in " + file.getAbsolutePath() + ": " + text, e);
        }
    }

    private static String stripComment(String line) {
        boolean inBasic = false;
        boolean inLiteral = false;
        boolean escaped = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (inBasic) {
                if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inBasic = false;
                }
            } else if (inLiteral) {
                if (c == '\'') {
                    inLiteral = false;
                }
            } else if (c == '"') {
                inBasic = true;
            } else if (c == '\'') {
                inLiteral = true;
            } else if (c == '#') {
                return line.substring(0, i);
            }
        }
        return line;
    }

    private static int findKeyValueSeparator(String line) {
        boolean inBasic = false;
        boolean inLiteral = false;
        boolean escaped = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (inBasic) {
                if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inBasic = false;
                }
            } else if (inLiteral) {
                if (c == '\'') {
                    inLiteral = false;
                }
            } else if (c == '"') {
                inBasic = true;
            } else if (c == '\'') {
                inLiteral = true;
            } else if (c == '=') {
                return i;
            }
        }
        return -1;
    }

    private static int bracketBalance(String text) {
        int balance = 0;
        boolean inBasic = false;
        boolean inLiteral = false;
        boolean escaped = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (inBasic) {
                if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inBasic = false;
                }
            } else if (inLiteral) {
                if (c == '\'') {
                    inLiteral = false;
                }
            } else if (c == '"') {
                inBasic = true;
            } else if (c == '\'') {
                inLiteral = true;
            } else if (c == '[') {
                balance++;
            } else if (c == ']') {
                balance--;
            }
        }
        return balance;
    }

    private static List<String> splitTopLevel(String body) {
        List<String> parts = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        boolean inBasic = false;
        boolean inLiteral = false;
        boolean escaped = false;
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (escaped) {
                current.append(c);
                escaped = false;
                continue;
            }
            if (inBasic) {
                current.append(c);
                if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inBasic = false;
                }
            } else if (inLiteral) {
                current.append(c);
                if (c == '\'') {
                    inLiteral = false;
                }
            } else if (c == '"') {
                inBasic = true;
                current.append(c);
            } else if (c == '\'') {
                inLiteral = true;
                current.append(c);
            } else if (c == ',') {
                parts.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            parts.add(current.toString());
        }
        return parts;
    }

    private static String unescapeBasic(String text) {
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
                case '"':
                    out.append('"');
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

    private static void appendComment(StringBuilder out, String comment) {
        if (comment == null || comment.isEmpty()) {
            return;
        }
        for (String line : comment.split("\r?\n")) {
            out.append("# ").append(line).append(ConfigFiles.newline());
        }
    }

    private static String formatKey(String key) {
        boolean bare = !key.isEmpty();
        for (int i = 0; i < key.length() && bare; i++) {
            char c = key.charAt(i);
            bare = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_'
                    || c == '-';
        }
        return bare ? key : '"' + escapeBasic(key) + '"';
    }

    private static String formatValue(Object value) {
        if (value == null) {
            return "\"\"";
        }
        if (value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof Number) {
            return formatNumber((Number) value);
        }
        if (value instanceof String[]) {
            StringBuilder out = new StringBuilder("[");
            String[] elements = (String[]) value;
            for (int i = 0; i < elements.length; i++) {
                if (i > 0) {
                    out.append(", ");
                }
                out.append('"').append(escapeBasic(elements[i])).append('"');
            }
            return out.append(']').toString();
        }
        return '"' + escapeBasic(String.valueOf(value)) + '"';
    }

    private static String formatNumber(Number number) {
        if (number instanceof Double || number instanceof Float) {
            double value = number.doubleValue();
            if (Double.isNaN(value)) {
                return "nan";
            }
            if (Double.isInfinite(value)) {
                return value > 0 ? "inf" : "-inf";
            }
            return Double.toString(value);
        }
        return number.toString();
    }

    private static String escapeBasic(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '\\':
                    out.append("\\\\");
                    break;
                case '"':
                    out.append("\\\"");
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
                default:
                    out.append(c);
                    break;
            }
        }
        return out.toString();
    }
}
