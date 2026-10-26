package decok.dfcdvadstf.catframe.config;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import decok.dfcdvadstf.catframe.config.format.ConfigFormat;
import decok.dfcdvadstf.catframe.config.format.JsonConfigFormat;
import decok.dfcdvadstf.catframe.config.format.PropertiesConfigFormat;
import decok.dfcdvadstf.catframe.config.format.TomlConfigFormat;
import net.minecraftforge.common.config.Configuration;

/**
 * Entry point of the config system.
 * <p>
 * A config class is a class carrying {@link Configure}; every config entry is a
 * {@code static}, non-{@code final} field of that class, annotated with one of the
 * {@code Default*} annotations plus, optionally, {@link Configure.Comment},
 * {@link Configure.Name} or {@link Configure.Ignore}. Registration reflects over
 * those fields, resolves each value from the backing file and assigns it back.
 * <p>
 * Typical usage, from {@code FMLPreInitializationEvent}:
 *
 * <pre>
 * ConfigManager.init(event.getModConfigurationDirectory());
 * ConfigManager.register(CompatConfig.class);
 * </pre>
 *
 * Registration loads the file immediately, applies the values and writes the file
 * back when it is missing, incomplete or unparsable. No discovery is attempted:
 * the jar of this mod ships {@code TweakClass}/{@code FMLCorePlugin} manifest
 * entries, which makes FML skip it during mod discovery, so {@code ASMDataTable}
 * based scanning would work in development but silently miss the classes in
 * production.
 */
public final class ConfigManager {

    private static final Logger LOGGER = LogManager.getLogger("CatFrameCompat|Config");

    private static final Map<String, IConfigScope> SCOPES = new LinkedHashMap<String, IConfigScope>();
    private static final Map<Class<?>, String> KEYS = new LinkedHashMap<Class<?>, String>();

    private static File configDirectory;

    private ConfigManager() {
    }

    /**
     * Sets the directory config files are created in. Must be called before the
     * first {@link #register(Class)}.
     *
     * @param directory directory, usually {@code event.getModConfigurationDirectory()}
     */
    public static void init(File directory) {
        if (directory == null) {
            throw new ConfigException("Config directory must not be null");
        }
        configDirectory = directory;
    }

    /**
     * Loads a config class and binds it to its file.
     *
     * @param configClass class carrying {@link Configure}
     * @throws ConfigException when the class is declared incorrectly or its file cannot be handled
     */
    public static void register(Class<?> configClass) {
        if (configClass == null) {
            throw new ConfigException("Config class must not be null");
        }
        Configure annotation = configClass.getAnnotation(Configure.class);
        if (annotation == null) {
            throw new ConfigException(configClass.getName() + " is missing the @Configure annotation");
        }
        if (configDirectory == null) {
            throw new ConfigException("ConfigManager.init(...) must be called before registering " + configClass.getName());
        }

        String modid = annotation.modid().trim();
        if (modid.isEmpty()) {
            throw new ConfigException("@Configure(modid = ...) must not be empty on " + configClass.getName());
        }
        String name = annotation.name().trim().isEmpty() ? modid : annotation.name().trim();
        String key = modid + '|' + annotation.type().name() + '|' + name;

        if (SCOPES.containsKey(key)) {
            throw new ConfigException("Config file '" + name + "." + annotation.type().getExtension()
                    + "' is already registered for mod '" + modid + "'");
        }

        File file = new File(configDirectory, name + "." + annotation.type().getExtension());
        List<Field> fields = configFields(configClass);

        IConfigScope scope;
        if (annotation.type() == ConfigType.FORGE_CFG) {
            scope = new ForgeConfigScope(new Configuration(file));
            applyDeclarations(scope, annotation, fields);
        } else {
            DocumentConfigScope documentScope = documentScope(file, annotation.type());
            applyDeclarations(documentScope, annotation, fields);
            logUndeclared(documentScope.getDocument(), configClass);
            scope = documentScope;
        }
        scope.save();

        SCOPES.put(key, scope);
        KEYS.put(configClass, key);

        LOGGER.info("Loaded {} config from {}", configClass.getSimpleName(), file.getAbsolutePath());
    }

    /**
     * @param configClass previously registered config class
     * @return scope bound to that class
     * @throws ConfigException when the class was never registered
     */
    public static IConfigScope getScope(Class<?> configClass) {
        String key = KEYS.get(configClass);
        if (key == null) {
            throw new ConfigException("Config class " + configClass.getName() + " was never registered");
        }
        return SCOPES.get(key);
    }

    /**
     * @param configClass previously registered config class
     * @return config file of that class
     * @throws ConfigException when the class was never registered
     */
    public static File getFile(Class<?> configClass) {
        String key = KEYS.get(configClass);
        if (key == null) {
            throw new ConfigException("Config class " + configClass.getName() + " was never registered");
        }
        Configure annotation = configClass.getAnnotation(Configure.class);
        String name = annotation.name().trim().isEmpty() ? annotation.modid().trim() : annotation.name().trim();
        return new File(configDirectory, name + "." + annotation.type().getExtension());
    }

