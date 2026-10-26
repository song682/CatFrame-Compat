package decok.dfcdvadstf.catframe.config.format;

import java.io.File;
import java.io.IOException;

import decok.dfcdvadstf.catframe.config.ConfigCategoryNode;
import decok.dfcdvadstf.catframe.config.ConfigDocument;
import decok.dfcdvadstf.catframe.config.ConfigEntryType;
import decok.dfcdvadstf.catframe.config.ConfigValue;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

/**
 * Forge native {@code .cfg} format.
 * <p>
 * Parsing and serialisation are delegated to {@link Configuration} so a generated
 * file is byte for byte what a hand written Forge config looks like: the
 * {@code B:}/{@code I:}/{@code D:}/{@code S:} type tags, the multi line
 * {@code S:key < ... >} list blocks, the category braces and the comment layout all
 * come from Forge itself rather than from this class.
 * <p>
 * Like the other formats this is a full rewrite. The document the config class
 * produces is the only source of truth, so entries the class no longer declares do
 * not survive, and neither do Forge's shared module sections
 * ({@code # Start of module X}); every registered config class is expected to own
 * its file. {@link #write(File, ConfigDocument)} therefore deliberately never
 * calls {@link Configuration#load()}, which would pull the previous content into the
 * very tree being written out.
 * <p>
 * The dotted qualified name is shared with {@link ConfigDocument} without translation:
 * Forge splits a category on {@link Configuration#CATEGORY_SPLITTER} and keeps parent
 * and child in one flat map keyed by the qualified name, so {@code "ui.theme"} becomes
 * {@code ui { theme { ... } } } and reads back as {@code "ui.theme"}.
 * <p>
 * {@link ConfigEntryType#LONG} has no Forge counterpart, {@link Property.Type} has no
 * long, so the value travels as {@link Property.Type#STRING} and is re-parsed on read.
 */
public final class ForgeCfgFormat implements ConfigFormat {

    @Override
    public void read(File file, ConfigDocument document) throws IOException {
        document.setFileExisted(file.isFile());
        if (!file.isFile()) {
            return;
        }

        Configuration configuration = new Configuration(file);
        try {
            configuration.load();
        } catch (RuntimeException e) {
            throw new IOException("Failed to parse Forge cfg " + file.getAbsolutePath(), e);
        }

        for (String name : configuration.getCategoryNames()) {
            ConfigCategory category = configuration.getCategory(name);
            for (Property property : category.getOrderedValues()) {
                if (!property.wasRead()) {
                    continue;
                }
                document.putRawValue(name, property.getName(), rawOf(property));
            }
        }
    }

    @Override
    public void write(File file, ConfigDocument document) throws IOException {
        Configuration configuration = new Configuration(file);

        for (ConfigCategoryNode category : document.getCategories()) {
            if (category.isEmpty()) {
                continue;
            }
            for (ConfigValue entry : category.getEntries()) {
                declare(configuration, category.getQualifiedName(), entry);
            }
        }

        configuration.save();
    }

    /**
     * Creates the property Forge expects for this kind and stores the resolved value.
     * <p>
     * The default value is handed to Forge so {@link Property#isDefault()} and a GUI
     * reset keep working. The value itself is written separately because a fresh
     * {@link Configuration} starts empty and without that extra write the file would
     * only ever contain defaults.
     */
    private static void declare(Configuration configuration, String category, ConfigValue entry) {
        String comment = entry.getComment() == null ? "" : entry.getComment();

        if (entry.getType() == ConfigEntryType.STRING_LIST) {
            Property property = configuration.get(category, entry.getKey(), defaultList(entry), comment);
            property.set((String[]) entry.getValue());
            return;
        }

        Property property = configuration.get(category, entry.getKey(), text(entry.getDefaultValue()), comment,
                forgeType(entry.getType()));
        property.set(text(entry.getValue()));
    }

    private static String[] defaultList(ConfigValue entry) {
        Object fallback = entry.getDefaultValue();
        return fallback instanceof String[] ? (String[]) fallback : new String[0];
    }

    /**
     * Both directions hand values over as raw strings and let
     * {@link ConfigValue#coerce(Object, ConfigEntryType)} interpret them against the
     * kind declared by the config class, which is what the plain text formats do.
     */
    private static Object rawOf(Property property) {
        return property.isList() ? property.getStringList() : property.getString();
    }

    private static Property.Type forgeType(ConfigEntryType type) {
        switch (type) {
            case BOOLEAN:
                return Property.Type.BOOLEAN;
            case INTEGER:
                return Property.Type.INTEGER;
            case DOUBLE:
                return Property.Type.DOUBLE;
            default:
                return Property.Type.STRING;
        }
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
