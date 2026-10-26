package decok.dfcdvadstf.catframe.config;

import java.io.File;
import java.io.IOException;

import decok.dfcdvadstf.catframe.config.format.ConfigFormat;

/**
 * {@link IConfigScope} backed by a {@link ConfigDocument}, used by every format
 * except {@link ConfigType#FORGE_CFG}.
 * <p>
 * Every getter both resolves the value and records the declared kind, comment and
 * default into the document, so the file can be generated or repaired from the
 * same call.
 */
public final class DocumentConfigScope implements IConfigScope {

    private final ConfigDocument document;
    private final ConfigFormat format;
    private final File file;

    public DocumentConfigScope(ConfigDocument document, ConfigFormat format, File file) {
        this.document = document;
        this.format = format;
        this.file = file;
    }

    @Override
    public boolean getBoolean(String category, String key, boolean defaultValue, String comment) {
        return (Boolean) declare(category, key, defaultValue, comment).getValue();
    }

    @Override
    public int getInt(String category, String key, int defaultValue, String comment) {
        return (Integer) declare(category, key, defaultValue, comment).getValue();
    }

    @Override
    public long getLong(String category, String key, long defaultValue, String comment) {
        return (Long) declare(category, key, defaultValue, comment).getValue();
    }

    @Override
    public double getDouble(String category, String key, double defaultValue, String comment) {
        return (Double) declare(category, key, defaultValue, comment).getValue();
    }

    @Override
    public String getString(String category, String key, String defaultValue, String comment) {
        return (String) declare(category, key, defaultValue, comment).getValue();
    }

    @Override
    public String[] getStringList(String category, String key, String[] defaultValue, String comment) {
        return (String[]) declare(category, key, defaultValue, comment).getValue();
    }

    @Override
    public void set(String category, String key, Object value) {
        ConfigValue entry = document.find(category) == null ? null : document.find(category).find(key);
        if (entry == null || entry.getType() == null) {
            throw new ConfigException("Config entry '" + key + "' in category '" + category
                    + "' was never declared, cannot update it");
        }
        if (entry.updateValue(value)) {
            document.markDirty();
        }
    }

    @Override
    public void save() {
        if (!document.needsWrite() || document.isEmpty()) {
            return;
        }
        try {
            format.write(file, document);
        } catch (IOException e) {
            throw new ConfigException("Failed to write config file " + file.getAbsolutePath(), e);
        }
        document.markClean();
    }

    /**
     * @return document backing this scope
     */
    public ConfigDocument getDocument() {
        return document;
    }

    private ConfigValue declare(String category, String key, Object defaultValue, String comment) {
        if (key == null || key.trim().isEmpty()) {
            throw new ConfigException("Config entry name must not be empty (category '" + category + "')");
        }
        ConfigValue entry = document.entry(category, key);
        if (entry.define(ConfigEntryType.ofDefault(defaultValue), defaultValue, comment)) {
            document.markDirty();
        }
        return entry;
    }
}
