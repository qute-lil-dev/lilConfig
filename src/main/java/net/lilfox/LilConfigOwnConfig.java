package net.lilfox;

import net.lilfox.annotation.LilConfigMod;
import net.lilfox.annotation.MenuKey;
import net.lilfox.annotation.Tab;
import net.lilfox.config.ConfigBoolean;
import net.lilfox.config.ConfigHotkey;

/**
 * lilConfig's own settings, registered via the annotation-based API.
 *
 * <p>Persisted to {@code config/lilconfig.json}. Contains one tab:
 * <ul>
 *   <li>{@code settings} — library-level toggles and the GUI open key.</li>
 * </ul>
 *
 * <p>Open this config screen in-game with the key bound to {@link #MENU_KEY}
 * (default: Ctrl+Shift+L).
 */
@LilConfigMod(modId = "lilconfig", displayName = "lilConfig")
public final class LilConfigOwnConfig {

    /** Enables the vanilla keybind override feature. */
    @Tab("settings")
    public static ConfigBoolean VANILLA_KEY_OVERRIDE = ConfigBoolean.of(false);

    /**
     * When {@code true}, the vanilla {@code KeyBindsScreen} opens normally but
     * key input is intercepted to accumulate multi-key combos (Variant C).
     * When {@code false}, a flat {@link net.lilfox.gui.LilConfigScreen} is shown instead (Variant B).
     */
    @Tab("settings")
    public static ConfigBoolean VANILLA_UI_EMBED = ConfigBoolean.of(false);

    /** Hotkey that opens this config screen. */
    @Tab("settings")
    @MenuKey
    public static ConfigHotkey MENU_KEY = ConfigHotkey.of("LEFT_CONTROL, LEFT_SHIFT, L");

    private LilConfigOwnConfig() {}
}
