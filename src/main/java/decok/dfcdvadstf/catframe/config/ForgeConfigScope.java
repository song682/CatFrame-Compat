package decok.dfcdvadstf.catframe.config;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

/**
 * {@link IConfigScope} backed by the native Forge {@link Configuration}.
 * <p>
 * Categories are handed to Forge untouched, so a {@link ConfigType#FORGE_CFG}
 * config keeps the behaviour of a hand written Forge config: category names are
 * case-insensitive, existing keys the class does not declare are preserved, and
 * comments end up in the generated {@code .cfg}.
 */
public final class ForgeConfigScope implements IConfigScope {

    private final Configuration configuration;
    private final Map<String, Property> properties = new LinkedHashMap<String, Property>();

    public ForgeConfigScope(Configuration configuration) {
        this.configuration = configuration;
        // Reading is what populates the category tree; without it every lookup would
        // create a fresh default and save() would overwrite the user's file.
        configuration.load();
    }

    @Override
    public boolean getBoolean(String category, String key, boolean defaultValue, String comment) {
        Property property = property(category, key, Boolean.toString(defaultValue), comment, Property.Type.BOOLEAN);
        return property.getBoolean(defaultValue);
    }

    @Override
    public int getInt(String category, String key, int defaultValue, String comment) {
        Property property = property(category, key, Integer.toString(defaultValue), comment, Property.Type.INTEGER);
        return property.getInt(defaultValue);
    }

    @Override
    public long getLong(String category, String key, long defaultValue, String comment) {
        // Forge has no long type; the value round-trips as a string instead.
        Property property = property(category, key, Long.toString(defaultValue), comment, Property.Type.STRING);
        try {
            return Long.parseLong(property.getString().trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public double getDouble(String category, String key, double defaultValue, String comment) {
        Property property = property(category, key, Double.toString(defaultValue), comment, Property.Type.DOUBLE);
        return property.getDouble(defaultValue);
    }

    @Override
    public String getString(String category, String key, String defaultValue, String comment) {
        Property property = property(category, key, defaultValue, comment, Property.Type.STRING);
        return property.getString();
    }

    @Override
    public String[] getStringList(String category, String key, String[] defaultValue, String comment) {
        Property property = listProperty(category, key, defaultValue, comment);
        return property.getStringList();
    }

    @Override
    public void set(String category, String key, Object value) {
        Property property = properties.get(cacheKey(category, key));
        if (property == null) {
            throw new ConfigException("Config entry '" + key + "' in category '" + category
                    + "' was never declared, cannot update it");
        }
        if (value instanceof Boolean) {
            property.set((Boolean) value);
        } else if (value instanceof Integer) {
            property.set((Integer) value);
        } else if (value instanceof Double) {
            property.set((Double) value);
        } else if (value instanceof String[]) {
            property.set((String[]) value);
        } else if (value instanceof Long) {
            property.set(Long.toString((Long) value));
        } else {
            property.set(String.valueOf(value));
        }
    }

    @Override
    public void save() {
        if (configuration.hasChanged()) {
            configuration.save();
        }
    }

    /**
     * @return underlying Forge configuration, for callers that need Forge-only features
     */
    public Configuration getConfiguration() {
        return configuration;
    }

    private Property property(String category, String key, String defaultValue, String comment, Property.Type type) {
        String name = ConfigDocument.normalize(category);
        String cacheKey = cacheKey(name, key);
        Property property = properties.get(cacheKey);
        if (property == null) {
            property = configuration.get(name, key, defaultValue, comment, type);
            if (property == null) {
                throw new ConfigException(
                        "Forge refused to create config entry '" + key + "' in category '" + name + "'");
            }
            properties.put(cacheKey, property);
        }
        return property;
    }

    private Property listProperty(String category, String key, String[] defaultValue, String comment) {
        String name = ConfigDocument.normalize(category);
        String cacheKey = cacheKey(name, key);
        Property property = properties.get(cacheKey);
        if (property == null) {
            property = configuration.get(name, key, defaultValue, comment);
            if (property == null) {
                throw new ConfigException(
                        "Forge refused to create config entry '" + key + "' in category '" + name + "'");
            }
            properties.put(cacheKey, property);
        }
        return property;
    }

    private static String cacheKey(String category, String key) {
        return category + '\u0000' + key;
    }
}
