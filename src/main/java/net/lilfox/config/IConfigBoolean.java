package net.lilfox.config;

/**
 * Extension of {@link IConfig} for entries that hold a boolean value.
 */
public interface IConfigBoolean extends IConfig {

    /**
     * Returns the current boolean value.
     */
    boolean getValue();

    /**
     * Sets the current boolean value.
     *
     * @param value the new value
     */
    void setValue(boolean value);

    /**
     * Returns the default boolean value.
     */
    boolean getDefaultValue();
}
