package net.lilfox.vanilla;

import com.mojang.blaze3d.platform.InputConstants;
import net.lilfox.config.ConfigGroup;
import net.lilfox.config.ConfigHotkey;
import net.lilfox.config.ConfigSeparator;
import net.lilfox.config.IConfig;
import net.lilfox.config.IConfigHotkey;
import net.lilfox.hotkey.KeyBind;
import net.lilfox.manager.IConfigProvider;
import net.lilfox.manager.LilConfigManager;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
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

    /** Reverse map: ConfigHotkey → its vanilla KeyMapping, used for category checks. */
    private final Map<Object, KeyMapping> reverseKeyMap = new IdentityHashMap<>();

    private final List<ConfigGroup> groups = new ArrayList<>();

    /** Held state from the previous tick (true = combo was fully held). */
    private final Map<KeyMapping, Boolean> prevHeld = new IdentityHashMap<>();

    /** Accumulated pending clicks per KeyMapping (incremented on leading edge). */
    private final Map<KeyMapping, Integer> pendingClicks = new IdentityHashMap<>();

    private volatile boolean initialized = false;

    @Nullable private ConfigGroup cachedFlatGroup;

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

        KeyMapping[] keyMappings = Minecraft.getInstance().options.keyMappings.clone();
        Arrays.sort(keyMappings);
        Map<KeyMapping.Category, List<IConfig>> byCategory = new LinkedHashMap<>();

        for (KeyMapping km : keyMappings) {
            InputConstants.Key rawKey = km.getDefaultKey();
            KeyBind def = rawKey.equals(InputConstants.UNKNOWN) ? KeyBind.NONE : KeyBind.of(rawKey);
            ConfigHotkey hotkey = new ConfigHotkey(km.getName(), def);
            keyMap.put(km, hotkey);
            reverseKeyMap.put(hotkey, km);
            byCategory.computeIfAbsent(km.getCategory(), k -> new ArrayList<>()).add(hotkey);
        }

        for (Map.Entry<KeyMapping.Category, List<IConfig>> entry : byCategory.entrySet()) {
            Component desc = entry.getKey().label();
            String nameKey = desc.getContents() instanceof TranslatableContents tc
                    ? tc.getKey()
                    : desc.getString();
            groups.add(new ConfigGroup(nameKey, entry.getValue()));
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
     * Polls each mapped combo, applies superset inhibition, updates held state,
     * and accumulates pending clicks on the leading edge.
     * Must only be called from the client thread.
     */
    public void tick() {
        ensureInitialized();

        // Step 1: compute raw held state for all entries.
        Map<KeyMapping, Boolean> heldNow = new IdentityHashMap<>(keyMap.size());
        for (Map.Entry<KeyMapping, ConfigHotkey> entry : keyMap.entrySet()) {
            KeyBind bind = entry.getValue().getKeyBind();
            boolean held = bind != KeyBind.NONE && !bind.getKeys().isEmpty() && bind.isPressed();
            heldNow.put(entry.getKey(), held);
        }

        // Step 2: superset inhibition — if a proper superset combo is also held, suppress this one.
        for (Map.Entry<KeyMapping, ConfigHotkey> e1 : keyMap.entrySet()) {
            if (!heldNow.getOrDefault(e1.getKey(), false)) continue;
            KeyBind bind1 = e1.getValue().getKeyBind();
            for (Map.Entry<KeyMapping, ConfigHotkey> e2 : keyMap.entrySet()) {
                if (e2.getKey() == e1.getKey()) continue;
                if (!heldNow.getOrDefault(e2.getKey(), false)) continue;
                if (isProperSubset(bind1, e2.getValue().getKeyBind())) {
                    heldNow.put(e1.getKey(), false);
                    break;
                }
            }
        }

        // Step 3: update prevHeld and accumulate clicks on the leading edge.
        for (Map.Entry<KeyMapping, ConfigHotkey> entry : keyMap.entrySet()) {
            KeyMapping km = entry.getKey();
            boolean held = heldNow.getOrDefault(km, false);
            boolean wasHeld = prevHeld.getOrDefault(km, false);
            if (held && !wasHeld) {
                pendingClicks.put(km, pendingClicks.getOrDefault(km, 0) + 1);
            }
            prevHeld.put(km, held);
        }
    }

    private static boolean isProperSubset(KeyBind sub, KeyBind sup) {
        List<InputConstants.Key> subKeys = sub.getKeys();
        List<InputConstants.Key> supKeys = sup.getKeys();
        if (subKeys.size() >= supKeys.size()) return false;
        return supKeys.containsAll(subKeys);
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

    // -------------------------------------------------------------------------
    // Variant B / C support
    // -------------------------------------------------------------------------

    /**
     * Returns the current override combo for the given vanilla key mapping,
     * or {@link KeyBind#NONE} if no override is configured.
     *
     * @param km the vanilla key mapping to query
     * @return the current combo, or {@link KeyBind#NONE}
     */
    public KeyBind getComboForMapping(KeyMapping km) {
        ensureInitialized();
        ConfigHotkey hk = keyMap.get(km);
        return hk != null ? hk.getKeyBind() : KeyBind.NONE;
    }

    /**
     * Returns {@code true} if {@code hk} belongs to the vanilla debug key category
     * ({@link net.minecraft.client.KeyMapping.Category#DEBUG}).
     * Debug keys are only active when F3 is held, so they only conflict within
     * their own category.
     *
     * @param hk the config hotkey to test
     * @return {@code true} if the underlying {@link KeyMapping} is in the debug category
     */
    public boolean isDebugHotkey(IConfigHotkey hk) {
        ensureInitialized();
        KeyMapping km = reverseKeyMap.get(hk);
        return km != null && km.getCategory() == KeyMapping.Category.DEBUG;
    }

    /**
     * Returns {@code true} if {@code hk} belongs to the vanilla spectator key category
     * ({@link net.minecraft.client.KeyMapping.Category#SPECTATOR}).
     * Spectator keys are only active in spectator mode, so they only conflict within
     * their own category.
     *
     * @param hk the config hotkey to test
     * @return {@code true} if the underlying {@link KeyMapping} is in the spectator category
     */
    public boolean isSpectatorHotkey(IConfigHotkey hk) {
        ensureInitialized();
        KeyMapping km = reverseKeyMap.get(hk);
        return km != null && km.getCategory() == KeyMapping.Category.SPECTATOR;
    }

    /**
     * Returns {@code true} if {@code hk} is a vanilla key managed by this provider.
     * Used to apply vanilla-specific conflict rules (e.g. skip conflict when both
     * keys are at their default values, since vanilla intentionally ships duplicate
     * defaults such as {@code key.debug.overlay} and {@code key.debug.modifier}).
     *
     * @param hk the config hotkey to test
     * @return {@code true} if {@code hk} wraps a vanilla {@link KeyMapping}
     */
    public boolean isVanillaHotkey(IConfigHotkey hk) {
        return reverseKeyMap.containsKey(hk);
    }

    /**
     * Returns {@code true} if the override combo for {@code km} has a subset/superset conflict
     * with any other non-empty override combo in the key map.
     *
     * <p>Two combos conflict when one's key set is a subset of or equal to the other's.
     * Conflict domains are isolated: debug keys only conflict within
     * {@link net.minecraft.client.KeyMapping.Category#DEBUG}, and spectator keys only within
     * {@link net.minecraft.client.KeyMapping.Category#SPECTATOR}, since both groups are
     * active only under specific conditions (F3 held / spectator mode).
     *
     * <p>No conflict is reported when both combos are still at their default values.
     * Vanilla intentionally ships duplicate defaults (e.g. {@code key.debug.overlay} and
     * {@code key.debug.modifier} are both F3), so showing a conflict there would be misleading.
     *
     * @param km the vanilla key mapping to check
     * @return {@code true} if a conflict exists
     */
    public boolean hasConflictForMapping(KeyMapping km) {
        ensureInitialized();
        ConfigHotkey target = keyMap.get(km);
        if (target == null) return false;
        KeyBind targetBind = target.getKeyBind();
        if (targetBind.getKeys().isEmpty()) return false;
        boolean targetIsDebug = km.getCategory() == KeyMapping.Category.DEBUG;
        boolean targetIsSpectator = km.getCategory() == KeyMapping.Category.SPECTATOR;
        for (Map.Entry<KeyMapping, ConfigHotkey> entry : keyMap.entrySet()) {
            if (entry.getKey() == km) continue;
            KeyBind other = entry.getValue().getKeyBind();
            if (other.getKeys().isEmpty()) continue;
            boolean entryIsDebug = entry.getKey().getCategory() == KeyMapping.Category.DEBUG;
            boolean entryIsSpectator = entry.getKey().getCategory() == KeyMapping.Category.SPECTATOR;
            if (entryIsDebug != targetIsDebug || entryIsSpectator != targetIsSpectator) continue;
            // Vanilla ships intentional duplicate defaults (e.g. debug overlay and modifier are
            // both F3); skip conflict when both overrides are still at their default values.
            if (!target.isModified() && !entry.getValue().isModified()) continue;
            List<InputConstants.Key> ak = targetBind.getKeys();
            List<InputConstants.Key> bk = other.getKeys();
            if (bk.containsAll(ak) || ak.containsAll(bk)) return true;
        }
        return false;
    }

    /**
     * Sets the override combo for the given vanilla key mapping and persists immediately.
     *
     * @param km   the vanilla key mapping to update
     * @param bind the new combo; {@link KeyBind#NONE} effectively disables the override
     */
    public void setComboForMapping(KeyMapping km, KeyBind bind) {
        ensureInitialized();
        ConfigHotkey hk = keyMap.get(km);
        if (hk != null) hk.setKeyBind(bind);
        LilConfigManager.getInstance().saveAll();
    }

    /**
     * Returns a single {@link ConfigGroup} containing all override hotkeys with
     * {@link ConfigSeparator} headers between categories.
     * The result is cached after the first call. Shares the same {@link ConfigHotkey}
     * instances as the per-category groups, so changes are reflected immediately.
     */
    public synchronized ConfigGroup getFlatGroup() {
        ensureInitialized();
        if (cachedFlatGroup != null) return cachedFlatGroup;
        List<IConfig> entries = new ArrayList<>();
        for (ConfigGroup g : groups) {
            entries.add(new ConfigSeparator("", g.getName()));
            entries.addAll(g.getConfigs());
        }
        cachedFlatGroup = new ConfigGroup("vanilla_keys_flat", List.copyOf(entries));
        return cachedFlatGroup;
    }

    /**
     * Returns a transient {@link IConfigProvider} wrapping the flat group.
     * This provider is never registered with {@link LilConfigManager} — it is used
     * only to open {@link net.lilfox.gui.LilConfigScreen} in Variant B.
     * Persistence is handled by the registered {@link VanillaKeybindProvider}.
     */
    public IConfigProvider asFlatProvider() {
        ConfigGroup flat = getFlatGroup();
        VanillaKeybindProvider self = this;
        return new IConfigProvider() {
            @Override public String getModId()                    { return self.getModId(); }
            @Override public String getDisplayName()              { return self.getDisplayName(); }
            @Override public List<ConfigGroup> getConfigGroups() { return List.of(flat); }
            @Override public KeyBind getMenuOpenKey()             { return KeyBind.NONE; }
        };
    }
}
