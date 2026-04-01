package net.lilfox.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.lilfox.hotkey.KeyBind;

/**
 * A standalone hotkey configuration entry backed by a rebind button in the GUI.
 *
 * <p>JSON representation: the save string produced by {@link KeyBind#toSaveString()}.
 */
public class ConfigHotkey extends ConfigBase<KeyBind> implements IConfigHotkey {

    /**
     * Creates a new standalone hotkey config entry.
     *
     * @param name       the unique name used as JSON key and i18n base key
     * @param defaultKey the default key binding; use {@link KeyBind#NONE} for unbound
     */
    public ConfigHotkey(String name, KeyBind defaultKey) {
        super(name, defaultKey != null ? defaultKey : KeyBind.NONE);
    }

    /**
     * Creates a new standalone hotkey config entry from a comma-separated key name string.
     *
     * @param name     the unique name used as JSON key and i18n base key
     * @param keyNames comma-separated GLFW key names, e.g. {@code "LEFT_CONTROL, L"}
     * @see KeyBind#parse(String)
     */
    public ConfigHotkey(String name, String keyNames) {
        this(name, KeyBind.parse(keyNames));
    }

    /** {@inheritDoc} */
    @Override
    public ConfigType getType() {
        return ConfigType.HOTKEY;
    }

    /** {@inheritDoc} */
    @Override
    public KeyBind getKeyBind() {
        return value;
    }

    /** {@inheritDoc} */
    @Override
    public void setKeyBind(KeyBind keyBind) {
        this.value = keyBind != null ? keyBind : KeyBind.NONE;
    }

    /** {@inheritDoc} */
    @Override
    public KeyBind getDefaultKeyBind() {
        return defaultValue;
    }

    /** {@inheritDoc} */
    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(value.toSaveString());
    }

    /** {@inheritDoc} */
    @Override
    public void fromJson(JsonElement element) {
        try {
            setKeyBind(KeyBind.fromSaveString(element.getAsString()));
        } catch (Exception ignored) {}
    }

    /**
     * {@inheritDoc}
     *
     * @return this {@code ConfigHotkey} for fluent chaining
     */
    @Override
    public ConfigHotkey withEffectButton(String labelKey, Runnable action) {
        super.withEffectButton(labelKey, action);
        return this;
    }
}
