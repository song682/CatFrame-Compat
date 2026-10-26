package decok.dfcdvadstf.catframe.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Describes one config entry, placed on a {@code static}, non-{@code final} field
 * of a class carrying {@link Configure}.
 * <p>
 * Exactly one of the type annotations must be present, and it both selects the
 * kind of the entry and carries the default value:
 *
 * <pre>
 * &#64;ConfigEntry.Comment("Enable the ItemPhysic compatibility layer")
 * &#64;ConfigEntry.Boolean(false)
 * public static boolean itemPhysicCompat;
 *
 * &#64;ConfigEntry.Comment("Active UI theme id")
 * &#64;ConfigEntry.Category("ui")
 * &#64;ConfigEntry.Text("catframe:vanilla")
 * public static String activeTheme;
 * </pre>
 *
 * The type annotation and the field type have to agree; {@link ConfigManager}
 * rejects a mismatch instead of silently converting.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigEntry {

    /**
     * Comment written above the entry. One element per line.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface Comment {

        String[] value();
    }

    /**
     * Category of this entry, using dots for sub categories ({@code "ai.pathing"}).
     * Defaults to {@link IConfigScope#GENERAL}.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface Category {

        String value() default IConfigScope.GENERAL;
    }

    /**
     * Overrides the entry name, which defaults to the field name.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface Name {

        String value();
    }

    /**
     * Excludes a field from the config file. The field keeps whatever value it has.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface Ignore {}

    /**
     * Declares a {@code boolean} entry.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface Boolean {

        boolean value();
    }

    /**
     * Declares an {@code int} entry.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface Int {

        int value();
    }

    /**
     * Declares a {@code long} entry.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface Long {

        long value();
    }

    /**
     * Declares a {@code double} entry.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface Double {

        double value();
    }

    /**
     * Declares a {@code String} entry. Named {@code Text} because an annotation
     * called {@code String} would shadow {@link java.lang.String} inside its own
     * body.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface Text {

        String value();
    }

    /**
     * Declares a {@code String[]} entry.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface List {

        String[] value();
    }
}
