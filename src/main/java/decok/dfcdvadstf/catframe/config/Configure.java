package decok.dfcdvadstf.catframe.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the backing file of a config class and, through the nested
 * annotations, every entry that class exposes.
 * <p>
 * There is no interface to implement. A class is a config class when it carries
 * this annotation, and each config entry is a {@code static}, non-{@code final}
 * field of that class. {@link ConfigManager#register(Class)} reflects over the
 * fields and fills them in, so declaring an entry looks like this:
 *
 * <pre>
 * &#64;Configure(modid = Tags.MODID, type = ConfigType.FORGE_CFG)
 * public final class CompatConfig {
 *
 *     &#64;Configure.Comment("Enable the ItemPhysic compatibility layer")
 *     &#64;Configure.DefaultBoolean(false)
 *     public static boolean itemPhysicCompat;
 * }
 * </pre>
 *
 * Every field needs one of the {@code Default*} annotations. The initialiser of
 * the field is not used as the default value, because nothing guarantees that
 * registration runs before other code touches the field.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Configure {

    /**
     * @return mod id the config belongs to
     */
    String modid();

    /**
     * @return file format, which also decides the file extension
     */
    ConfigType type();

    /**
     * @return file name without extension, defaults to {@link #modid()}
     */
    String name() default "";

    /**
     * Root category of every entry in the class. Dots create sub categories
     * ({@code "ai.pathing"}), so a field of this class ends up at
     * {@code ai.pathing.<field name>}.
     *
     * @return root category, {@code general} by default
     */
    String category() default IConfigScope.GENERAL;

    /**
     * Comment written above the entry. One element per line.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface Comment {

        String[] value();
    }

    /**
     * Marks a field as hidden from the config file. The field keeps whatever value
     * it has.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface Ignore {}

    /**
     * Overrides the entry name, which defaults to the field name.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface Name {

        String value();
    }

    /**
     * Default value of a {@code boolean} field.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface DefaultBoolean {

        boolean value();
    }

    /**
     * Default value of an {@code int} field.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface DefaultInt {

        int value();
    }

    /**
     * Default value of a {@code long} field.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface DefaultLong {

        long value();
    }

    /**
     * Default value of a {@code double} field.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface DefaultDouble {

        double value();
    }

    /**
     * Default value of a {@code String} field.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface DefaultString {

        String value();
    }

    /**
     * Default value of a {@code String[]} field.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface DefaultStringList {

        String[] value();
    }
}
