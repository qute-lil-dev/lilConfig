package net.lilfox.mixin;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the private {@code key} and {@code changeButton} fields of
 * {@link KeyBindsList.KeyEntry} for use in lilConfig's screen mixins.
 */
@Mixin(KeyBindsList.KeyEntry.class)
public interface KeyBindsEntryAccessor {

    /** Returns the vanilla key mapping this entry represents. */
    @Accessor("key")
    KeyMapping getKey();

    /** Returns the "change binding" button in this entry's row. */
    @Accessor("changeButton")
    Button getChangeButton();

    /** Returns the "reset to default" button in this entry's row. */
    @Accessor("resetButton")
    Button getResetButton();

    /** Sets the conflict flag that drives the vanilla yellow-stripe indicator. */
    @Accessor("hasCollision")
    void setHasCollision(boolean value);
}
