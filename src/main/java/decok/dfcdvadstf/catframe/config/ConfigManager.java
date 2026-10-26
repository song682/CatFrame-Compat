package decok.dfcdvadstf.catframe.config;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
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
 * A config class carries {@link Configure} and implements {@link IConfig}. Both
 * are mandatory: registering a class that satisfies only one of them fails, which
 * keeps the annotation and the behaviour from drifting apart.
 * <p>
 * Typical usage, from {@code FMLPreInitializationEvent}:
 *
 * <pre>
 * ConfigManager.init(event.getModConfigurationDirectory());
 * ConfigManager.register(CatFrameConfig.class);
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
     * @param configClass class carrying {@link Configure} and implementing {@link IConfig}
     * @throws ConfigException when the class is declared incorrectly or its file cannot be handled
     */
    public static void register(Class<?> configClass) {
        if (configClass == null) {
            throw new ConfigException("Config class must not be null");
        }
        Configure annotation = configClass.getAnnotation(Configure.class);
        boolean implementsConfig = IConfig.class.isAssignableFrom(configClass);

        if (annotation == null && !implementsConfig) {
            throw new ConfigException(configClass.getName()
                    + " is neither annotated with @Configure nor implements IConfig");
        }
        if (annotation == null) {
            throw new ConfigException(configClass.getName()
                    + " implements IConfig but is missing the @Configure annotation");
        }
        if (!implementsConfig) {
            throw new ConfigException(configClass.getName()
                    + " is annotated with @Configure but does not implement IConfig");
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
        IConfig instance = instantiate(configClass);

        IConfigScope scope = annotation.type() == ConfigType.FORGE_CFG ? loadForge(configClass, instance, file)
                : loadDocument(configClass, instance, file, annotation.type());

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
        Configure annotation = configClass.getAnnotation(Configure.class);
        if (annotation == null) {
            throw new ConfigException("Config class " + configClass.getName() + " is missing @Configure");
        }
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

    private static IConfigScope loadForge(Class<?> configClass, IConfig instance, File file) {
        Configuration configuration = new Configuration(file);
        ForgeConfigScope scope = new ForgeConfigScope(configuration);
        invokeLoad(configClass, instance, scope);
        scope.save();
        return scope;
    }

    private static IConfigScope loadDocument(Class<?> configClass, IConfig instance, File file, ConfigType type) {
        ConfigFormat format = createFormat(type);
        ConfigDocument document = new ConfigDocument();

        try {
            format.read(file, document);
        } catch (IOException e) {
            throw new ConfigException("Failed to read config file " + file.getAbsolutePath(), e);
        }

        DocumentConfigScope scope = new DocumentConfigScope(document, format, file);
        invokeLoad(configClass, instance, scope);

        for (String raw : document.getUndeclaredKeys()) {
            LOGGER.debug("Dropping config entry '{}' in category '{}' that {} no longer declares",
                    ConfigDocument.rawEntryOf(raw), ConfigDocument.rawCategoryOf(raw),
                    configClass.getSimpleName());
        }

        scope.save();
        return scope;
    }

    private static void invokeLoad(Class<?> configClass, IConfig instance, IConfigScope scope) {
        try {
            instance.load(scope);
        } catch (RuntimeException e) {
            throw new ConfigException("Config class " + configClass.getName() + " failed to load", e);
        }
    }

    private static IConfig instantiate(Class<?> configClass) {
        try {
            return (IConfig) configClass.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException e) {
            throw new ConfigException(
                    "Config class " + configClass.getName() + " needs a public no-argument constructor", e);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new ConfigException("Could not instantiate config class " + configClass.getName(), e);
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
