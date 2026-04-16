package net.lilfox.manager;

import net.fabricmc.loader.api.FabricLoader;
import net.lilfox.annotation.DevTab;
import net.lilfox.annotation.Hotkeyed;
import net.lilfox.annotation.Config;
import net.lilfox.annotation.MenuKey;
import net.lilfox.annotation.Section;
import net.lilfox.annotation.Tab;
import net.lilfox.config.*;
import net.lilfox.hotkey.KeyBind;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Scans a class annotated with {@link Config} and builds an
 * {@link IConfigProvider} adapter from its static {@link IConfig} fields.
 *
 * <p>Field processing rules:
 * <ul>
 *   <li>Fields must be {@code static} and hold an {@link IConfig} instance.</li>
 *   <li>Fields with {@link Tab} are assigned to the named tab; first-occurrence
 *       order determines tab ordering.</li>
 *   <li>Fields with {@link Section} are grouped under a named section within their
 *       tab; a {@link ConfigSeparator} header is inserted on every section change.</li>
 *   <li>Fields without {@link Tab} when others have one are routed to a {@code "misc"} tab.</li>
 *   <li>Fields without {@link Section} when others in the same tab have one are routed
 *       to a {@code "misc"} section.</li>
 *   <li>Fields annotated with {@link Hotkeyed} and declared as {@link ConfigBoolean} are
 *       replaced (via reflection) with a {@link ConfigHotkeyedBoolean} using the
 *       annotation's {@code defaultKey}.</li>
 *   <li>The field name (UPPER_SNAKE_CASE → lowerCamelCase) is injected as each
 *       entry's name; the mod id is also set so the GUI can build i18n keys.</li>
 * </ul>
 */
final class AnnotationConfigScanner {

    private static final String MISC     = "misc";
    private static final String NO_SEC   = "__none__";

    private AnnotationConfigScanner() {}

    /**
     * Scans {@code configClass} and returns an {@link IConfigProvider} adapter.
     *
     * @param configClass the class annotated with {@link Config}
     * @return a provider ready for registration
     * @throws IllegalArgumentException if the class lacks {@link Config}
     * @see #toCamelCase(String)
     */
    @SuppressWarnings("null")   // getAnnotation() can return null; IDE null-analysis is imprecise here
    static IConfigProvider scan(Class<?> configClass) {
        @Nullable Config meta = configClass.getAnnotation(Config.class);
        if (meta == null) {
            throw new IllegalArgumentException(
                    configClass.getName() + " is not annotated with @Config");
        }

        String modId       = meta.modId();
        String displayName = meta.displayName().isBlank() ? modId : meta.displayName();

        boolean isDev = FabricLoader.getInstance().isDevelopmentEnvironment();

        Map<String, Map<String, List<IConfig>>> tabMap = new LinkedHashMap<>();

        @Nullable IConfigHotkey menuKeyEntry = null;

        boolean anyTab = false;
        for (Field f : configClass.getDeclaredFields()) {
            if (!isConfigField(f)) continue;
            if (f.getAnnotation(Tab.class) != null
                    || (isDev && f.getAnnotation(DevTab.class) != null)) {
                anyTab = true;
                break;
            }
        }

        for (Field f : configClass.getDeclaredFields()) {
            if (!isConfigField(f)) continue;
            if (!isDev && f.getAnnotation(DevTab.class) != null) continue;
            f.setAccessible(true);

            IConfig config = readField(f);
            if (config == null) continue;

            config = applyHotkeyed(f, config);

            String name = toCamelCase(f.getName());
            ((ConfigBase<?>) config).setName(name);
            ((ConfigBase<?>) config).setModId(modId);

            if (f.getAnnotation(MenuKey.class) != null && config instanceof IConfigHotkey hk) {
                menuKeyEntry = hk;
            }

            @Nullable Tab     tabAnn    = f.getAnnotation(Tab.class);
            @Nullable DevTab  devTabAnn = f.getAnnotation(DevTab.class);
            @Nullable Section secAnn    = f.getAnnotation(Section.class);

            String tabId = (tabAnn != null)    ? tabAnn.value()
                         : (devTabAnn != null)  ? devTabAnn.value()
                         : (anyTab              ? MISC : modId);
            @Nullable String secId = (secAnn != null) ? secAnn.value() : null;

            Map<String, List<IConfig>> sections =
                    tabMap.computeIfAbsent(tabId, k -> new LinkedHashMap<>());

            String bucket = secId != null ? secId
                          : hasNamedSection(sections) ? MISC
                          : NO_SEC;

            sections.computeIfAbsent(bucket, k -> new ArrayList<>()).add(config);
        }

        for (Map<String, List<IConfig>> sections : tabMap.values()) {
            if (hasNamedSection(sections) && sections.containsKey(NO_SEC)) {
                List<IConfig> orphans = sections.remove(NO_SEC);
                sections.computeIfAbsent(MISC, k -> new ArrayList<>()).addAll(0, orphans);
            }
        }

        List<ConfigGroup> groups = new ArrayList<>();
        for (Map.Entry<String, Map<String, List<IConfig>>> tabEntry : tabMap.entrySet()) {
            Map<String, List<IConfig>> sections = tabEntry.getValue();
            boolean multiSection = sections.size() > 1
                    || (sections.size() == 1 && !sections.containsKey(NO_SEC));

            List<IConfig> flat = new ArrayList<>();
            for (Map.Entry<String, List<IConfig>> secEntry : sections.entrySet()) {
                List<IConfig> entries = secEntry.getValue();
                if (entries.isEmpty()) continue;
                if (multiSection && !secEntry.getKey().equals(NO_SEC)) {
                    flat.add(new ConfigSeparator(modId, secEntry.getKey()));
                }
                flat.addAll(entries);
            }
            groups.add(new ConfigGroup(tabEntry.getKey(), flat));
        }

        return new ScannedConfigProvider(modId, displayName, menuKeyEntry, groups);
    }

