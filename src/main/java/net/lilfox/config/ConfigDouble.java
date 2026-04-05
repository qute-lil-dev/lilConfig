package net.lilfox.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * A double-precision floating-point configuration entry with optional bounds.
 *
 * <p>Values are automatically clamped to {@code [min, max]} on every write,
 * including during deserialisation. Pass {@link Double#NEGATIVE_INFINITY} and
 * {@link Double#POSITIVE_INFINITY} (the defaults) to leave the value unbounded.
 * JSON representation: a single {@code number} primitive.
 */
public class ConfigDouble extends ConfigBase<Double> {

    private final double min;
    private final double max;

    /**
     * Creates a new double config entry with explicit bounds.
     * {@code defaultValue} is clamped to {@code [min, max]}.
     *
     * @param name         the unique name used as JSON key and i18n base key
     * @param defaultValue the default value (will be clamped)
     * @param min          the inclusive lower bound; use {@link Double#NEGATIVE_INFINITY} for none
     * @param max          the inclusive upper bound; use {@link Double#POSITIVE_INFINITY} for none
     * @throws IllegalArgumentException if {@code min > max}
     */
    public ConfigDouble(String name, double defaultValue, double min, double max) {
        super(name, Math.clamp(defaultValue, min, max));
        if (min > max) {
            throw new IllegalArgumentException("min > max for config: " + name);
        }
        this.min = min;
        this.max = max;
    }

    /**
     * Creates a new unbounded double config entry.
     *
     * @param name         the unique name used as JSON key and i18n base key
     * @param defaultValue the default value
     */
    public ConfigDouble(String name, double defaultValue) {
        this(name, defaultValue, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    /** {@inheritDoc} */
    @Override
    public ConfigType getType() {
        return ConfigType.DOUBLE;
    }

    /**
     * Returns the current double value.
     */
    public double getValue() {
        return value;
    }

    /**
     * Sets the current value, clamping it to {@code [min, max]}.
     *
     * @param value the new value before clamping
     */
    public void setValue(double value) {
        this.value = Math.clamp(value, min, max);
        notifyChanged();
    }

    /**
     * Returns the default double value.
     */
    public double getDefaultValue() {
        return defaultValue;
    }

    /**
     * Returns the inclusive lower bound, or {@link Double#NEGATIVE_INFINITY} if unbounded.
     */
    public double getMin() {
        return min;
    }

    /**
     * Returns the inclusive upper bound, or {@link Double#POSITIVE_INFINITY} if unbounded.
     */
    public double getMax() {
        return max;
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
            this.value = Math.clamp(element.getAsDouble(), min, max);
        } catch (Exception ignored) {}
    }

    /**
     * {@inheritDoc}
     *
     * @return this {@code ConfigDouble} for fluent chaining
     */
    @Override
    public ConfigDouble withOnChange(Runnable listener) {
        super.withOnChange(listener);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @return this {@code ConfigDouble} for fluent chaining
     */
    @Override
    public ConfigDouble withEffectButton(String labelKey, Runnable action) {
        super.withEffectButton(labelKey, action);
        return this;
    }
}
