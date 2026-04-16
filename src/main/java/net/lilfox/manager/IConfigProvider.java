package net.lilfox.manager;

import net.lilfox.config.ConfigGroup;
import net.lilfox.hotkey.KeyBind;

import java.util.List;

/**
 * Internal contract used by lilConfig to represent a registered config provider.
 *
 * <p><b>Mod authors should not implement this interface directly.</b>
 * Use the annotation-based API instead:
 * <pre>{@code
 * @Config(modId = "mymod")
 * public class MyModConfig {
 *     @Tab("general")
 *     public static ConfigBoolean ENABLED = ConfigBoolean.of(true);
 * }
 *
 * // In ClientModInitializer:
 * ConfigManager.getInstance().register(MyModConfig.class);
 * }</pre>
 *
 * <p>This interface remains public only because internal classes in sibling packages
 * (e.g. {@code net.lilfox.vanilla}, {@code net.lilfox}) still need to reference it.
 * It may become package-private in a future release.
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
