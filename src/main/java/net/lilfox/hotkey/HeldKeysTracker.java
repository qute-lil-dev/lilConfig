package net.lilfox.hotkey;

import java.util.HashSet;
import java.util.Set;

/**
 * Tracks which keyboard keys are currently held, updated by a mixin on
 * {@link net.minecraft.client.KeyboardHandler} before any Minecraft routing.
 *
 * <p>All methods must be called on the main client thread.
 */
public final class HeldKeysTracker {

    private static final Set<Integer> held = new HashSet<>();

    private HeldKeysTracker() {}

    /** Called on GLFW_PRESS. */
    public static void onPress(int key) {
        held.add(key);
    }

    /** Called on GLFW_RELEASE. */
    public static void onRelease(int key) {
        held.remove(key);
    }

    /** Returns the live set of currently held key codes. */
    public static Set<Integer> held() {
        return held;
    }
}
