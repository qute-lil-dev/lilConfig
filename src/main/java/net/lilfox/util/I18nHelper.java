package net.lilfox.util;

import net.lilfox.config.IConfig;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Utility for resolving lilConfig i18n keys with raw-identifier fallbacks.
 *
 * <p>For a config entry with mod id {@code "mymod"} and name {@code "showHud"}
 * the key tiers are:
 * <ul>
 *   <li>{@code mymod.settings.showHud} — row label shown in the config list.
 *       Fallback: the raw field name (e.g. {@code "showHud"}).</li>
 *   <li>{@code mymod.settings.showHud.nice} — extended display name used in
 *       in-game notifications when a hotkey fires. Returns {@code null} when
 *       the key is absent (caller decides whether to show anything).</li>
 *   <li>{@code mymod.settings.showHud.desc} — tooltip body. Returns {@code null} when
 *       absent (tooltip not shown).</li>
 * </ul>
 *
 * <p>Tab labels use {@code mymod.tabs.tabId}; section headers use
 * {@code mymod.sections.sectionId}. Both fall back to the raw identifier string
 * when the key is absent.
 *
 * <p>Entries registered without a mod id (empty string) fall back to using
 * the entry name directly as the translation key, preserving backward
 * compatibility with the vanilla keybind provider and other internal uses.
 */
public final class I18nHelper {

    private I18nHelper() {}

    /**
     * Returns the row label {@link Component} for {@code config}.
     * Looks up {@code modid.settings.name}; falls back to the raw entry name.
     *
     * @param config the config entry
     * @return a non-null label component
     */
    public static Component label(IConfig config) {
        String key = labelKey(config);
        if (!config.getModId().isEmpty() && !I18n.exists(key)) {
            return Component.literal(config.getName());
        }
        return Component.translatable(key);
    }

    /**
     * Returns the translation key used for the row label.
     * When modId is empty, the bare entry name is used (legacy/internal behaviour).
     */
    public static String labelKey(IConfig config) {
        String modId = config.getModId();
        return modId.isEmpty() ? config.getName() : modId + ".settings." + config.getName();
    }

    /**
     * Returns the "nice name" string if the {@code .nice} key exists in the current
     * language, otherwise returns {@code null}.
     *
     * @param config the config entry
     * @return the nice name, or {@code null}
     */
    public static @Nullable String niceName(IConfig config) {
        String modId = config.getModId();
        if (modId.isEmpty()) return null;
        String key = modId + ".settings." + config.getName() + ".nice";
        return I18n.exists(key) ? I18n.get(key) : null;
    }

    /**
     * Returns a {@link Component} for the tooltip body.
     * Uses the translated {@code modId.settings.name.desc} key when available;
     * otherwise returns a literal component showing the key itself (useful
     * for developers who need to know which key to localise).
     * Returns {@code null} only when {@code modId} is empty.
     *
     * @param config the config entry
     * @return a tooltip component, or {@code null} if modId is empty
     */
    public static @Nullable Component desc(IConfig config) {
        String modId = config.getModId();
        if (modId.isEmpty()) return null;
        String key = modId + ".settings." + config.getName() + ".desc";
        return I18n.exists(key) ? Component.translatable(key) : Component.literal(key);
    }

    /**
     * Returns a {@link Component} for an input field tooltip (INTEGER, DOUBLE, STRING types).
     * Uses the translated {@code modId.settings.name.inputDesc} key when available;
     * returns {@code null} when the key is absent (tooltip not shown).
     *
     * @param config the config entry
     * @return a tooltip component, or {@code null} if absent or modId is empty
     */
    public static @Nullable Component inputDesc(IConfig config) {
        String modId = config.getModId();
        if (modId.isEmpty()) return null;
        String key = modId + ".settings." + config.getName() + ".inputDesc";
        return I18n.exists(key) ? Component.translatable(key) : null;
    }

    /**
     * Returns a {@link Component} for a mod-specific hotkey button tooltip.
     * Uses the translated {@code modId.settings.name.hotkeyDesc} key when available;
     * returns {@code null} when the key is absent (tooltip not shown).
     *
     * @param config the config entry
     * @return a tooltip component, or {@code null} if absent or modId is empty
     */
    public static @Nullable Component hotkeyDesc(IConfig config) {
        String modId = config.getModId();
        if (modId.isEmpty()) return null;
        String key = modId + ".settings." + config.getName() + ".hotkeyDesc";
        return I18n.exists(key) ? Component.translatable(key) : null;
    }

    /**
     * Returns the tab label {@link Component} for the given mod and tab identifier.
     * Looks up {@code modId.tabs.tabId}; falls back to the raw tab id string.
     *
     * @param modId the owning mod id
     * @param tabId the tab identifier
     * @return a non-null label component
     */
    public static Component tabLabel(String modId, String tabId) {
        if (!modId.isEmpty()) {
            String key = modId + ".tabs." + tabId;
            if (I18n.exists(key)) return Component.translatable(key);
        }
        return Component.literal(tabId);
    }

    /**
     * Returns the section header {@link Component} for {@code config}.
     * Looks up {@code modId.sections.name}; falls back to the raw section name string.
     *
     * @param config the separator config entry
     * @return a non-null label component (unstyled; caller applies formatting)
     */
    public static Component sectionLabel(IConfig config) {
        String modId = config.getModId();
        if (!modId.isEmpty()) {
            String key = modId + ".sections." + config.getName();
            if (I18n.exists(key)) return Component.translatable(key);
            return Component.literal(config.getName());
        }
        // Legacy / vanilla: empty modId — use the name as a direct translation key
        // (e.g. vanilla key category names like "key.categories.movement").
        return Component.translatable(config.getName());
    }
}
