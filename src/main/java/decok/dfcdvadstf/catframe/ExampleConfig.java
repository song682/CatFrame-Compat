package decok.dfcdvadstf.catframe;

import decok.dfcdvadstf.catframe.compact.Tags;
import decok.dfcdvadstf.catframe.config.ConfigException;
import decok.dfcdvadstf.catframe.config.ConfigType;
import decok.dfcdvadstf.catframe.config.Configure;
import decok.dfcdvadstf.catframe.config.IConfig;
import decok.dfcdvadstf.catframe.config.IConfigScope;

/**
 * Reference config class for the {@code decok.dfcdvadstf.catframe.config} system.
 * <p>
 * Every config class needs the same three pieces: this {@link Configure}
 * annotation naming the file, an {@link IConfig#load(IConfigScope)} body that
 * declares each entry together with its default value and comment, and one static
 * field per entry that the rest of the mod reads. The annotation and the interface
 * are both mandatory - {@code ConfigManager.register} rejects a class that only has
 * one of them.
 * <p>
 * The file name is spelled out instead of falling back to the modid, because
 * {@link CompatConfig} already owns {@code catframe_compat.cfg}; without an
 * explicit name the two classes would write to the same file.
 * <p>
 * Registration is explicit, {@code ConfigManager} documents why auto discovery is
 * not usable here:
 *
 * <pre>
 * ConfigManager.init(event.getModConfigurationDirectory());
 * ConfigManager.register(ExampleConfig.class);
 * </pre>
 */
@Configure(modid = Tags.MODID, type = ConfigType.FORGE_CFG, name = "catframe-example")
public final class ExampleConfig implements IConfig {

    /** Category of the entries that are not global switches, written as {@code [example]}. */
    private static final String CATEGORY_EXAMPLE = "example";

    /**
     * Scope handed to {@link #load(IConfigScope)}, kept so {@link #saveExampleName(String)}
     * can write back after registration finished.
     */
    private static IConfigScope scope;

    // [general] - no category argument.
    /** Master switch of the example config. */
    public static boolean exampleEnabled;
    /** Free form label, only used by the example. */
    public static String exampleName;

    // [example] - explicit category argument.
    /** Retry budget of a single compatibility probe. */
    public static int maxRetries;
    /** Probe timeout, stored as a string because Forge has no long type. */
    public static long timeoutMillis;
    /** Confidence above which a detected resource pack counts as a match. */
    public static double threshold;
    /** Arbitrary labels, written as a comma separated list. */
    public static String[] tags;

    @Override
    public void load(IConfigScope scope) {
        ExampleConfig.scope = scope;

        // No category argument: both entries land in [general].
        exampleEnabled = scope.getBoolean("exampleEnabled", true,
                "Master switch of the example config. Turn it off to see that the generated file keeps "
                        + "the entry and its comment while the rest of the mod ignores the option.");
        exampleName = scope.getString("exampleName", "catframe",
                "Free form label, only used by the example.");

        // Explicit category: these land in [example].
        maxRetries = scope.getInt(CATEGORY_EXAMPLE, "maxRetries", 3,
                "How many times a failing compatibility probe is retried before it is reported as absent.");
        timeoutMillis = scope.getLong(CATEGORY_EXAMPLE, "timeoutMillis", 30000L,
                "Timeout of a single probe in milliseconds. Stored as a string because Forge has no long type.");
        threshold = scope.getDouble(CATEGORY_EXAMPLE, "threshold", 0.75D,
                "Confidence above which a detected resource pack is treated as a match.");
        tags = scope.getStringList(CATEGORY_EXAMPLE, "tags", new String[] { "catframe", "example" },
                "Arbitrary labels, written as a comma separated list.");
    }

    /**
     * Updates {@code exampleName} in memory and on disk.
     * <p>
     * Mirrors {@link CompatConfig#saveActiveTheme(String)}: {@link IConfigScope#set}
     * only marks the file as changed, the write itself happens in
     * {@link IConfigScope#save()}.
     *
     * @param name new label
     * @throws ConfigException when the class was never registered
     */
    public static void saveExampleName(String name) {
        if (scope == null) {
            throw new ConfigException("ExampleConfig has not been registered yet");
        }
        exampleName = name;
        scope.set("exampleName", name);
        scope.save();
    }
}
