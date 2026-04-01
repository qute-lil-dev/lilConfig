package net.lilfox.manager;

import net.lilfox.config.ConfigGroup;
import net.lilfox.hotkey.KeyBind;

import java.util.List;

/**
 * Contract that a mod must implement to register its configuration with lilConfig.
 *
 * <p>Implement this interface and call
 * {@link LilConfigManager#register(IConfigProvider)} from your
 * {@code ClientModInitializer} to integrate with the library.
 *
 * <p>Minimal implementation example:
 * <pre>{@code
 * public class MyModConfig implements IConfigProvider {
 *     public static final ConfigBoolean ENABLED =
 *         new ConfigBoolean("enabled", true);
 *
 *     private static final ConfigGroup GENERAL =
 *         new ConfigGroup("general", List.of(ENABLED));
 *
 *     public String getModId()          { return "mymod"; }
 *     public String getDisplayName()    { return "My Mod"; }
 *     public List<ConfigGroup> getConfigGroups() { return List.of(GENERAL); }
 *     public KeyBind getMenuOpenKey()   { return KeyBind.NONE; }
 * }
 * }</pre>
 */
public interface IConfigProvider {

    /**
     * Returns the mod ID used as the JSON file name
     * ({@code <configDir>/<modId>.json}) and as a unique provider identifier.
     */
    String getModId();

    /**
     * Returns the human-readable display name shown as the config screen title.
     */
    String getDisplayName();

    /**
     * Returns the ordered list of config groups.
     * Each group is rendered as a separate tab in the GUI.
     */
    List<ConfigGroup> getConfigGroups();

    /**
     * Returns the in-game hotkey that opens this provider's config screen.
     * Return {@link KeyBind#NONE} to disable hotkey access for this provider.
     */
    KeyBind getMenuOpenKey();
}
