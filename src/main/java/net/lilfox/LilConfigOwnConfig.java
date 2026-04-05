package net.lilfox;

import net.lilfox.config.ConfigBoolean;
import net.lilfox.config.ConfigGroup;
import net.lilfox.config.ConfigHotkey;
import net.lilfox.demo.DemoConfig;
import net.lilfox.hotkey.KeyBind;
import net.lilfox.manager.IConfigProvider;

import java.util.List;

/**
 * Singleton {@link IConfigProvider} for lilConfig itself.
 *
 * <p>Persisted to {@code config/lilconfig.json}. Contains two groups:
 * <ul>
 *   <li>{@code settings} — library-level toggles and the GUI open key.</li>
 *   <li>{@code demo} — all demo entries from {@link DemoConfig}, so they are
 *       accessible through the library's own config screen.</li>
 * </ul>
 *
 * <p>Open the screen in-game with the key bound to {@link #MENU_KEY}
 * (default: Ctrl+Shift+L).
 */
public final class LilConfigOwnConfig implements IConfigProvider {

    private static final LilConfigOwnConfig INSTANCE = new LilConfigOwnConfig();

    /** Enables the vanilla keybind override feature. */
    public static final ConfigBoolean VANILLA_KEY_OVERRIDE =
            new ConfigBoolean("vanilla_key_override", false);

    /**
     * When {@code true}, the vanilla {@code KeyBindsScreen} opens normally but
     * key input is intercepted to accumulate multi-key combos (Variant C).
     * When {@code false}, a flat {@link net.lilfox.gui.LilConfigScreen} is shown instead (Variant B).
     */
    public static final ConfigBoolean VANILLA_UI_EMBED =
            new ConfigBoolean("vanilla_ui_embed", false);

    /** Hotkey that opens this config screen. */
    public static final ConfigHotkey MENU_KEY =
            new ConfigHotkey("menu_key", "LEFT_CONTROL, LEFT_SHIFT, L");

    private static final ConfigGroup SETTINGS = new ConfigGroup("settings",
            List.of(VANILLA_KEY_OVERRIDE, VANILLA_UI_EMBED, MENU_KEY));

    private static final ConfigGroup DEMO = new ConfigGroup("demo",
            List.of(DemoConfig.SHOW_HUD, DemoConfig.DEBUG_MODE, DemoConfig.MAX_ITEMS,
                    DemoConfig.SERVER_URL, DemoConfig.OVERLAY, DemoConfig.SCREENSHOT));

    private LilConfigOwnConfig() {}

    /** Returns the singleton instance. */
    public static LilConfigOwnConfig getInstance() {
        return INSTANCE;
    }

    /**
     * Returns the {@link #VANILLA_KEY_OVERRIDE} config entry.
     * Used by the vanilla keybind mixin to check whether the override is active.
     */
    public ConfigBoolean getVanillaKeyOverride() {
        return VANILLA_KEY_OVERRIDE;
    }

    /**
     * Returns the {@link #VANILLA_UI_EMBED} config entry.
     * Used by the screen redirect mixin to select Variant B or C.
     */
    public ConfigBoolean getVanillaUiEmbed() {
        return VANILLA_UI_EMBED;
    }

    @Override
    public String getModId() {
        return "lilconfig";
    }

    @Override
    public String getDisplayName() {
        return "lilConfig";
    }

    @Override
    public List<ConfigGroup> getConfigGroups() {
        return List.of(SETTINGS, DEMO);
    }

    @Override
    public KeyBind getMenuOpenKey() {
        return MENU_KEY.getKeyBind();
    }
}
