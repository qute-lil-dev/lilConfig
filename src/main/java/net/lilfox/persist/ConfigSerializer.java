package net.lilfox.persist;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.lilfox.config.ConfigGroup;
import net.lilfox.config.IConfig;
import net.lilfox.manager.IConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Handles JSON persistence for {@link IConfigProvider} instances.
 *
 * <p>Each provider is stored in {@code <configDir>/<modId>.json}.
 * The file structure mirrors the provider's group hierarchy:
 * <pre>
 * {
 *   "group_name": {
 *     "config_name": value,
 *     ...
 *   }
 * }
 * </pre>
 *
 * <p>I/O and parse failures are logged as warnings; they never throw.
 */
public final class ConfigSerializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("lilconfig");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ConfigSerializer() {}

    /**
     * Returns the filesystem path for the given provider's config file.
     *
     * @param provider the provider
     * @return {@code <configDir>/<modId>.json}
     */
    private static Path getConfigPath(IConfigProvider provider) {
        return FabricLoader.getInstance()
                .getConfigDir()
                .resolve(provider.getModId() + ".json");
    }

    /**
     * Saves all config values from the given provider to its JSON file.
     * Skips writing if no config entry is dirty. Creates parent directories if
     * necessary. Marks all entries clean after a successful write.
     * Logs a warning on I/O failure.
     *
     * @param provider the provider to save
     */
    public static void save(IConfigProvider provider) {
        boolean anyDirty = provider.getConfigGroups().stream()
                .flatMap(g -> g.getConfigs().stream())
                .anyMatch(IConfig::isDirty);
        if (!anyDirty) return;

        JsonObject root = new JsonObject();
        for (ConfigGroup group : provider.getConfigGroups()) {
            JsonObject groupObj = new JsonObject();
            for (IConfig config : group.getConfigs()) {
                groupObj.add(config.getName(), config.toJson());
            }
            root.add(group.getName(), groupObj);
        }
        Path path = getConfigPath(provider);
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(root, writer);
            }
            provider.getConfigGroups().stream()
                    .flatMap(g -> g.getConfigs().stream())
                    .forEach(IConfig::markClean);
        } catch (IOException e) {
            LOGGER.warn("Failed to save config for '{}': {}", provider.getModId(), e.getMessage());
        }
    }

    /**
     * Loads persisted values into the given provider from its JSON file.
     * Missing keys are left at their current (default) values.
     * Logs a warning on I/O or parse failure but does not throw.
     *
     * @param provider the provider to load
     */
    public static void load(IConfigProvider provider) {
        Path path = getConfigPath(provider);
        if (!Files.exists(path)) return;
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (!parsed.isJsonObject()) return;
            JsonObject root = parsed.getAsJsonObject();
            for (ConfigGroup group : provider.getConfigGroups()) {
                JsonObject groupObj = root.getAsJsonObject(group.getName());
                if (groupObj == null) continue;
                for (IConfig config : group.getConfigs()) {
                    JsonElement el = groupObj.get(config.getName());
                    if (el != null) { config.fromJson(el); config.markClean(); }
                }
            }
        } catch (IOException | JsonParseException e) {
            LOGGER.warn("Failed to load config for '{}': {}", provider.getModId(), e.getMessage());
        }
    }
}
