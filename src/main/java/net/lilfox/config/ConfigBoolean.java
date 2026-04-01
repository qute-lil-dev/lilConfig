package net.lilfox.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * A boolean configuration entry represented as a toggle button in the GUI.
 *
 * <p>JSON representation: a single {@code boolean} primitive.
 */
public class ConfigBoolean extends ConfigBase<Boolean> implements IConfigBoolean {

    /**
     * Creates a new boolean config entry.
     *
     * @param name         the unique name used as JSON key and i18n base key
     * @param defaultValue the default value
     */
    public ConfigBoolean(String name, boolean defaultValue) {
        super(name, defaultValue);
    }

    /** {@inheritDoc} */
    @Override
    public ConfigType getType() {
        return ConfigType.BOOLEAN;
    }

    /** {@inheritDoc} */
    @Override
    public boolean getValue() {
        return value;
    }

    /** {@inheritDoc} */
    @Override
    public void setValue(boolean value) {
        this.value = value;
    }

    /** {@inheritDoc} */
    @Override
    public boolean getDefaultValue() {
        return defaultValue;
    }

    /** {@inheritDoc} */
    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(value);
    }

    /** {@inheritDoc} */
    @Override
    public void fromJson(JsonElement element) {
        try {
            this.value = element.getAsBoolean();
        } catch (Exception ignored) {}
    }

    /**
     * {@inheritDoc}
     *
     * @return this {@code ConfigBoolean} for fluent chaining
     */
    @Override
    public ConfigBoolean withEffectButton(String labelKey, Runnable action) {
        super.withEffectButton(labelKey, action);
        return this;
    }
}
