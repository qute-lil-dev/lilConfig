package net.lilfox.config;

/**
 * Enumerates all supported configuration value types.
 * The GUI uses this to determine which widget to render for a given config entry.
 */
public enum ConfigType {
    /** A simple boolean toggle. */
    BOOLEAN,
    /** A boolean toggle combined with a configurable hotkey that activates it. */
    HOTKEYED_BOOLEAN,
    /** A standalone configurable hotkey with no associated boolean state. */
    HOTKEY,
    /** An integer value with optional minimum and maximum bounds. */
    INTEGER,
    /** A free-form string value backed by a text field. */
    STRING
}
