package net.lilfox.demo;

import net.lilfox.config.ConfigBoolean;
import net.lilfox.config.ConfigDouble;
import net.lilfox.config.ConfigHotkey;
import net.lilfox.config.ConfigHotkeyedBoolean;
import net.lilfox.config.ConfigInteger;
import net.lilfox.config.ConfigOptionList;
import net.lilfox.config.ConfigSeparator;
import net.lilfox.config.ConfigString;

/**
 * Static field holder for demo config entries; referenced by
 * {@code LilConfigOwnConfig}'s demo group.
 *
 * <p>Contains one entry per {@link net.lilfox.config.ConfigType} to serve as
 * a live showcase of every widget the library can render.
 */
public final class DemoConfig {

    /** Render quality preset options. */
    public enum Quality { LOW, MEDIUM, HIGH, ULTRA }

    // --- basic ---
    public static final ConfigBoolean         SHOW_HUD      = new ConfigBoolean("show_hud", true);
    public static final ConfigBoolean         DEBUG_MODE    = new ConfigBoolean("debug_mode", false);

    // --- numeric / option ---
    public static final ConfigSeparator       SEP_VALUES    = new ConfigSeparator("demo.separator.values");
    public static final ConfigInteger         MAX_ITEMS     = new ConfigInteger("max_items", 64, 1, 256);
    public static final ConfigDouble          RENDER_SCALE  = new ConfigDouble("render_scale", 1.0, 0.1, 4.0);
    public static final ConfigOptionList<Quality> QUALITY   = new ConfigOptionList<>("quality", Quality.MEDIUM);

    // --- text ---
    public static final ConfigSeparator       SEP_NETWORK   = new ConfigSeparator("demo.separator.network");
    public static final ConfigString          SERVER_URL    = new ConfigString("server_url", "localhost");

    // --- hotkeys ---
    public static final ConfigSeparator       SEP_HOTKEYS   = new ConfigSeparator("demo.separator.hotkeys");
    public static final ConfigHotkeyedBoolean OVERLAY       = new ConfigHotkeyedBoolean(
            "overlay", false, "LEFT_CONTROL, O");
    public static final ConfigHotkey          SCREENSHOT    = new ConfigHotkey("screenshot", "F2");

    private DemoConfig() {}
}
