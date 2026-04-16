package net.lilfox.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import org.jetbrains.annotations.Nullable;

/**
 * A non-interactive separator entry used to display a section header in the GUI.
 * Carries no value and is never persisted to JSON.
 *
 * <p>{@code name} is the section identifier (e.g. {@code "display"}); together with
 * {@code modId} it forms the i18n key {@code modId.sections.name}.
 */
public final class ConfigSeparator implements IConfig {

    private final String modId;
    private final String name;

    /**
     * Creates a separator with the given mod id and section identifier.
     *
     * @param modId the owning mod id, used to build the i18n key
     * @param name  the section identifier (e.g. {@code "display"})
     */
    public ConfigSeparator(String modId, String name) {
        this.modId = modId;
        this.name = name;
    }

    /** {@inheritDoc} */
    @Override public String getName()                       { return name; }
    /** {@inheritDoc} */
    @Override public String getModId()                      { return modId; }
    /** {@inheritDoc} */
    @Override public ConfigType getType()                   { return ConfigType.SEPARATOR; }
    /** {@inheritDoc} */
    @Override public boolean isModified()                   { return false; }
    /** {@inheritDoc} */
    @Override public void resetToDefault()                  {}
    /** {@inheritDoc} */
    @Override public JsonElement toJson()                   { return JsonNull.INSTANCE; }
    /** {@inheritDoc} */
    @Override public void fromJson(JsonElement element)     {}
    /** {@inheritDoc} */
    @Override public boolean isDirty()                      { return false; }
    /** {@inheritDoc} */
    @Override public void markDirty()                       {}
    /** {@inheritDoc} */
    @Override public void markClean()                       {}
    /** {@inheritDoc} */
    @Override @Nullable public String getEffectButtonLabel() { return null; }
    /** {@inheritDoc} */
    @Override @Nullable public Runnable getEffectAction()    { return null; }
}
