package net.lilfox.hotkey;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable representation of a keyboard shortcut consisting of up to
 * {@value #MAX_KEYS} simultaneous keys. Any key — including modifier keys such
 * as Ctrl or Shift — may participate as an equal member of the combination.
 *
 * <p>KeyBind deliberately does not use Minecraft's {@code KeyMapping} system
 * and will not appear in the vanilla Controls screen.
 *
 * <p>Use {@link #NONE} to represent an unbound (disabled) shortcut.
 */
public final class KeyBind {

    /** Maximum number of keys in a single combination. */
    public static final int MAX_KEYS = 4;

    /** A KeyBind representing no key assignment (unbound / disabled). */
    public static final KeyBind NONE = new KeyBind(List.of());

    /**
     * Maps GLFW key codes to their English constant names (e.g. 65 → "A",
     * 341 → "LEFT CONTROL"). Built once via reflection so names are always
     * in English regardless of the active keyboard layout or game language.
     */
    private static final Map<Integer, String> GLFW_KEY_NAMES = buildGlfwKeyNames();

    private static Map<Integer, String> buildGlfwKeyNames() {
        Map<Integer, String> map = new HashMap<>();
        for (java.lang.reflect.Field f : GLFW.class.getFields()) {
            String name = f.getName();
            if (!name.startsWith("GLFW_KEY_")) continue;
            try {
                int code = f.getInt(null);
                if (code >= 0) map.putIfAbsent(code, name.substring(9).replace('_', ' '));
            } catch (IllegalAccessException ignored) {}
        }
        return Collections.unmodifiableMap(map);
    }

    private final List<InputConstants.Key> keys;

    /**
     * Creates a new KeyBind from an ordered list of keys.
     * If more than {@value #MAX_KEYS} keys are supplied the excess is silently discarded.
     *
     * @param keys the keys that must be held simultaneously; may be empty
     */
    public KeyBind(List<InputConstants.Key> keys) {
        int count = Math.min(keys.size(), MAX_KEYS);
        this.keys = List.copyOf(keys.subList(0, count));
    }

    /**
     * Creates a KeyBind from a vararg list of {@link InputConstants.Key} objects.
     * Excess keys beyond {@value #MAX_KEYS} are discarded.
     *
     * @param keys the keys that must be held simultaneously
     * @return a new KeyBind
     */
    public static KeyBind of(InputConstants.Key... keys) {
        int count = Math.min(keys.length, MAX_KEYS);
        return new KeyBind(Arrays.asList(keys).subList(0, count));
    }

    /**
     * Creates a KeyBind from GLFW key codes. Convenience factory for static
     * default values declared at class-load time.
     * Excess codes beyond {@value #MAX_KEYS} are discarded; {@code GLFW_KEY_UNKNOWN} entries
     * are skipped.
     *
     * <p>Example: {@code KeyBind.ofGlfw(GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_G)}
     *
     * @param glfwKeyCodes the GLFW key constants to include
     * @return a new KeyBind
     */
    public static KeyBind ofGlfw(int... glfwKeyCodes) {
        List<InputConstants.Key> list = new ArrayList<>();
        for (int code : glfwKeyCodes) {
            if (code == GLFW.GLFW_KEY_UNKNOWN) continue;
            list.add(InputConstants.Type.KEYSYM.getOrCreate(code));
            if (list.size() >= MAX_KEYS) break;
        }
        return new KeyBind(list);
    }

    /**
     * Returns an unmodifiable ordered list of the keys in this combination.
     */
    public List<InputConstants.Key> getKeys() {
        return keys;
    }

    /**
     * Returns {@code true} if the given key is currently held, supporting both
     * keyboard ({@link InputConstants.Type#KEYSYM}) and mouse button
     * ({@link InputConstants.Type#MOUSE}) keys.
     *
     * @param key          the key to query
     * @param windowHandle the native GLFW window handle
     * @return {@code true} if the key is currently pressed
     */
    public static boolean isKeyHeld(InputConstants.Key key, long windowHandle) {
        if (key.getType() == InputConstants.Type.MOUSE) {
            int btn = key.getValue();
            return MouseButtonTracker.isHeldOrPressedThisTick(btn)
                    || GLFW.glfwGetMouseButton(windowHandle, btn) == GLFW.GLFW_PRESS;
        }
        return GLFW.glfwGetKey(windowHandle, key.getValue()) == GLFW.GLFW_PRESS;
    }

    /**
     * Returns {@code true} if every key in this combination is currently held.
     * Supports both keyboard and mouse button keys.
     * Always returns {@code false} for {@link #NONE}.
     */
    public boolean isPressed() {
        if (keys.isEmpty()) return false;
        long windowHandle = Minecraft.getInstance().getWindow().handle();
        for (InputConstants.Key key : keys) {
            if (!isKeyHeld(key, windowHandle)) return false;
        }
        return true;
    }

    /**
     * Returns a new KeyBind with {@code key} appended to the combination.
     * Returns {@code this} unchanged if {@code key} is already present
     * or the {@value #MAX_KEYS}-key limit would be exceeded.
     *
     * @param key the key to add
     * @return the updated KeyBind, or {@code this} if unchanged
     */
    public KeyBind withKey(InputConstants.Key key) {
        if (keys.contains(key) || keys.size() >= MAX_KEYS) return this;
        List<InputConstants.Key> newKeys = new ArrayList<>(keys);
        newKeys.add(key);
        return new KeyBind(newKeys);
    }

    /**
     * Serialises this KeyBind to a save string such as
     * {@code "KEYSYM:341+KEYSYM:71"} (Left Ctrl + G).
     * Returns {@code "none"} for {@link #NONE}.
     *
     * @return the save string
     */
    public String toSaveString() {
        if (keys.isEmpty()) return "none";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keys.size(); i++) {
            if (i > 0) sb.append('+');
            InputConstants.Key k = keys.get(i);
            sb.append(k.getType().name()).append(':').append(k.getValue());
        }
        return sb.toString();
    }

    /**
     * Returns a concise human-readable display string such as
     * {@code "LEFT CONTROL+G"} or {@code "NONE"}.
     * Names are always in English regardless of the active keyboard layout
     * or game language, derived from GLFW constant names.
     *
     * @return the display string
     */
    public String toDisplayString() {
        if (keys.isEmpty()) return "NONE";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keys.size(); i++) {
            if (i > 0) sb.append(" + ");
            sb.append(keyDisplayName(keys.get(i)));
        }
        return sb.toString();
    }

    private static String keyDisplayName(InputConstants.Key key) {
        if (key.getType() == InputConstants.Type.MOUSE) {
            return switch (key.getValue()) {
                case GLFW.GLFW_MOUSE_BUTTON_LEFT   -> "MOUSE LEFT";
                case GLFW.GLFW_MOUSE_BUTTON_RIGHT  -> "MOUSE RIGHT";
                case GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> "MOUSE MIDDLE";
                default -> "MOUSE " + (key.getValue() + 1);
            };
        }
        String name = GLFW_KEY_NAMES.get(key.getValue());
        return name != null ? name : key.getType().name() + " " + key.getValue();
    }

    /**
     * Parses a save string produced by {@link #toSaveString()}.
     * Returns {@link #NONE} if the input is {@code "none"}, blank, or malformed.
     *
     * @param saveString the string to parse
     * @return the parsed KeyBind, or {@link #NONE} on failure
     */
    public static KeyBind fromSaveString(String saveString) {
        if (saveString == null || saveString.isBlank() || saveString.equalsIgnoreCase("none")) {
            return NONE;
        }
        try {
            String[] parts = saveString.split("\\+");
            List<InputConstants.Key> list = new ArrayList<>();
            for (String part : parts) {
                if (list.size() >= MAX_KEYS) break;
                String[] typeAndCode = part.split(":");
                InputConstants.Type type = InputConstants.Type.valueOf(typeAndCode[0]);
                int code = Integer.parseInt(typeAndCode[1]);
                list.add(type.getOrCreate(code));
            }
            return new KeyBind(list);
        } catch (Exception e) {
            return NONE;
        }
    }

    /**
     * Parses a comma-separated list of GLFW key names such as {@code "LEFT_CONTROL, L"}
     * into a KeyBind. Each token is matched to a {@code GLFW.GLFW_KEY_<TOKEN>} constant
     * via reflection (case-insensitive, spaces treated as underscores).
     * Returns {@link #NONE} if any token is unrecognised or the string is blank.
     *
     * <p>Example: {@code KeyBind.parse("LEFT_CONTROL, L")}
     *
     * @param names comma-separated GLFW key names
     * @return the parsed KeyBind, or {@link #NONE} on failure
     */
    public static KeyBind parse(String names) {
        if (names == null || names.isBlank()) return NONE;
        String[] tokens = names.split(",");
        int count = Math.min(tokens.length, MAX_KEYS);
        int[] codes = new int[count];
        for (int i = 0; i < count; i++) {
            String token = tokens[i].trim().toUpperCase().replace(' ', '_');
            try {
                java.lang.reflect.Field f = GLFW.class.getField("GLFW_KEY_" + token);
                codes[i] = f.getInt(null);
            } catch (ReflectiveOperationException e) {
                return NONE;
            }
        }
        return ofGlfw(codes);
    }

    /**
     * Creates a single-key KeyBind from a {@code keyPressed} Screen event.
     * Used by the GUI hotkey widget to start building a new key combination.
     * Returns {@link #NONE} for unknown key codes.
     *
     * @param keyCode  the GLFW key code
     * @param scanCode the platform scan code (unused, reserved for future use)
     * @return a single-key KeyBind, or {@link #NONE}
     */
    public static KeyBind fromKeyEvent(int keyCode, int scanCode) {
        if (keyCode == GLFW.GLFW_KEY_UNKNOWN) return NONE;
        return of(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
    }

    /**
     * Two KeyBinds are equal when they contain the same keys regardless of insertion order.
     * This relies on {@link InputConstants.Type#getOrCreate(int)} returning cached instances,
     * making reference equality valid for {@link InputConstants.Key} objects.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof KeyBind other)) return false;
        if (keys.size() != other.keys.size()) return false;
        for (InputConstants.Key k : keys) {
            if (!other.keys.contains(k)) return false;
        }
        return true;
    }

    /**
     * Order-independent hash code consistent with {@link #equals}.
     */
    @Override
    public int hashCode() {
        int h = 0;
        for (InputConstants.Key k : keys) h += Objects.hashCode(k);
        return h;
    }

    @Override
    public String toString() {
        return toDisplayString();
    }
}
