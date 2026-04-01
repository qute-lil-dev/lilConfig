package net.lilfox.manager;

import com.mojang.blaze3d.platform.InputConstants;
import net.lilfox.config.ConfigGroup;
import net.lilfox.config.ConfigHotkeyedBoolean;
import net.lilfox.config.IConfig;
import net.lilfox.hotkey.KeyBind;
import net.lilfox.persist.ConfigSerializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
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
 * <p>Hotkey triggering uses fire-on-release semantics: a hotkey fires when the
 * last-held key is released, provided the keys were pressed in recorded order and
 * no superset binding was simultaneously active (superset inhibition).
 */
public final class LilConfigManager {

    private static final LilConfigManager INSTANCE = new LilConfigManager();

    private final List<IConfigProvider> providers = new ArrayList<>();
    private final List<ConfigHotkeyedBoolean> hotkeyedBooleans = new ArrayList<>();

    /** Gesture state keyed by object identity (provider or ConfigHotkeyedBoolean). */
    private final Map<Object, GestureState> gestureStates =
            Collections.synchronizedMap(new IdentityHashMap<>());

    private LilConfigManager() {}

    /** Returns the singleton instance. */
    public static LilConfigManager getInstance() {
        return INSTANCE;
    }

    // -------------------------------------------------------------------------
    // Registration
    // -------------------------------------------------------------------------

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
                if (c instanceof ConfigHotkeyedBoolean hb)
                    hotkeyedBooleans.add(hb);
        ConfigSerializer.load(provider);
    }

    /** Returns an unmodifiable list of all registered providers in registration order. */
    public List<IConfigProvider> getProviders() {
        return Collections.unmodifiableList(providers);
    }

    // -------------------------------------------------------------------------
    // Persistence
    // -------------------------------------------------------------------------

    /**
     * Saves all registered providers' current values to their respective JSON files.
     * Called on config screen close and on client shutdown.
     */
    public void saveAll() {
        for (IConfigProvider provider : providers) {
            ConfigSerializer.save(provider);
        }
    }

    // -------------------------------------------------------------------------
    // Hotkey tick — fire-on-release, order-aware, superset-inhibited
    // -------------------------------------------------------------------------

    /**
     * Polls all registered {@link ConfigHotkeyedBoolean} entries and toggles those
     * whose hotkey just fired this tick. Respects each entry's
     * {@link ConfigHotkeyedBoolean.HotkeyContext}.
     *
     * <p>Firing semantics:
     * <ul>
     *   <li>A hotkey fires when the last-held key is <em>released</em> (not on press),
     *       so that e.g. pressing Ctrl+Alt+A does not accidentally fire a Ctrl hotkey.</li>
     *   <li>Keys must be pressed in the order they appear in the {@link KeyBind}
     *       (order of assignment during recording).</li>
     *   <li>If a superset binding was simultaneously fully held, the subset is suppressed.</li>
     * </ul>
     *
     * @param currentScreen the currently open screen, or {@code null} if in-game
     */
    public void tickHotkeys(@Nullable Screen currentScreen) {
        boolean inGame = currentScreen == null;
        long windowHandle = Minecraft.getInstance().getWindow().handle();

        record Entry(ConfigHotkeyedBoolean hb, KeyBind bind, GestureState state) {}
        List<Entry> active = new ArrayList<>();

        for (ConfigHotkeyedBoolean hb : hotkeyedBooleans) {
            boolean contextMatch = switch (hb.getHotkeyContext()) {
                case IN_GAME  -> inGame;
                case GUI_OPEN -> !inGame;
                case ALWAYS   -> true;
            };
            KeyBind bind = hb.getKeyBind();
            if (!contextMatch || bind == KeyBind.NONE || bind.getKeys().isEmpty()) continue;

            GestureState state = getOrCreateState(hb, bind.getKeys().size());
            tickGesture(state, bind, windowHandle);
            active.add(new Entry(hb, bind, state));
        }

        for (Entry e : active) {
            if (e.state.pendingFire) e.hb.setValue(!e.hb.getValue());
        }
    }

    /**
     * Polls all provider menu keys and returns providers whose menu key fired this tick.
     *
     * <p>Should be called each client tick when no config screen is open.
     */
    public List<IConfigProvider> pollFiredMenuKeys() {
        long windowHandle = Minecraft.getInstance().getWindow().handle();

        List<IConfigProvider> fired = new ArrayList<>();
        for (IConfigProvider provider : providers) {
            KeyBind menuKey = provider.getMenuOpenKey();
            if (menuKey == KeyBind.NONE || menuKey.getKeys().isEmpty()) continue;

            GestureState state = getOrCreateState(provider, menuKey.getKeys().size());
            tickGesture(state, menuKey, windowHandle);
            if (state.pendingFire) fired.add(provider);
        }
        return fired;
    }

    // -------------------------------------------------------------------------
    // Gesture state machine
    // -------------------------------------------------------------------------

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

        // Detect newly pressed keys and advance order tracking
        for (int i = 0; i < n; i++) {
            if (curr[i] && !state.prevHeld[i]) {
                if (i == state.expectedNext) {
                    state.expectedNext++;
                } else {
                    state.orderBroken = true;
                }
            }
        }

        // Fire: all keys became held this tick (transition from not-fully-held), order was valid
        state.pendingFire = fullyHeld && !state.wasFullyHeld && state.orderValid();

        // Reset gesture state when all keys are released
        if (allReleased) {
            state.expectedNext = 0;
            state.orderBroken = false;
        }

        state.wasFullyHeld = fullyHeld;
        state.prevHeld = curr;
    }

}
