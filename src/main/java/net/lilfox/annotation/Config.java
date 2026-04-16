package net.lilfox.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as the config holder for a mod and registers it with lilConfig.
 *
 * <p>Apply to a class that holds {@code public static} config fields.
 * Register with:
 * <pre>{@code
 * ConfigManager.getInstance().register(MyModConfig.class);
 * }</pre>
 *
 * <p>To enable an in-game hotkey that opens the config screen, annotate a
 * {@code public static ConfigHotkey} field in the class with {@link MenuKey}.
 *
 * @see net.lilfox.annotation.Tab
 * @see net.lilfox.annotation.Section
 * @see net.lilfox.annotation.Hotkeyed
 * @see net.lilfox.annotation.MenuKey
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Config {

    /** The mod ID used as the JSON file name and i18n key prefix. */
    String modId();

    /**
     * The human-readable display name shown as the config screen title.
     * Defaults to {@link #modId()} if empty.
     */
    String displayName() default "";
}
