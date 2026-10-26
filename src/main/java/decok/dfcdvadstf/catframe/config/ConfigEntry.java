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
     * <p>
     * {@link #min()} and {@link #max()} are optional. A value read from the file
     * outside that range is clamped instead of being accepted, so a hand edited
     * number cannot reach code that assumes a sane bound.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface Int {

        int value();

        int min() default Integer.MIN_VALUE;

        int max() default Integer.MAX_VALUE;
    }

    /**
     * Declares a {@code long} entry.
     * <p>
     * {@link #min()} and {@link #max()} behave like the ones on {@link Int}. They
     * are spelled with a fully qualified {@code java.lang.Long} because the
     * annotation name would otherwise shadow it.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface Long {

        long value();

        long min() default java.lang.Long.MIN_VALUE;

        long max() default java.lang.Long.MAX_VALUE;
    }

    /**
     * Declares a {@code double} entry.
     * <p>
     * {@link #min()} and {@link #max()} behave like the ones on {@link Int}, with
     * {@code NaN} treated as out of range and replaced by {@link #value()}.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface Double {

        double value();

        double min() default -java.lang.Double.MAX_VALUE;

        double max() default java.lang.Double.MAX_VALUE;
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
