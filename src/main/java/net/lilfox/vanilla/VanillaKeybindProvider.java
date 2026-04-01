package net.lilfox.vanilla;

import net.lilfox.config.ConfigGroup;
import net.lilfox.config.ConfigHotkey;
import net.lilfox.config.IConfig;
import net.lilfox.hotkey.KeyBind;
import net.lilfox.manager.IConfigProvider;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link IConfigProvider} that wraps all vanilla {@link KeyMapping} objects as
 * {@link ConfigHotkey} entries, allowing the player to assign multi-key combos
 * to vanilla actions.
 *
 * <p>Persisted to {@code config/lilconfig_vanilla_keys.json}. Active only when
 * {@code LilConfigOwnConfig.VANILLA_KEY_OVERRIDE} is enabled; the mixin that
 * reads {@link #isComboHeld} and {@link #consumeClick} is always loaded but
 * guards itself with the same flag.
 *
 * <p>Initialization is lazy: {@link #ensureInitialized()} is called from
 * {@link #getConfigGroups()} (triggered at registration) and from {@link #tick()}.
 */
public final class VanillaKeybindProvider implements IConfigProvider {

    private static final VanillaKeybindProvider INSTANCE = new VanillaKeybindProvider();

    /** Map from vanilla KeyMapping to the ConfigHotkey holding the override combo. */
    private final Map<KeyMapping, ConfigHotkey> keyMap = new IdentityHashMap<>();

    private final List<ConfigGroup> groups = new ArrayList<>();

    /** Held state from the previous tick (true = combo was fully held). */
    private final Map<KeyMapping, Boolean> prevHeld = new IdentityHashMap<>();

    /** Accumulated pending clicks per KeyMapping (incremented on leading edge). */
    private final Map<KeyMapping, Integer> pendingClicks = new IdentityHashMap<>();

    private volatile boolean initialized = false;

    private VanillaKeybindProvider() {}

    /** Returns the singleton instance. */
    public static VanillaKeybindProvider getInstance() {
        return INSTANCE;
    }

    // -------------------------------------------------------------------------
    // Lazy initialization
    // -------------------------------------------------------------------------

    /**
     * Initialises the key map and config groups from the current vanilla key mappings.
     * Idempotent and thread-safe; runs at most once.
     */
    private synchronized void ensureInitialized() {
        if (initialized) return;

        KeyMapping[] keyMappings = Minecraft.getInstance().options.keyMappings;
        Map<KeyMapping.Category, List<IConfig>> byCategory = new LinkedHashMap<>();

        for (KeyMapping km : keyMappings) {
            ConfigHotkey hotkey = new ConfigHotkey(km.getName(), KeyBind.NONE);
            keyMap.put(km, hotkey);
            byCategory.computeIfAbsent(km.getCategory(), k -> new ArrayList<>()).add(hotkey);
        }

        for (Map.Entry<KeyMapping.Category, List<IConfig>> entry : byCategory.entrySet()) {
            groups.add(new ConfigGroup(entry.getKey().toString(), entry.getValue()));
        }

        initialized = true;
    }

    // -------------------------------------------------------------------------
    // IConfigProvider
    // -------------------------------------------------------------------------

    @Override
    public String getModId() {
        return "lilconfig_vanilla_keys";
    }

    @Override
    public String getDisplayName() {
        return "Vanilla Key Overrides";
    }

    @Override
    public List<ConfigGroup> getConfigGroups() {
        ensureInitialized();
        return Collections.unmodifiableList(groups);
    }

    @Override
    public KeyBind getMenuOpenKey() {
        return KeyBind.NONE;
    }

    // -------------------------------------------------------------------------
    // Tick — called each client tick when override is enabled
    // -------------------------------------------------------------------------

    /**
     * Polls each mapped combo, updates held state, and accumulates pending clicks.
     * Must only be called from the client thread.
     */
    public void tick() {
        ensureInitialized();

        for (Map.Entry<KeyMapping, ConfigHotkey> entry : keyMap.entrySet()) {
            KeyMapping km = entry.getKey();
            KeyBind bind = entry.getValue().getKeyBind();

            if (bind == KeyBind.NONE || bind.getKeys().isEmpty()) {
                prevHeld.put(km, false);
                continue;
            }

            boolean held = bind.isPressed();
            boolean wasHeld = prevHeld.getOrDefault(km, false);

            if (held && !wasHeld) {
                pendingClicks.put(km, pendingClicks.getOrDefault(km, 0) + 1);
            }

            prevHeld.put(km, held);
        }
    }

    // -------------------------------------------------------------------------
    // Mixin callbacks
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if the override combo for {@code km} is currently held.
     * Called from the {@code isDown()} mixin inject.
     *
     * @param km the vanilla key mapping to query
     * @return {@code true} if the override combo is held
     */
    public boolean isComboHeld(KeyMapping km) {
        return prevHeld.getOrDefault(km, false);
    }

    /**
     * Consumes one pending click for {@code km} and returns {@code true} if one was available.
     * Called from the {@code consumeClick()} mixin inject.
     *
     * @param km the vanilla key mapping to query
     * @return {@code true} if a click was consumed
     */
    public boolean consumeClick(KeyMapping km) {
        int count = pendingClicks.getOrDefault(km, 0);
        if (count <= 0) return false;
        pendingClicks.put(km, count - 1);
        return true;
    }

    /**
     * Returns {@code true} once {@link #ensureInitialized()} has completed.
     * The mixin guards itself with this check to avoid acting before the key map is ready.
     */
    public boolean isInitialized() {
        return initialized;
    }
}