    private static boolean isConfigField(Field f) {
        return Modifier.isStatic(f.getModifiers())
                && IConfig.class.isAssignableFrom(f.getType());
    }

    private static @Nullable IConfig readField(Field f) {
        try {
            return (IConfig) f.get(null);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot access config field " + f.getName(), e);
        }
    }

    /**
     * If {@code f} carries {@link Hotkeyed} and {@code config} is a plain
     * {@link ConfigBoolean}, replaces the field value with a
     * {@link ConfigHotkeyedBoolean} and returns it. Otherwise returns
     * {@code config} unchanged.
     */
    @SuppressWarnings("null")
    private static IConfig applyHotkeyed(Field f, IConfig config) {
        @Nullable Hotkeyed ann = f.getAnnotation(Hotkeyed.class);
        if (ann == null) return config;
        if (!(config instanceof ConfigBoolean bool)) return config;
        if (config instanceof ConfigHotkeyedBoolean) return config;

        String key = ann.defaultKey();
        KeyBind defaultBind = key.isBlank() ? KeyBind.NONE : KeyBind.parse(key);
        ConfigHotkeyedBoolean hb = new ConfigHotkeyedBoolean("", bool.getValue(), defaultBind);
        try {
            f.set(null, hb);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot replace field " + f.getName()
                    + " with ConfigHotkeyedBoolean", e);
        }
        return hb;
    }

    /** Returns {@code true} if {@code sections} contains any bucket other than {@link #NO_SEC}. */
    private static boolean hasNamedSection(Map<String, List<IConfig>> sections) {
        for (String k : sections.keySet()) {
            if (!k.equals(NO_SEC)) return true;
        }
        return false;
    }

    /**
     * Converts a Java field name to lowerCamelCase.
     * {@code SHOW_HUD} → {@code "showHud"}, {@code maxItems} → {@code "maxItems"}.
     */
    static String toCamelCase(String name) {
        if (!name.contains("_")) {
            return Character.toLowerCase(name.charAt(0)) + name.substring(1);
        }
        String[] parts = name.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                sb.append(Character.toUpperCase(parts[i].charAt(0)));
                sb.append(parts[i].substring(1));
            }
        }
        return sb.toString();
    }

    // -------------------------------------------------------------------------

    /** Minimal {@link IConfigProvider} produced from scanned annotation metadata. */
    private static final class ScannedConfigProvider implements IConfigProvider {

        private final String modId;
        private final String displayName;
        private final @Nullable IConfigHotkey menuKeyEntry;
        private final List<ConfigGroup> groups;

        ScannedConfigProvider(String modId, String displayName,
                              @Nullable IConfigHotkey menuKeyEntry, List<ConfigGroup> groups) {
            this.modId         = modId;
            this.displayName   = displayName;
            this.menuKeyEntry  = menuKeyEntry;
            this.groups        = List.copyOf(groups);
        }

        @Override public String            getModId()        { return modId; }
        @Override public String            getDisplayName()  { return displayName; }
        @Override public List<ConfigGroup> getConfigGroups() { return groups; }
        @Override public KeyBind           getMenuOpenKey()  {
            return menuKeyEntry != null ? menuKeyEntry.getKeyBind() : KeyBind.NONE;
        }
    }
}
