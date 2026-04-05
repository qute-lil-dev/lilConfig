package net.lilfox.mixin;

import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the private {@code keyBindsScreen} field of {@link KeyBindsList}
 * so that {@link KeyBindsListMixin} can read {@link KeyBindsScreen#selectedKey}
 * and skip the currently-edited entry during label refresh.
 */
@Mixin(KeyBindsList.class)
public interface KeyBindsListAccessor {

    /** Returns the {@link KeyBindsScreen} that owns this list. */
    @Accessor("keyBindsScreen")
    KeyBindsScreen getKeyBindsScreen();
}
