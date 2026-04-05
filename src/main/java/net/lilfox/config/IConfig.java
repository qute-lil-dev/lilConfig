package net.lilfox.config;

import com.google.gson.JsonElement;
import org.jetbrains.annotations.Nullable;

/**
 * Base interface for all lilConfig configuration entries.
 *
 * <p>Each entry has a unique {@link #getName() name} used as both the JSON key
 * and the i18n base translation key. Implementations must support JSON
 * round-trips and reset-to-default semantics.
 */
public interface IConfig {

    /**
     * Returns the entry name used as the JSON key and i18n base translation key.
     */
    String getName();

    /**
     * Returns the type of this entry; used by the GUI to select the appropriate widget.
     */
    ConfigType getType();

    /**
     * Returns {@code true} if the current value differs from the default value.
     * The GUI shows the reset button when this returns {@code true}.
     */
    boolean isModified();

    /**
     * Resets the current value back to the default value.
     */
    void resetToDefault();

    /**
     * Serialises the current value to a JSON element for persistence.
     *
     * @return a JSON element representing the current value
     */
    JsonElement toJson();

    /**
     * Deserialises the value from a JSON element.
     * Malformed or incompatible input must be silently ignored,
     * leaving the current value unchanged.
     *
     * @param element the JSON element to read from
     */
    void fromJson(JsonElement element);

    /**
     * Returns {@code true} if the value has changed since the last save or load.
     */
    boolean isDirty();

    /**
     * Marks this entry as having unsaved changes.
     */
    void markDirty();

    /**
     * Clears the dirty flag (called after save or load).
     */
    void markClean();

    /**
     * Returns the translation key for the optional effect button label,
     * or {@code null} if this entry has no effect button.
     */
    @Nullable String getEffectButtonLabel();

    /**
     * Returns the callback invoked when the effect button is clicked,
     * or {@code null} if this entry has no effect button.
     */
    @Nullable Runnable getEffectAction();
}
