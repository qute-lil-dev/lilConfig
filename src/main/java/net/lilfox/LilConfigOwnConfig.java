package net.lilfox;

import net.lilfox.annotation.DevTab;
import net.lilfox.annotation.Hotkeyed;
import net.lilfox.annotation.Config;
import net.lilfox.annotation.MenuKey;
import net.lilfox.annotation.Section;
import net.lilfox.annotation.Tab;
import net.lilfox.config.ConfigBoolean;
import net.lilfox.config.ConfigDouble;
import net.lilfox.config.ConfigHotkey;
import net.lilfox.config.ConfigInteger;
import net.lilfox.config.ConfigOptionList;
import net.lilfox.config.ConfigString;

/**
 * lilConfig's own settings, registered via the annotation-based API.
 *
 * <p>Persisted to {@code config/lilconfig.json}. Contains one tab:
 * <ul>
 *   <li>{@code settings} — library-level toggles and the GUI open key.</li>
 * </ul>
 *
 * <p>In development environments two additional tabs are shown ({@code general}
 * and {@code hotkeys}) that exercise every widget type available in lilConfig.
 * They are hidden in production via {@link DevTab}.
 *
 * <p>Open this config screen in-game with the key bound to {@link #menuKey}
 * (default: Ctrl+Shift+L).
 */
@Config(modId = "lilconfig", displayName = "lilConfig")
public final class LilConfigOwnConfig {

    /** Enables the vanilla keybind override feature. */
    @Tab("settings")
    public static ConfigBoolean vanillaKeyOverride = ConfigBoolean.of(false);

    /**
     * When {@code true}, the vanilla {@code KeyBindsScreen} opens normally but
     * key input is intercepted to accumulate multi-key combos (Variant C).
     * When {@code false}, a flat {@link net.lilfox.gui.ConfigScreen} is shown instead (Variant B).
     */
    @Tab("settings")
    public static ConfigBoolean vanillaUiEmbed = ConfigBoolean.of(false);

    /** Hotkey that opens this config screen. */
    @Tab("settings")
    @MenuKey
    public static ConfigHotkey menuKey = ConfigHotkey.of("LEFT_CONTROL, LEFT_SHIFT, L");

    /** Render quality preset options (dev demo). */
    public enum Quality { LOW, MEDIUM, HIGH, ULTRA }

    @DevTab()
    @Section("display")
    public static ConfigBoolean showHud = ConfigBoolean.of(true);

    @DevTab()
    @Section("display")
    public static ConfigBoolean debugMode = ConfigBoolean.of(false);

    @DevTab()
    @Section("values")
    public static ConfigInteger maxItems = ConfigInteger.of(64, 1, 256);

    @DevTab()
    @Section("values")
    public static ConfigDouble renderScale = ConfigDouble.of(1.0, 0.1, 4.0);

    @DevTab()
    @Section("values")
    public static ConfigOptionList<Quality> quality = ConfigOptionList.of(Quality.MEDIUM);

    @DevTab()
    @Section("network")
    public static ConfigString serverUrl = ConfigString.of("localhost");

    @DevTab()
    @Hotkeyed(defaultKey = "LEFT_CONTROL, O")
    public static ConfigBoolean overlay = ConfigBoolean.of(false);

    @DevTab()
    public static ConfigHotkey screenshot = ConfigHotkey.of("F2");

    private LilConfigOwnConfig() {}
}
