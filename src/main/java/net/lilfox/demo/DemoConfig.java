package net.lilfox.demo;

import net.lilfox.annotation.Hotkeyed;
import net.lilfox.annotation.LilConfigMod;
import net.lilfox.annotation.MenuKey;
import net.lilfox.annotation.Section;
import net.lilfox.annotation.Tab;
import net.lilfox.config.*;

/**
 * Demo config that showcases all widget types available in lilConfig.
 *
 * <p>Demonstrates the annotation-based API:
 * <ul>
 *   <li>{@link LilConfigMod} on the class — registers mod ID and display name.</li>
 *   <li>{@link Tab} on each field — assigns it to a named GUI tab.</li>
 *   <li>{@link Section} on a field — inserts a section header for that group.</li>
 *   <li>{@link Hotkeyed} on a {@link ConfigBoolean} — adds a hotkey toggle; only
 *       the boolean value is accessible from the field type.</li>
 *   <li>{@link MenuKey} on a {@link ConfigHotkey} — designates the screen open key.</li>
 *   <li>Factory methods ({@code ConfigBoolean.of(...)}, etc.) — entry names are
 *       inferred from field names at registration time.</li>
 * </ul>
 *
 * <p>Register from {@code ClientModInitializer}:
 * <pre>{@code
 * LilConfigManager.getInstance().register(DemoConfig.class);
 * }</pre>
 */
@LilConfigMod(modId = "lilconfig_demo", displayName = "lilConfig Demo")
public final class DemoConfig {

    /** Render quality preset options. */
    public enum Quality { LOW, MEDIUM, HIGH, ULTRA }

    // ---- Tab: general ----

    @Tab("general")
    @Section("display")
    public static ConfigBoolean SHOW_HUD = ConfigBoolean.of(true);

    @Tab("general")
    @Section("display")
    public static ConfigBoolean DEBUG_MODE = ConfigBoolean.of(false);

    @Tab("general")
    @Section("values")
    public static ConfigInteger MAX_ITEMS = ConfigInteger.of(64, 1, 256);

    @Tab("general")
    @Section("values")
    public static ConfigDouble RENDER_SCALE = ConfigDouble.of(1.0, 0.1, 4.0);

    @Tab("general")
    @Section("values")
    public static ConfigOptionList<Quality> QUALITY = ConfigOptionList.of(Quality.MEDIUM);

    @Tab("general")
    @Section("network")
    public static ConfigString SERVER_URL = ConfigString.of("localhost");

    // ---- Tab: hotkeys ----

    @Tab("hotkeys")
    @Hotkeyed(defaultKey = "LEFT_CONTROL, O")
    public static ConfigBoolean OVERLAY = ConfigBoolean.of(false);
    // In mod code: OVERLAY.getValue() — boolean only; hotkey managed by the GUI.

    @Tab("hotkeys")
    public static ConfigHotkey SCREENSHOT = ConfigHotkey.of("F2");

    @Tab("hotkeys")
    @MenuKey
    public static ConfigHotkey OPEN = ConfigHotkey.of("LEFT_CONTROL, LEFT_SHIFT, D");

    private DemoConfig() {}
}
