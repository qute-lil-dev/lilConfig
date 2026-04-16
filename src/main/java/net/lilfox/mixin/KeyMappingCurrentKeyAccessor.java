package net.lilfox.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the private {@code key} field of {@link KeyMapping}, which holds the
 * player's currently assigned key (as opposed to {@link KeyMapping#getDefaultKey()}
 * which returns the shipped default).
 *
 * <p>Used by {@link net.lilfox.manager.ConfigManager#getConflicts} to detect
 * conflicts between lilConfig hotkeys and vanilla key bindings that the player
 * has not overridden through the VanillaKeybindProvider.
 */
@Mixin(KeyMapping.class)
public interface KeyMappingCurrentKeyAccessor {

    /** Returns the currently assigned key for this mapping. */
    @Accessor("key")
    InputConstants.Key getCurrentKey();
}
