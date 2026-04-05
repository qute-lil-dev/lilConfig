package net.lilfox.hotkey;

/**
 * Controls when a hotkey is active.
 *
 * <ul>
 *   <li>{@link #IN_GAME} — only while no screen is open (default)</li>
 *   <li>{@link #GUI_OPEN} — only while any screen is open</li>
 *   <li>{@link #ALWAYS} — regardless of screen state</li>
 * </ul>
 */
public enum HotkeyContext { IN_GAME, GUI_OPEN, ALWAYS }
