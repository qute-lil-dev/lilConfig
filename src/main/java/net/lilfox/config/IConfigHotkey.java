package net.lilfox.config;

import net.lilfox.hotkey.KeyBind;

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
}
