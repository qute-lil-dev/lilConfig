package net.lilfox.manager;

import com.mojang.blaze3d.platform.InputConstants;
import net.lilfox.config.ConfigGroup;
import net.lilfox.config.ConfigHotkeyedBoolean;
import net.lilfox.config.IConfig;
import net.lilfox.config.IConfigHotkey;
import net.lilfox.hotkey.HotkeyContext;
import net.lilfox.hotkey.IHotkeyCallback;
import net.lilfox.hotkey.KeyBind;
import net.lilfox.mixin.KeyMappingCurrentKeyAccessor;
import net.lilfox.persist.ConfigSerializer;
import net.lilfox.util.I18nHelper;
import net.lilfox.vanilla.VanillaKeybindProvider;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.List;

/**
 * Central registry for all lilConfig providers.
 *
 * <p>Obtain the singleton via {@link #getInstance()} and call
 * {@link #register(IConfigProvider)} from your {@code ClientModInitializer}.
 * The manager loads each provider's persisted values immediately on registration
 * and saves all providers when explicitly requested (e.g. on screen close or
 * client shutdown).
 *
 * <p>Hotkey triggering uses fire-on-press semantics: a hotkey fires on the tick
 * when all keys first become simultaneously held (leading edge), provided the keys
 * were pressed in recorded order and no superset binding was simultaneously active
 * (superset inhibition).
 */
public final class ConfigManager {

    private static final ConfigManager INSTANCE = new ConfigManager();

    private final List<IConfigProvider> providers = new ArrayList<>();
    private final List<IConfigHotkey> allHotkeys = new ArrayList<>();

    /** Gesture state keyed by object identity (provider or IConfigHotkey). */
    private final Map<Object, GestureState> gestureStates =
            Collections.synchronizedMap(new IdentityHashMap<>());

    private ConfigManager() {}

    /** Returns the singleton instance. */
    public static ConfigManager getInstance() {
        return INSTANCE;
    }

    /**
     * Scans {@code configClass} (annotated with
     * {@link net.lilfox.annotation.Config}) and registers the resulting
     * provider. Equivalent to calling
     * {@link #register(IConfigProvider) register(AnnotationConfigScanner.scan(configClass))}.
     *
     * @param configClass the config holder class annotated with {@code @Config}
     * @throws IllegalArgumentException if the class lacks the annotation or a provider
     *                                  with the same mod ID is already registered
     */
    public void register(Class<?> configClass) {
        register(AnnotationConfigScanner.scan(configClass));
    }

    /**
     * Registers a config provider and immediately loads its persisted values
     * from disk via {@link ConfigSerializer#load(IConfigProvider)}.
     *
     * @param provider the provider to register
     * @throws IllegalArgumentException if a provider with the same mod ID is already registered
     */
    public void register(IConfigProvider provider) {
        for (IConfigProvider existing : providers) {
            if (existing.getModId().equals(provider.getModId())) {
                throw new IllegalArgumentException(
                        "A provider with mod ID '" + provider.getModId() + "' is already registered.");
            }
        }
        providers.add(provider);
        for (ConfigGroup g : provider.getConfigGroups())
            for (IConfig c : g.getConfigs())
                if (c instanceof IConfigHotkey hk)
                    allHotkeys.add(hk);
        ConfigSerializer.load(provider);
    }

    /** Returns an unmodifiable list of all registered providers in registration order. */
    public List<IConfigProvider> getProviders() {
        return Collections.unmodifiableList(providers);
    }

    /**
     * Describes a single hotkey conflict: another binding that overlaps with the queried one.
     *
     * @param modName    display name of the mod or source (e.g. "Litematica", "Vanilla Keys")
     * @param groupName  display name of the config group / category
     * @param entryLabel translated label of the conflicting config entry
     * @param keyDisplay human-readable key string of the conflicting binding
     */
    public record ConflictEntry(String modName, String groupName, Component entryLabel, String keyDisplay) {}

