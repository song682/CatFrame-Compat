package decok.dfcdvadstf.catframe.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the backing file of a config class.
 * <p>
 * A class carrying this annotation must also implement {@link IConfig};
 * {@link ConfigManager#register(Class)} rejects any class that satisfies only
 * one of the two.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Configure {

    /**
     * Mod id the config belongs to.
     *
     * @return owning mod id
     */
    String modid();

    /**
     * Backing file format. Decides both the parser and the file extension.
     *
     * @return file format
     */
    ConfigType type();

    /**
     * File name without extension. Defaults to {@link #modid()}.
     *
     * @return config file name
     */
    String name() default "";
}
