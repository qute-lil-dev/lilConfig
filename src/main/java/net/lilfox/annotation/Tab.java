package net.lilfox.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Assigns a config field to a named tab in the GUI.
 *
 * <p>Tab order is determined by the first occurrence of each id in field
 * declaration order. Fields without {@code @Tab} are placed in a
 * {@code "misc"} tab appended last (only when other fields do have {@code @Tab}).
 * If no field in the class carries {@code @Tab}, all fields share a single
 * implicit tab named after the mod id.
 *
 * <p>The tab id is used as a translation key for the tab label.
 * Example: {@code "general"} looks up {@code "general"} in the mod's lang file.
 *
 * @see Section
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Tab {

    /** The tab identifier. Used as the translation key for the tab label. */
    String value();
}