    /**
     * Returns a list of all bindings that conflict with {@code kb}.
     *
     * <p>Two bindings conflict when one's key set is a subset of or equal to the other's.
     * Conflict domains are isolated: debug vanilla keys only conflict within
     * {@link net.minecraft.client.KeyMapping.Category#DEBUG}, and spectator vanilla keys
     * only within {@link net.minecraft.client.KeyMapping.Category#SPECTATOR}.
     *
     * <p>When both the owner and the candidate are vanilla keys still at their default values,
     * no conflict is reported (vanilla intentionally ships duplicate defaults).
     *
     * <p>In addition to registered providers, also checks native vanilla {@link KeyMapping}
     * instances (the player's current assignments in the controls screen) that are not already
     * covered by a VanillaKeybindProvider override.
     *
     * @param kb    the binding to check
     * @param owner the config entry that owns {@code kb}; skipped during iteration.
     *              May be {@code null} to treat as a non-categorised entry.
     * @return an unmodifiable list of conflict entries (empty when no conflicts)
     */
    public List<ConflictEntry> getConflicts(KeyBind kb, @Nullable IConfigHotkey owner) {
        if (kb.getKeys().isEmpty()) return List.of();
        VanillaKeybindProvider vanilla = VanillaKeybindProvider.getInstance();
        boolean ownerIsDebug = owner != null && vanilla.isDebugHotkey(owner);
        boolean ownerIsSpectator = owner != null && vanilla.isSpectatorHotkey(owner);
        List<InputConstants.Key> ak = kb.getKeys();

        List<ConflictEntry> result = new ArrayList<>();

        for (IConfigProvider provider : providers) {
            for (ConfigGroup group : provider.getConfigGroups()) {
                for (IConfig config : group.getConfigs()) {
                    if (!(config instanceof IConfigHotkey hk)) continue;
                    if (hk == owner) continue;
                    KeyBind other = hk.getKeyBind();
                    if (other.getKeys().isEmpty()) continue;
                    if (ownerIsDebug != vanilla.isDebugHotkey(hk)) continue;
                    if (ownerIsSpectator != vanilla.isSpectatorHotkey(hk)) continue;
                    if (owner != null
                            && vanilla.isVanillaHotkey(owner)
                            && vanilla.isVanillaHotkey(hk)
                            && !owner.isModified()
                            && !hk.isModified()) continue;
                    List<InputConstants.Key> bk = other.getKeys();
                    if (bk.containsAll(ak) || ak.containsAll(bk)) {
                        result.add(new ConflictEntry(
                                provider.getDisplayName(),
                                group.getName(),
                                I18nHelper.label(config),
                                other.toDisplayString()));
                    }
                }
            }
        }

        KeyMapping[] keyMappings = Minecraft.getInstance().options.keyMappings;
        for (KeyMapping km : keyMappings) {
            KeyBind override = vanilla.getComboForMapping(km);
            if (!override.getKeys().isEmpty()) continue;
            InputConstants.Key rawKey = ((KeyMappingCurrentKeyAccessor) km).getCurrentKey();
            if (rawKey.equals(InputConstants.UNKNOWN)) continue;
            KeyBind vanillaBind = KeyBind.of(rawKey);
            List<InputConstants.Key> bk = vanillaBind.getKeys();
            if (bk.containsAll(ak) || ak.containsAll(bk)) {
                boolean isDebug = km.getCategory() == KeyMapping.Category.DEBUG;
                boolean isSpectator = km.getCategory() == KeyMapping.Category.SPECTATOR;
                if (isDebug != ownerIsDebug || isSpectator != ownerIsSpectator) continue;
                result.add(new ConflictEntry(
                        Component.translatable("lilconfig.tooltip.conflicts.vanilla").getString(),
                        km.getCategory().label().getString(),
                        Component.translatable(km.getName()),
                        rawKey.getDisplayName().getString()));
            }
        }

        return Collections.unmodifiableList(result);
    }

