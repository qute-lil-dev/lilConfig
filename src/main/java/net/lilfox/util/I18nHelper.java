package net.lilfox.util;

import net.lilfox.config.IConfig;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Utility for resolving lilConfig i18n keys with humanised fallbacks.
 *
 * <p>For a config entry with mod id {@code "mymod"} and name {@code "show_hud"}
 * the key tiers are:
 * <ul>
 *   <li>{@code mymod.show_hud} — row label shown in the config list.
 *       Fallback: humanised field name (e.g. {@code "Show Hud"}).</li>
 *   <li>{@code mymod.show_hud.nice} — extended display name used in
 *       in-game notifications when a hotkey fires. Returns {@code null} when
 *       the key is absent (caller decides whether to show anything).</li>
 *   <li>{@code mymod.show_hud.desc} — tooltip body. Returns {@code null} when
 *       absent (tooltip not shown).</li>
 * </ul>
 *
 * <p>Entries registered without a mod id (empty string) fall back to using
 * the entry name directly as the translation key, preserving backward
 * compatibility with the vanilla keybind provider and other internal uses.
 */
public final class I18nHelper {

    private I18nHelper() {}

    /**
     * Returns the row label {@link Component} for {@code config}.
     * Looks up {@code modid.name}; falls back to a humanised version of the entry name.
     *
     * @param config the config entry
     * @return a non-null label component
     */
    public static Component label(IConfig config) {
        String key = labelKey(config);
        if (!config.getModId().isEmpty() && !I18n.exists(key)) {
            return Component.literal(humanize(config.getName()));
        }
        return Component.translatable(key);
    }

    /**
     * Returns the translation key used for the row label.
     * When modId is empty, the bare entry name is used (legacy/internal behaviour).
     */
    public static String labelKey(IConfig config) {
        String modId = config.getModId();
        return modId.isEmpty() ? config.getName() : modId + "." + config.getName();
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
        String key = modId + "." + config.getName() + ".nice";
        return I18n.exists(key) ? I18n.get(key) : null;
    }

    /**
     * Returns a {@link Component} for the tooltip body if the {@code .desc} key exists,
     * otherwise returns {@code null} (tooltip not shown).
     *
     * @param config the config entry
     * @return a tooltip component, or {@code null}
     */
    public static @Nullable Component desc(IConfig config) {
        String modId = config.getModId();
        if (modId.isEmpty()) return null;
        String key = modId + "." + config.getName() + ".desc";
        return I18n.exists(key) ? Component.translatable(key) : null;
    }

    /**
     * Converts a lower_snake_case or UPPER_SNAKE_CASE identifier to a space-separated
     * title-cased string. {@code "show_hud"} → {@code "Show Hud"}.
     */
    static String humanize(String name) {
        if (name.isBlank()) return name;
        String lower = name.toLowerCase().replace('_', ' ');
        StringBuilder sb = new StringBuilder(lower.length());
        boolean cap = true;
        for (char c : lower.toCharArray()) {
            sb.append(cap ? Character.toUpperCase(c) : c);
            cap = c == ' ';
        }
        return sb.toString();
    }
}
