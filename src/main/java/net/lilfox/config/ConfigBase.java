package net.lilfox.config;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Abstract base class for all lilConfig configuration entries.
 *
 * <p>Manages the entry name, the current and default values, and the optional
 * effect button attachment. Subclasses provide type-specific serialisation and
 * may override {@link #isModified()} and {@link #resetToDefault()} when they
 * hold more than one logical value (e.g. {@code ConfigHotkeyedBoolean}).
 *
 * @param <T> the value type stored by this entry
 */
public abstract class ConfigBase<T> implements IConfig {

    private final String name;

    /** The immutable default value; used for reset and modification detection. */
    protected final T defaultValue;

    /** The current mutable value. */
    protected T value;

    @Nullable private String effectButtonLabel;
    @Nullable private Runnable effectAction;
    private boolean dirty = false;
    @Nullable private Runnable onChange;

    /**
     * Creates a new config entry with the given name and default value.
     * The current value is initialised to {@code defaultValue}.
     *
     * @param name         the unique name used as JSON key and i18n base key
     * @param defaultValue the default (and initial) value
     */
    protected ConfigBase(String name, T defaultValue) {
        this.name = name;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return name;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isModified() {
        return !Objects.equals(value, defaultValue);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDirty()  { return dirty; }

    /** {@inheritDoc} */
    @Override
    public void markDirty()   { dirty = true; }

    /** {@inheritDoc} */
    @Override
    public void markClean()   { dirty = false; }

    /**
     * Registers a listener called each time the value changes programmatically.
     * Not invoked during {@link #fromJson} (load from disk).
     *
     * @param listener the callback; replaces any previously set listener
     * @return this instance for fluent chaining
     */
    public ConfigBase<T> withOnChange(Runnable listener) {
        this.onChange = listener;
        return this;
    }

    /**
     * Marks this entry dirty and notifies the registered onChange listener, if any.
     * Called by {@code setValue} in subclasses. Never called from {@code fromJson}.
     */
    protected void notifyChanged() {
        markDirty();
        if (onChange != null) onChange.run();
    }

    /** {@inheritDoc} */
    @Override
    public void resetToDefault() {
        this.value = defaultValue;
        markDirty();
    }

    /** {@inheritDoc} */
    @Override
    @Nullable
    public String getEffectButtonLabel() {
        return effectButtonLabel;
    }

    /** {@inheritDoc} */
    @Override
    @Nullable
    public Runnable getEffectAction() {
        return effectAction;
    }

    /**
     * Attaches an effect button to this config entry.
     *
     * <p>The effect button is rendered in the GUI row next to the main widget
     * and invokes {@code action} on the render thread when clicked.
     * Returns {@code ConfigBase<T>}; subclasses override this method with a
     * covariant return type for fluent chaining.
     *
     * @param labelKey the translation key for the button label
     * @param action   the action to invoke on click
     * @return this instance
     */
    public ConfigBase<T> withEffectButton(String labelKey, Runnable action) {
        this.effectButtonLabel = labelKey;
        this.effectAction = action;
        return this;
    }
}