    /**
     * Returns {@code true} if {@code kb} has a subset/superset conflict with any other
     * registered {@link IConfigHotkey} entry or native vanilla key binding.
     *
     * @param kb    the binding to check
     * @param owner the config entry that owns {@code kb}; may be {@code null}
     * @return {@code true} if a conflict exists
     */
    public boolean isConflicting(KeyBind kb, @Nullable IConfigHotkey owner) {
        return !getConflicts(kb, owner).isEmpty();
    }

    /**
     * Saves all registered providers' current values to their respective JSON files.
     * Called on config screen close and on client shutdown.
     */
    public void saveAll() {
        for (IConfigProvider provider : providers) {
            ConfigSerializer.save(provider);
        }
    }

    /**
     * Polls all registered {@link IConfigHotkey} entries and fires those whose hotkey
     * triggered this tick. Respects each entry's {@link HotkeyContext}.
     *
     * <p>On fire:
     * <ul>
     *   <li>{@link ConfigHotkeyedBoolean} entries toggle their boolean value.</li>
     *   <li>Any entry with a registered {@link IHotkeyCallback} invokes it.</li>
     * </ul>
     *
     * <p>Firing semantics:
     * <ul>
     *   <li>A hotkey fires when all keys are simultaneously held for the first time
     *       (leading edge / fire-on-press), so that e.g. a Ctrl hotkey does not
     *       accidentally fire when Ctrl+Alt+A is pressed.</li>
     *   <li>Keys must be pressed in the order they appear in the {@link KeyBind}
     *       (order of assignment during recording).</li>
     *   <li>If another binding that shares at least one key is simultaneously fully held and
     *       is not a subset of this binding, this binding is suppressed (active-key inhibition).
     *       This prevents keys already claimed by an active combo from triggering a new combo,
     *       while still allowing a superset combo to extend the active one.</li>
     * </ul>
     *
     * @param currentScreen the currently open screen, or {@code null} if in-game
     */
    public void tickHotkeys(@Nullable Screen currentScreen) {
        boolean inGame = currentScreen == null;
        long windowHandle = Minecraft.getInstance().getWindow().handle();

        record Entry(IConfigHotkey hk, KeyBind bind, GestureState state) {}
        List<Entry> active = new ArrayList<>();

        for (IConfigHotkey hk : allHotkeys) {
            boolean contextMatch = switch (hk.getHotkeyContext()) {
                case IN_GAME  -> inGame;
                case GUI_OPEN -> !inGame;
                case ALWAYS   -> true;
            };
            KeyBind bind = hk.getKeyBind();
            if (!contextMatch || bind == KeyBind.NONE || bind.getKeys().isEmpty()) continue;

            GestureState state = getOrCreateState(hk, bind.getKeys().size());
            tickGesture(state, bind, windowHandle);
            active.add(new Entry(hk, bind, state));
        }

        for (Entry e1 : active) {
            if (!e1.state.pendingFire) continue;
            for (Entry e2 : active) {
                if (e2 == e1) continue;
                if (e2.state.wasFullyHeld
                        && hasKeyOverlap(e1.bind, e2.bind)
                        && !isSubset(e2.bind, e1.bind)) {
                    e1.state.pendingFire = false;
                    break;
                }
            }
        }
        for (Entry e : active) {
            if (!e.state.pendingFire) continue;
            if (e.hk() instanceof ConfigHotkeyedBoolean hb) {
                hb.setValue(!hb.getValue());
            }
            IHotkeyCallback cb = e.hk().getCallback();
            if (cb != null) cb.onHotkey();
        }
    }

