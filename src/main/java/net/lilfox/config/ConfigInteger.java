package net.lilfox.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * An integer configuration entry with inclusive minimum and maximum bounds.
 *
 * <p>Values are automatically clamped to {@code [minValue, maxValue]} on every
 * write, including during deserialisation. JSON representation: a single
 * {@code number} primitive.
 */
public class ConfigInteger extends ConfigBase<Integer> {

    private final int minValue;
    private final int maxValue;

    /**
     * Creates an integer config entry whose name will be injected by the annotation scanner.
     *
     * @param defaultValue the default value (will be clamped)
     * @param minValue     the inclusive lower bound
     * @param maxValue     the inclusive upper bound
     * @return a new unnamed entry
     */
    public static ConfigInteger of(int defaultValue, int minValue, int maxValue) {
        return new ConfigInteger("", defaultValue, minValue, maxValue);
    }

    /**
     * Creates a new integer config entry.
     * {@code defaultValue} is clamped to {@code [minValue, maxValue]}.
     *
     * @param name         the unique name used as JSON key and i18n base key
     * @param defaultValue the default value (will be clamped)
     * @param minValue     the inclusive lower bound
     * @param maxValue     the inclusive upper bound
     * @throws IllegalArgumentException if {@code minValue > maxValue}
     */
    public ConfigInteger(String name, int defaultValue, int minValue, int maxValue) {
        super(name, Math.clamp(defaultValue, minValue, maxValue));
        if (minValue > maxValue) {
            throw new IllegalArgumentException("minValue > maxValue for config: " + name);
        }
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    /** {@inheritDoc} */
    @Override
    public ConfigType getType() {
        return ConfigType.INTEGER;
    }

    /**
     * Returns the current integer value.
     */
    public int getValue() {
        return value;
    }

    /**
     * Sets the current value, clamping it to {@code [minValue, maxValue]}.
     *
     * @param value the new value before clamping
     */
    public void setValue(int value) {
        this.value = Math.clamp(value, minValue, maxValue);
        notifyChanged();
    }

    /**
     * Returns the default integer value.
     */
    public int getDefaultValue() {
        return defaultValue;
    }

    /**
     * Returns the inclusive lower bound.
     */
    public int getMinValue() {
        return minValue;
    }

    /**
     * Returns the inclusive upper bound.
     */
    public int getMaxValue() {
        return maxValue;
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
            this.value = Math.clamp(element.getAsInt(), minValue, maxValue);
        } catch (Exception ignored) {}
    }

    /**
     * {@inheritDoc}
     *
     * @return this {@code ConfigInteger} for fluent chaining
     */
    @Override
    public ConfigInteger withOnChange(Runnable listener) {
        super.withOnChange(listener);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @return this {@code ConfigInteger} for fluent chaining
     */
    @Override
    public ConfigInteger withEffectButton(String labelKey, Runnable action) {
        super.withEffectButton(labelKey, action);
        return this;
    }
}
