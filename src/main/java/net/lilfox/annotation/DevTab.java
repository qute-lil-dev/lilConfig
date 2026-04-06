package net.lilfox.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Assigns a config field to a named tab that is shown only in a development
 * environment ({@code FabricLoader.getInstance().isDevelopmentEnvironment()}).
 *
 * <p>In production the field is skipped entirely by the annotation scanner;
 * in development it behaves exactly like {@link Tab} with the given value.
 *
 * <p>The default tab id is {@code "dev"}, so a bare {@code @DevTab} places
 * the field in a single dev-only tab. Use an explicit value when you need
 * multiple dev-only tabs:
 * <pre>{@code
 * @DevTab("general")  public static ConfigBoolean SHOW_HUD = ...;
 * @DevTab("hotkeys")  public static ConfigHotkey  SCREENSHOT = ...;
 * }</pre>
 *
 * @see Tab
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface DevTab {

    /** The tab identifier. Defaults to {@code "dev"}. */
    String value() default "dev";
}
