package net.lilfox.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Assigns a config field to a named section within its tab.
 *
 * <p>Section order is determined by first occurrence in field declaration order.
 * A section header separator is inserted automatically in the GUI whenever the
 * section changes. Fields without {@code @Section} are placed in a
 * {@code "misc"} section (when other fields in the same tab do have sections).
 *
 * <p>The section id is used as a translation key for the section header label.
 *
 * @see Tab
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Section {

    /** The section identifier. Used as the translation key for the section header. */
    String value();
}
