package net.lilfox.config;

import java.util.List;

/**
 * A named, ordered collection of {@link IConfig} entries that maps to a single
 * tab in the lilConfig GUI.
 *
 * <p>The group name is used as the JSON object key for serialisation and as
 * the translation key for the tab label displayed in the GUI.
 */
public class ConfigGroup {

    private final String name;
    private final List<IConfig> configs;

    /**
     * Creates a new config group.
     *
     * @param name    the group name used as JSON key and i18n tab label key
     * @param configs the ordered list of config entries belonging to this group
     */
    public ConfigGroup(String name, List<IConfig> configs) {
        this.name = name;
        this.configs = List.copyOf(configs);
    }

    /**
     * Returns the group name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns an unmodifiable ordered list of the config entries in this group.
     */
    public List<IConfig> getConfigs() {
        return configs;
    }
}
