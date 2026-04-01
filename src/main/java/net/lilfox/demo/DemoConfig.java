package net.lilfox.demo;

import net.lilfox.config.ConfigBoolean;
import net.lilfox.config.ConfigHotkey;
import net.lilfox.config.ConfigHotkeyedBoolean;
import net.lilfox.config.ConfigInteger;
import net.lilfox.config.ConfigString;

/**
 * Static field holder for demo config entries; referenced by
 * {@code LilConfigOwnConfig}'s demo group.
 */
public final class DemoConfig {

    public static final ConfigBoolean         SHOW_HUD   = new ConfigBoolean("show_hud", true);
    public static final ConfigBoolean         DEBUG_MODE = new ConfigBoolean("debug_mode", false);
    public static final ConfigHotkeyedBoolean OVERLAY    = new ConfigHotkeyedBoolean(
            "overlay", false, "LEFT_CONTROL, O");
    public static final ConfigHotkey          SCREENSHOT = new ConfigHotkey("screenshot", "F2");
    public static final ConfigInteger         MAX_ITEMS  = new ConfigInteger("max_items", 64, 1, 256);
    public static final ConfigString          SERVER_URL = new ConfigString("server_url", "localhost");

    private DemoConfig() {}
}
