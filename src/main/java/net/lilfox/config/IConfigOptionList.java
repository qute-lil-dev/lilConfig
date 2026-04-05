package net.lilfox.config;

/**
 * Interface for cyclic enum-based option selectors.
 *
 * <p>The GUI renders a button showing the current option's name. Clicking
 * the button calls {@link #cycle()} to advance to the next option.
 *
 * @param <E> the enum type whose constants are the available options
 */
public interface IConfigOptionList<E extends Enum<E>> extends IConfig {

    /**
     * Returns the currently selected option.
     */
    E getValue();

    /**
     * Sets the current option.
     *
     * @param value the new option value; must be one of {@link #getOptions()}
     */
    void setValue(E value);

    /**
     * Returns all available options in declaration order.
     *
     * @return array of all enum constants for this option list
     */
    E[] getOptions();

    /**
     * Advances the selected option to the next one, wrapping around after the last.
     */
    void cycle();
}
