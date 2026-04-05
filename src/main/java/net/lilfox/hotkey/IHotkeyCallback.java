package net.lilfox.hotkey;

/**
 * Callback invoked on the client tick thread when a hotkey fires.
 *
 * <p>Register via {@link net.lilfox.config.ConfigHotkey#withCallback(IHotkeyCallback)} or
 * {@link net.lilfox.config.ConfigHotkeyedBoolean#withCallback(IHotkeyCallback)}.
 */
@FunctionalInterface
public interface IHotkeyCallback {

    /**
     * Called when the associated hotkey fires (leading-edge, fire-on-press).
     */
    void onHotkey();
}
