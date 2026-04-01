package net.lilfox.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.lilfox.hotkey.KeyBind;

/**
 * A configuration entry that combines a boolean toggle with a configurable hotkey.
 *
 * <p>When the hotkey is pressed in-game, the boolean value is toggled.
 * Both the boolean value and the key binding are managed and reset independently;
 * {@link #isModified()} returns {@code true} if either differs from its default.
 *
 * <p>JSON representation: an object with {@code "value"} (boolean) and
 * {@code "key"} (save string) fields. A plain boolean primitive is also accepted
 * for backwards compatibility (key binding is left at its current value).
 */
public class ConfigHotkeyedBoolean extends ConfigBase<Boolean>
        implements IConfigBoolean, IConfigHotkey {

    /**
     * Controls when the hotkey is active.
     *
     * <ul>
     *   <li>{@link #IN_GAME} — only while no screen is open (default)</li>
     *   <li>{@link #GUI_OPEN} — only while any screen is open</li>
     *   <li>{@link #ALWAYS} — regardless of screen state</li>
     * </ul>
     */
    public enum HotkeyContext { IN_GAME, GUI_OPEN, ALWAYS }

    private KeyBind keyBind;
    private final KeyBind defaultKeyBind;
    private HotkeyContext hotkeyContext = HotkeyContext.IN_GAME;

    /**
     * Creates a new hotkeyed boolean config entry.
     *
     * @param name           the unique name used as JSON key and i18n base key
     * @param defaultValue   the default boolean value
     * @param defaultKeyBind the default key binding; use {@link KeyBind#NONE} for unbound
     */
    public ConfigHotkeyedBoolean(String name, boolean defaultValue, KeyBind defaultKeyBind) {
        super(name, defaultValue);
        this.defaultKeyBind = defaultKeyBind != null ? defaultKeyBind : KeyBind.NONE;
        this.keyBind = this.defaultKeyBind;
    }

    /**
     * Creates a new hotkeyed boolean config entry from a comma-separated key name string.
     *
     * @param name         the unique name used as JSON key and i18n base key
     * @param defaultValue the default boolean value
     * @param keyNames     comma-separated GLFW key names, e.g. {@code "LEFT_CONTROL, O"}
     * @see KeyBind#parse(String)
     */
    public ConfigHotkeyedBoolean(String name, boolean defaultValue, String keyNames) {
        this(name, defaultValue, KeyBind.parse(keyNames));
    }

    @Override
    public ConfigType getType() {
        return ConfigType.HOTKEYED_BOOLEAN;
    }

    @Override
    public boolean getValue() {
        return value;
    }

    @Override
    public void setValue(boolean value) {
        this.value = value;
    }

    @Override
    public boolean getDefaultValue() {
        return defaultValue;
    }

    @Override
    public KeyBind getKeyBind() {
        return keyBind;
    }

    @Override
    public void setKeyBind(KeyBind keyBind) {
        this.keyBind = keyBind != null ? keyBind : KeyBind.NONE;
    }

    @Override
    public KeyBind getDefaultKeyBind() {
        return defaultKeyBind;
    }

    /**
     * Returns the context in which this hotkey is active.
     */
    public HotkeyContext getHotkeyContext() {
        return hotkeyContext;
    }

    /**
     * Sets the context in which this hotkey is active.
     *
     * @param ctx the desired context
     * @return this instance for fluent chaining
     */
    public ConfigHotkeyedBoolean withHotkeyContext(HotkeyContext ctx) {
        this.hotkeyContext = ctx;
        return this;
    }

    /**
     * Returns {@code true} if either the boolean value or the key binding
     * differs from its respective default.
     */
    @Override
    public boolean isModified() {
        return super.isModified() || !keyBind.equals(defaultKeyBind);
    }

    /**
     * Resets both the boolean value and the key binding to their defaults.
     */
    @Override
    public void resetToDefault() {
        super.resetToDefault();
        this.keyBind = defaultKeyBind;
    }

    @Override
    public JsonElement toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("value", value);
        obj.addProperty("key", keyBind.toSaveString());
        return obj;
    }

    @Override
    public void fromJson(JsonElement element) {
        try {
            if (element.isJsonObject()) {
                JsonObject obj = element.getAsJsonObject();
                if (obj.has("value")) this.value = obj.get("value").getAsBoolean();
                if (obj.has("key"))   this.keyBind = KeyBind.fromSaveString(obj.get("key").getAsString());
            } else {
                this.value = element.getAsBoolean();
            }
        } catch (Exception ignored) {}
    }

    /**
     * {@inheritDoc}
     *
     * @return this {@code ConfigHotkeyedBoolean} for fluent chaining
     */
    @Override
    public ConfigHotkeyedBoolean withEffectButton(String labelKey, Runnable action) {
        super.withEffectButton(labelKey, action);
        return this;
    }
}
