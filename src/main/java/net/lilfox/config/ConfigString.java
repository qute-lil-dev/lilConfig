package net.lilfox.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * A string configuration entry backed by a text field in the GUI.
 *
 * <p>JSON representation: a single {@code string} primitive.
 */
public class ConfigString extends ConfigBase<String> {

    /**
     * Creates a new string config entry.
     *
     * @param name         the unique name used as JSON key and i18n base key
     * @param defaultValue the default value
     */
    public ConfigString(String name, String defaultValue) {
        super(name, defaultValue != null ? defaultValue : "");
    }

    @Override
    public ConfigType getType() {
        return ConfigType.STRING;
    }

    /**
     * Returns the current string value.
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the current string value. A {@code null} argument is treated as
     * an empty string.
     *
     * @param value the new value
     */
    public void setValue(String value) {
        this.value = value != null ? value : "";
    }

    /**
     * Returns the default string value.
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(value);
    }

    @Override
    public void fromJson(JsonElement element) {
        try {
            this.value = element.getAsString();
        } catch (Exception ignored) {}
    }

    /**
     * {@inheritDoc}
     *
     * @return this {@code ConfigString} for fluent chaining
     */
    @Override
    public ConfigString withEffectButton(String labelKey, Runnable action) {
        super.withEffectButton(labelKey, action);
        return this;
    }
}
