package net.lilfox.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@link net.lilfox.config.ConfigHotkey} field as the hotkey that opens
 * this mod's config screen.
 *
 * <p>Apply to exactly one {@code public static ConfigHotkey} field in a
 * {@link Config}-annotated class. The current value of that field is read
 * every tick, so player rebinds take effect immediately.
 *
 * <p>If no field carries this annotation, the config screen is accessible only
 * via Mod Menu (no in-game hotkey).
 *
 * <pre>{@code
 * @Tab("settings")
 * @MenuKey
 * public static ConfigHotkey MENU_KEY = ConfigHotkey.of("LEFT_CONTROL, LEFT_SHIFT, L");
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface MenuKey {
}
