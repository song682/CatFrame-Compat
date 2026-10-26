package decok.dfcdvadstf.catframe;

import decok.dfcdvadstf.catframe.compact.Tags;
import decok.dfcdvadstf.catframe.config.ConfigEntry;
import decok.dfcdvadstf.catframe.config.ConfigType;
import decok.dfcdvadstf.catframe.config.Configure;

/**
 * Usage example for the annotation driven config system.
 * <p>
 * This class exists to show how the framework is meant to be used and to exercise
 * every annotation at least once; nothing in the mod reads these fields. Registering
 * it writes {@code config/catframe-example.cfg} on first launch.
 * <p>
 * The layout is the one every config class should follow:
 * <ul>
 * <li>the class carries {@link Configure} carrying the mod id and the file format;</li>
 * <li>an entry is a {@code static}, non-{@code final} field carrying exactly one kind
 * annotation, {@link ConfigEntry.Boolean}, {@link ConfigEntry.Int},
 * {@link ConfigEntry.Long}, {@link ConfigEntry.Double}, {@link ConfigEntry.Text} or
 * {@link ConfigEntry.List};</li>
 * <li>{@link ConfigEntry.Comment}, {@link ConfigEntry.Category} and
 * {@link ConfigEntry.Name} describe it;</li>
 * <li>an {@code int}, {@code long} or {@code double} entry may add {@code min} and
 * {@code max} to its kind annotation to bound the value.</li>
 * </ul>
 * Fields live in {@code general} unless they carry {@link ConfigEntry.Category}, and
 * the field name is the entry name unless they carry {@link ConfigEntry.Name}.
 */
@Configure(modid = Tags.MODID, type = ConfigType.FORGE_CFG, name = "catframe-example")
public final class ExampleConfig {

    private ExampleConfig() {
    }

    // ---- general -----------------------------------------------------------------

    @ConfigEntry.Comment("Master switch for everything this example shows.")
    @ConfigEntry.Boolean(true)
    public static boolean enabled;

    @ConfigEntry.Comment({
            "How many entries the example loader keeps in memory.",
            "The bound keeps a hand edited value from exhausting the heap."
    })
    @ConfigEntry.Int(value = 512, min = 1, max = 8192)
    public static int cacheSize;

    @ConfigEntry.Comment({
            "Seed of the example layout pass, zero derives it from the world hash.",
            "Large enough that an int would not hold it; cfg files store it as a string",
            "because Forge has no long type of its own."
    })
    @ConfigEntry.Long(value = 0L, min = 0L, max = 1000000000000L)
    public static long layoutSeed;

    @ConfigEntry.Comment("Scale applied to the example overlay.")
    @ConfigEntry.Double(value = 1.0D, min = 0.25D, max = 4.0D)
    public static double overlayScale;

    @ConfigEntry.Comment("Theme id the example overlay resolves its sprites from.")
    @ConfigEntry.Text("catframe:vanilla")
    public static String themeId;

    @ConfigEntry.Comment("Resource paths the example loader scans, in order. Every string is kept as written.")
    @ConfigEntry.List({
            "assets/catframe",
            "assets/minecraft"
    })
    public static String[] scanPaths;

    // ---- debug -------------------------------------------------------------------

    @ConfigEntry.Comment("Logs every example frame. Extremely noisy, debug only.")
    @ConfigEntry.Category("debug")
    @ConfigEntry.Name("logEveryFrame")
    @ConfigEntry.Boolean(false)
    public static boolean debugEveryFrame;

    @ConfigEntry.Comment("Minimum number of frames between two example log lines.")
    @ConfigEntry.Category("debug")
    @ConfigEntry.Int(value = 100, min = 1, max = 5000)
    public static int debugLogInterval;

    // ---- not an entry ------------------------------------------------------------

    /**
     * Shows how a {@code final} field, which cannot be assigned, is kept out of the
     * entry set. Constants of this class belong here; without
     * {@link ConfigEntry.Ignore} registration rejects the config class.
     */
    @ConfigEntry.Ignore
    public static final String EXAMPLE_MARKER = "catframe-example";
}
