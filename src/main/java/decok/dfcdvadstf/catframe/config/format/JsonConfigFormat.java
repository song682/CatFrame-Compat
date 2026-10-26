package decok.dfcdvadstf.catframe.config.format;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import decok.dfcdvadstf.catframe.config.ConfigCategoryNode;
import decok.dfcdvadstf.catframe.config.ConfigDocument;
import decok.dfcdvadstf.catframe.config.ConfigValue;
import decok.dfcdvadstf.catframe.config.ConfigException;
import decok.dfcdvadstf.catframe.config.IConfigScope;

/**
 * JSON config format.
 * <p>
 * Plain JSON has no comment syntax, so every entry is stored as an object holding
 * both fields:
 *
 * <pre>
 * {
 *   "general": {
 *     "enableEdgeOfHint": {
 *       "comment": "this is enabling the hint on the edge",
 *       "value": false
 *     }
 *   }
 * }
 * </pre>
 *
 * When every entry lives in {@code general} the category level is omitted and the
 * entry objects sit directly at the top level. Files are written pretty printed.
 */
public final class JsonConfigFormat implements ConfigFormat {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static final String COMMENT = "comment";
    private static final String VALUE = "value";

    @Override
    public void read(File file, ConfigDocument document) throws IOException {
        document.setFileExisted(file.isFile());
        if (!file.isFile()) {
            return;
        }

        String content = ConfigFiles.read(file);
        if (content.trim().isEmpty()) {
            return;
        }

        JsonElement root;
        try {
            root = new JsonParser().parse(content);
        } catch (RuntimeException e) {
            throw new ConfigException("Failed to parse JSON config " + file.getAbsolutePath(), e);
        }
        if (root == null || root.isJsonNull()) {
            return;
        }
        if (!root.isJsonObject()) {
            throw new ConfigException("JSON config " + file.getAbsolutePath() + " must contain a top level object");
        }

        readObject(IConfigScope.GENERAL, root.getAsJsonObject(), document);
    }

    @Override
    public void write(File file, ConfigDocument document) throws IOException {
        boolean omitGeneral = document.hasOnlyGeneral();
        JsonObject root = new JsonObject();

        for (ConfigCategoryNode category : document.getCategories()) {
            if (category.isEmpty()) {
                continue;
            }
            JsonObject target = omitGeneral ? root : navigate(root, category.getQualifiedName());
            for (ConfigValue entry : category.getEntries()) {
                JsonObject holder = new JsonObject();
                if (entry.getComment() != null && !entry.getComment().isEmpty()) {
                    holder.addProperty(COMMENT, entry.getComment());
                }
                holder.add(VALUE, toJson(entry.getValue()));
                target.add(entry.getKey(), holder);
            }
        }

        ConfigFiles.write(file, GSON.toJson(root) + ConfigFiles.newline());
    }

    private static void readObject(String category, JsonObject object, ConfigDocument document) {
        for (Map.Entry<String, JsonElement> member : object.entrySet()) {
            JsonElement value = member.getValue();
            if (isEntryObject(value)) {
                readEntry(category, member.getKey(), value.getAsJsonObject(), document);
            } else if (value.isJsonObject()) {
                String child = IConfigScope.GENERAL.equals(category) ? member.getKey()
                        : category + "." + member.getKey();
                readObject(child, value.getAsJsonObject(), document);
            } else {
                // A bare value without the comment/value wrapper: accept it as an
                // entry in the current category so hand written files still work.
                document.putRawValue(category, member.getKey(), toRaw(value));
            }
        }
    }

    private static void readEntry(String category, String key, JsonObject object, ConfigDocument document) {
        Object raw = object.has(VALUE) ? toRaw(object.get(VALUE)) : null;
        document.putRawValue(category, key, raw);
    }

    private static boolean isEntryObject(JsonElement element) {
        return element != null && element.isJsonObject() && element.getAsJsonObject().has(VALUE);
    }

    private static Object toRaw(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (element.isJsonArray()) {
            List<String> values = new ArrayList<String>();
            JsonArray array = element.getAsJsonArray();
            for (JsonElement child : array) {
                values.add(child == null || child.isJsonNull() ? "" : child.getAsString());
            }
            return values;
        }
        if (element.isJsonObject()) {
            return null;
        }
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (primitive.isBoolean()) {
            return primitive.getAsBoolean();
        }
        if (primitive.isNumber()) {
            return primitive.getAsNumber();
        }
        return primitive.getAsString();
    }

    private static JsonElement toJson(Object value) {
        if (value == null) {
            return JsonNull.INSTANCE;
        }
        if (value instanceof Boolean) {
            return new JsonPrimitive((Boolean) value);
        }
        if (value instanceof Number) {
            return new JsonPrimitive((Number) value);
        }
        if (value instanceof String[]) {
            JsonArray array = new JsonArray();
            for (String element : (String[]) value) {
                array.add(new JsonPrimitive(element));
            }
            return array;
        }
        return new JsonPrimitive(String.valueOf(value));
    }

    private static JsonObject navigate(JsonObject root, String qualifiedName) {
        JsonObject current = root;
        for (String part : qualifiedName.split("\\.")) {
            JsonElement child = current.get(part);
            if (child == null || !child.isJsonObject()) {
                JsonObject created = new JsonObject();
                current.add(part, created);
                current = created;
            } else {
                current = child.getAsJsonObject();
            }
        }
        return current;
    }
}
