package net.lilfox.hotkey;

import org.lwjgl.glfw.GLFW;

/**
 * Tracks mouse button presses with sticky per-tick semantics.
 *
 * <p>Bridges the gap between the 20 TPS tick rate and short mouse clicks:
 * a button is considered "held" for the entire tick window if it was pressed
 * at any point since the last {@link #clearTickWindow()} call.
 *
 * <p>All methods must be called on the main client thread.
 */
public final class MouseButtonTracker {

    /** GLFW supports mouse buttons 0–7. */
    private static final int MAX_BUTTONS = 8;

    private static final boolean[] heldNow        = new boolean[MAX_BUTTONS];
    private static final boolean[] pressedThisTick = new boolean[MAX_BUTTONS];

    private MouseButtonTracker() {}

    /**
     * Called by the mixin on every raw GLFW mouse button event.
     * {@code action} uses GLFW conventions: 1=PRESS, 0=RELEASE.
     */
    public static void onMouseButton(int button, int action) {
        if (button < 0 || button >= MAX_BUTTONS) return;
        boolean pressed = (action == GLFW.GLFW_PRESS);
        heldNow[button] = pressed;
        if (pressed) pressedThisTick[button] = true;
    }

    /**
     * Clears the per-tick sticky window.
     * Must be called at the START of each client tick, before hotkey detection.
     */
    public static void clearTickWindow() {
        for (int i = 0; i < MAX_BUTTONS; i++) {
            pressedThisTick[i] = false;
        }
    }

    /**
     * Returns {@code true} if the button is currently held OR was pressed
     * at any point since the last {@link #clearTickWindow()} call.
     *
     * @param button GLFW mouse button index (e.g. {@code GLFW.GLFW_MOUSE_BUTTON_LEFT} = 0)
     */
    public static boolean isHeldOrPressedThisTick(int button) {
        if (button < 0 || button >= MAX_BUTTONS) return false;
        return heldNow[button] || pressedThisTick[button];
    }
}