    /**
     * Writes pending changes of every registered config.
     */
    public static void saveAll() {
        for (IConfigScope scope : SCOPES.values()) {
            scope.save();
        }
    }

    /**
     * Collects the config entries of a class.
     *
     * @param configClass class carrying {@link Configure}
     * @return fields in declaration order, excluding the ones marked {@link Configure.Ignore}
     */
    private static List<Field> configFields(Class<?> configClass) {
        List<Field> fields = new ArrayList<Field>();
        for (Field field : configClass.getDeclaredFields()) {
            if (field.isSynthetic()) {
                continue;
            }
            boolean ignored = field.isAnnotationPresent(Configure.Ignore.class);
            if (!Modifier.isStatic(field.getModifiers())) {
                if (ignored) {
                    continue;
                }
                throw new ConfigException("Config field " + describe(field)
                        + " is not static. Config entries must be static fields");
            }
            if (Modifier.isFinal(field.getModifiers())) {
                if (ignored) {
                    continue;
                }
                throw new ConfigException("Config field " + describe(field)
                        + " is final and cannot be assigned. Add @Configure.Ignore to exclude it");
            }
            if (ignored) {
                continue;
            }
            fields.add(field);
        }
        return fields;
    }

    private static void applyDeclarations(IConfigScope scope, Configure annotation, List<Field> fields) {
        String category = annotation.category();
        for (Field field : fields) {
            String key = entryName(field);
            String comment = commentOf(field);
            field.setAccessible(true);
            try {
                field.set(null, resolve(scope, category, key, comment, field));
            } catch (IllegalAccessException e) {
                throw new ConfigException("Could not assign config field " + describe(field), e);
            }
        }
    }

    private static Object resolve(IConfigScope scope, String category, String key, String comment, Field field) {
        Class<?> type = field.getType();
        if (type == boolean.class) {
            return scope.getBoolean(category, key, defaultOf(field, Configure.DefaultBoolean.class).value(), comment);
        }
        if (type == int.class) {
            return scope.getInt(category, key, defaultOf(field, Configure.DefaultInt.class).value(), comment);
        }
        if (type == long.class) {
            return scope.getLong(category, key, defaultOf(field, Configure.DefaultLong.class).value(), comment);
        }
        if (type == double.class) {
            return scope.getDouble(category, key, defaultOf(field, Configure.DefaultDouble.class).value(), comment);
        }
        if (type == String.class) {
            return scope.getString(category, key, defaultOf(field, Configure.DefaultString.class).value(), comment);
        }
        if (type == String[].class) {
            return scope.getStringList(category, key, defaultOf(field, Configure.DefaultStringList.class).value(),
                    comment);
        }
        throw new ConfigException("Config field " + describe(field) + " has unsupported type " + type.getName()
                + ". Supported types are boolean, int, long, double, String and String[]");
    }

    private static <A extends java.lang.annotation.Annotation> A defaultOf(Field field, Class<A> annotation) {
        A value = field.getAnnotation(annotation);
        if (value == null) {
            throw new ConfigException("Config field " + describe(field) + " is missing @" + annotation.getSimpleName());
        }
        return value;
    }

    private static String entryName(Field field) {
        Configure.Name name = field.getAnnotation(Configure.Name.class);
        if (name == null) {
            return field.getName();
        }
        String value = name.value().trim();
        if (value.isEmpty()) {
            throw new ConfigException("Config field " + describe(field) + " has an empty @Configure.Name");
        }
        return value;
    }

    private static String commentOf(Field field) {
        Configure.Comment comment = field.getAnnotation(Configure.Comment.class);
        if (comment == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (String line : comment.value()) {
            if (out.length() > 0) {
                out.append('\n');
            }
            out.append(line);
        }
        return out.toString();
    }

    private static String describe(Field field) {
        return field.getDeclaringClass().getName() + '.' + field.getName();
    }

    private static DocumentConfigScope documentScope(File file, ConfigType type) {
        ConfigFormat format = createFormat(type);
        ConfigDocument document = new ConfigDocument();
        try {
            format.read(file, document);
        } catch (IOException e) {
            throw new ConfigException("Failed to read config file " + file.getAbsolutePath(), e);
        }
        return new DocumentConfigScope(document, format, file);
    }

    private static void logUndeclared(ConfigDocument document, Class<?> configClass) {
        for (String raw : document.getUndeclaredKeys()) {
            LOGGER.debug("Dropping config entry '{}' in category '{}' that {} no longer declares",
                    ConfigDocument.rawEntryOf(raw), ConfigDocument.rawCategoryOf(raw), configClass.getSimpleName());
        }
    }

    private static ConfigFormat createFormat(ConfigType type) {
        switch (type) {
            case JSON:
                return new JsonConfigFormat();
            case PROPERTIES:
                return new PropertiesConfigFormat();
            case TOML:
                return new TomlConfigFormat();
            default:
                throw new ConfigException("No plain text parser for config type " + type);
        }
    }
}