    /**
     * Polls all provider menu keys and returns providers whose menu key fired this tick.
     *
     * <p>Should be called each client tick when no config screen is open.
     */
    public List<IConfigProvider> pollFiredMenuKeys() {
        long windowHandle = Minecraft.getInstance().getWindow().handle();

        record MenuEntry(IConfigProvider provider, KeyBind bind, GestureState state) {}
        List<MenuEntry> active = new ArrayList<>();

        for (IConfigProvider provider : providers) {
            KeyBind menuKey = provider.getMenuOpenKey();
            if (menuKey == KeyBind.NONE || menuKey.getKeys().isEmpty()) continue;
            GestureState state = getOrCreateState(provider, menuKey.getKeys().size());
            tickGesture(state, menuKey, windowHandle);
            active.add(new MenuEntry(provider, menuKey, state));
        }

        for (MenuEntry e1 : active) {
            if (!e1.state.pendingFire) continue;
            for (MenuEntry e2 : active) {
                if (e2 == e1) continue;
                if (e2.state.wasFullyHeld
                        && hasKeyOverlap(e1.bind, e2.bind)
                        && !isSubset(e2.bind, e1.bind)) {
                    e1.state.pendingFire = false;
                    break;
                }
            }
        }

        List<IConfigProvider> fired = new ArrayList<>();
        for (MenuEntry e : active) {
            if (e.state.pendingFire) fired.add(e.provider);
        }
        return fired;
    }

    /**
     * Tracks the gesture state for a single key binding slot across ticks.
     *
     * <p>A gesture starts when the first key of the binding is pressed and ends
     * when all keys are simultaneously released.
     */
    private static final class GestureState {
        /** Held state recorded at the end of the previous tick (parallel to bind.keys). */
        boolean[] prevHeld;
        /**
         * Index into bind.keys of the next key that must be pressed to maintain
         * correct order. Incremented on each in-order press; reset on gesture end.
         */
        int expectedNext;
        /** Set when any key in the bind was pressed out of order in the current gesture. */
        boolean orderBroken;
        /** True while all keys of the bind are currently held (set at the end of each tick). */
        boolean wasFullyHeld;
        /** True if this binding fired this tick (consumed by the caller each tick). */
        boolean pendingFire;

        GestureState(int keyCount) {
            prevHeld = new boolean[keyCount];
        }

        boolean orderValid() {
            return !orderBroken && expectedNext >= prevHeld.length;
        }
    }

    private GestureState getOrCreateState(Object slot, int keyCount) {
        GestureState s = gestureStates.get(slot);
        if (s == null || s.prevHeld.length != keyCount) {
            s = new GestureState(keyCount);
            gestureStates.put(slot, s);
        }
        return s;
    }

    /**
     * Advances the gesture state machine by one tick and sets {@link GestureState#pendingFire}.
     */
    private static void tickGesture(GestureState state, KeyBind bind, long windowHandle) {
        List<InputConstants.Key> keys = bind.getKeys();
        int n = keys.size();
        boolean[] curr = new boolean[n];
        boolean fullyHeld = true;
        boolean allReleased = true;

        for (int i = 0; i < n; i++) {
            curr[i] = KeyBind.isKeyHeld(keys.get(i), windowHandle);
            if (!curr[i]) fullyHeld = false;
            if (curr[i]) allReleased = false;
        }

        for (int i = 0; i < n; i++) {
            if (curr[i] && !state.prevHeld[i]) {
                if (i == state.expectedNext) {
                    state.expectedNext++;
                } else {
                    state.orderBroken = true;
                }
            }
        }

        state.pendingFire = fullyHeld && !state.wasFullyHeld && state.orderValid();

        if (allReleased) {
            state.expectedNext = 0;
            state.orderBroken = false;
        }

        state.wasFullyHeld = fullyHeld;
        state.prevHeld = curr;
    }

    /** Returns {@code true} if {@code a} and {@code b} share at least one key. */
    private static boolean hasKeyOverlap(KeyBind a, KeyBind b) {
        List<InputConstants.Key> bKeys = b.getKeys();
        for (InputConstants.Key k : a.getKeys()) {
            if (bKeys.contains(k)) return true;
        }
        return false;
    }

    /**
     * Returns {@code true} if every key in {@code sub} is also present in {@code sup}
     * (non-strict: equal sets return {@code true}).
     */
    private static boolean isSubset(KeyBind sub, KeyBind sup) {
        return sup.getKeys().containsAll(sub.getKeys());
    }

}
