package net.lilfox.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import org.jetbrains.annotations.Nullable;

/**
 * A non-interactive separator entry used to display a section header in the GUI.
 * Carries no value and is never persisted to JSON.
 *
 * <p>The {@code name} is used as a translation key for the displayed label.
 */
public final class ConfigSeparator implements IConfig {

    private final String name;

    /**
     * Creates a separator with the given translation key as its label.
     *
     * @param translationKey the i18n key for the section header text
     */
    public ConfigSeparator(String translationKey) {
        this.name = translationKey;
    }

    @Override public String getName()                       { return name; }
    @Override public ConfigType getType()                   { return ConfigType.SEPARATOR; }
    @Override public boolean isModified()                   { return false; }
    @Override public void resetToDefault()                  {}
    @Override public JsonElement toJson()                   { return JsonNull.INSTANCE; }
    @Override public void fromJson(JsonElement element)     {}
    @Override public boolean isDirty()                      { return false; }
    @Override public void markDirty()                       {}
    @Override public void markClean()                       {}
    @Override @Nullable public String getEffectButtonLabel() { return null; }
    @Override @Nullable public Runnable getEffectAction()    { return null; }
}
