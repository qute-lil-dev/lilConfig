package net.lilfox.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.Objects;

/**
 * A cyclic enum-based option selector configuration entry.
 *
 * <p>Available options are all constants of the enum type {@code E}, taken from
 * the declaring class of the default value. {@link #cycle()} advances to the
 * next constant (wrapping around). JSON representation: the {@link Enum#name()}
 * of the selected constant.
 *
 * @param <E> the enum type whose constants are the available options
 */
// @SuppressWarnings("null"): E extends Enum<E> — values are always non-null;
// false positive from JDT null analysis on free type variables.
@SuppressWarnings("null")
public class ConfigOptionList<E extends Enum<E>> extends ConfigBase<E> implements IConfigOptionList<E> {

    private final Class<E> enumClass;
    private final E[] options;

    /**
     * Creates a new option-list config entry. The available options are all enum
     * constants of the type that {@code defaultValue} belongs to.
     *
     * @param name         the unique name used as JSON key and i18n base key
     * @param defaultValue the default (and initial) selected option; must not be {@code null}
     */
    public ConfigOptionList(String name, E defaultValue) {
        super(name, Objects.requireNonNull(defaultValue, "defaultValue must not be null"));
        this.enumClass = defaultValue.getDeclaringClass();
        this.options = enumClass.getEnumConstants();
    }

    /** {@inheritDoc} */
    @Override
    public ConfigType getType() {
        return ConfigType.OPTION_LIST;
    }

    /** {@inheritDoc} */
    @Override
    public E getValue() {
        return value;
    }

    /** {@inheritDoc} */
    @Override
    public void setValue(E value) {
        this.value = value;
        notifyChanged();
    }

    /** {@inheritDoc} */
    @Override
    public E[] getOptions() {
        return options;
    }

    /** {@inheritDoc} */
    @Override
    public void cycle() {
        int idx = 0;
        for (int i = 0; i < options.length; i++) {
            if (options[i] == value) {
                idx = i;
                break;
            }
        }
        setValue(options[(idx + 1) % options.length]);
    }

    /** {@inheritDoc} */
    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(value.name());
    }

    /** {@inheritDoc} */
    @Override
    public void fromJson(JsonElement element) {
        try {
            this.value = Enum.valueOf(enumClass, element.getAsString());
        } catch (Exception ignored) {}
    }

    /**
     * {@inheritDoc}
     *
     * @return this {@code ConfigOptionList} for fluent chaining
     */
    @Override
    public ConfigOptionList<E> withOnChange(Runnable listener) {
        super.withOnChange(listener);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @return this {@code ConfigOptionList} for fluent chaining
     */
    @Override
    public ConfigOptionList<E> withEffectButton(String labelKey, Runnable action) {
        super.withEffectButton(labelKey, action);
        return this;
    }
}
