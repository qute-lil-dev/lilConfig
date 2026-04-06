package net.lilfox.config;

import net.lilfox.hotkey.HotkeyContext;
import net.lilfox.hotkey.IHotkeyCallback;
import net.lilfox.hotkey.KeyBind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;

/**
 * Extension of {@link IConfig} for entries that hold a {@link KeyBind}.
 */
public interface IConfigHotkey extends IConfig {

    /**
     * Returns the current key binding.
     */
    KeyBind getKeyBind();

    /**
     * Sets the current key binding.
     *
     * @param keyBind the new key binding; {@link KeyBind#NONE} disables the shortcut
     */
    void setKeyBind(KeyBind keyBind);

    /**
     * Returns the default key binding.
     */
    KeyBind getDefaultKeyBind();

    /**
     * Returns the context in which this hotkey is active.
     */
    default HotkeyContext getHotkeyContext() { return HotkeyContext.IN_GAME; }

    /**
     * Returns {@code true} if the full key combination is currently held and the hotkey
     * context matches the current game state.
     */
    default boolean isHeld() {
        Screen s = Minecraft.getInstance().screen;
        boolean inGame = (s == null);
        boolean contextMatch = switch (getHotkeyContext()) {
            case IN_GAME  -> inGame;
            case GUI_OPEN -> !inGame;
            case ALWAYS   -> true;
        };
        return contextMatch && getKeyBind().isPressed();
    }

    /**
     * Returns the callback to invoke when this hotkey fires, or {@code null} if none.
     */
    default @Nullable IHotkeyCallback getCallback() { return null; }
}
