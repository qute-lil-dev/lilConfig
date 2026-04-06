package net.lilfox.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Converts a {@code ConfigBoolean} field into a hotkeyed boolean at registration time.
 *
 * <p>The annotation scanner replaces the field value with a
 * {@link net.lilfox.config.ConfigHotkeyedBoolean} instance. The field type remains
 * {@code ConfigBoolean}, so only the boolean value is accessible from mod code.
 * The hotkey is configured by the player in the GUI and stored in the JSON file,
 * but is not exposed through the field type.
 *
 * <p>Example:
 * <pre>{@code
 * @Tab("hotkeys")
 * @Hotkeyed(defaultKey = "LEFT_CTRL, O")
 * public static ConfigBoolean OVERLAY = ConfigBoolean.of(false);
 *
 * // In mod code:
 * if (MyModConfig.OVERLAY.getValue()) { ... }  // boolean only
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Hotkeyed {

    /**
     * The default key binding as a comma-separated list of GLFW key names,
     * e.g. {@code "LEFT_CONTROL, O"}. Empty string means unbound by default.
     */
    String defaultKey() default "";
}
