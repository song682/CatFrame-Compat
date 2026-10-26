package decok.dfcdvadstf.catframe.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the backing file of a config class.
 * <p>
 * There is no interface to implement. A class is a config class when it carries
 * this annotation; each config entry is a {@link ConfigEntry} annotated
 * {@code static}, non-{@code final} field of that class, and
 * {@link ConfigManager#register(Class)} reflects over those fields and fills them
 * in:
 *
 * <pre>
 * &#64;Configure(modid = Tags.MODID, type = ConfigType.FORGE_CFG)
 * public final class CompatConfig {
 *
 *     &#64;ConfigEntry.Comment("Enable the ItemPhysic compatibility layer")
 *     &#64;ConfigEntry.Boolean(false)
 *     public static boolean itemPhysicCompat;
 * }
 * </pre>
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
}
