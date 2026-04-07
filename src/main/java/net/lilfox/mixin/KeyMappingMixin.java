package net.lilfox.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.lilfox.LilConfigOwnConfig;
import net.lilfox.hotkey.KeyBind;
import net.lilfox.vanilla.VanillaKeybindProvider;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Intercepts {@link KeyMapping#isDown()} and {@link KeyMapping#consumeClick()} to
 * inject multi-key combo support when the vanilla keybind override is enabled.
 *
 * <p>Both injects are no-ops when the override toggle is off or when
 * {@link VanillaKeybindProvider} has not yet been initialised.
 */
@Mixin(KeyMapping.class)
public abstract class KeyMappingMixin {

    /**
     * When an override combo is configured for this mapping, replaces vanilla's
     * {@code isDown} result entirely with whether the override combo is held.
     * This suppresses the vanilla key so only the configured combo activates the action.
     * Mappings without an override are unaffected.
     */
    @Inject(method = "isDown", at = @At("RETURN"), cancellable = true)
    private void lilconfig_isDown(CallbackInfoReturnable<Boolean> cir) {
        if (!LilConfigOwnConfig.vanillaKeyOverride.getValue()) return;
        VanillaKeybindProvider p = VanillaKeybindProvider.getInstance();
        if (!p.isInitialized()) return;
        KeyBind override = p.getComboForMapping((KeyMapping) (Object) this);
        if (override == KeyBind.NONE || override.getKeys().isEmpty()) return;
        cir.setReturnValue(p.isComboHeld((KeyMapping) (Object) this));
    }

    /**
     * When vanilla resets or rebinds a key via {@code setKey()}, syncs the
     * {@link VanillaKeybindProvider} combo to match the new vanilla key.
     * This ensures clicking the vanilla reset button also clears our override combo.
     */
    @Inject(method = "setKey", at = @At("RETURN"))
    private void lilconfig_setKey(InputConstants.Key key, CallbackInfo ci) {
        if (!LilConfigOwnConfig.vanillaKeyOverride.getValue()) return;
        VanillaKeybindProvider p = VanillaKeybindProvider.getInstance();
        if (!p.isInitialized()) return;
        KeyBind newBind = key.equals(InputConstants.UNKNOWN) ? KeyBind.NONE : KeyBind.of(key);
        p.setComboForMapping((KeyMapping) (Object) this, newBind);
    }

    /**
     * If an override combo is configured for this mapping, bypasses vanilla click
     * consumption entirely and returns whether a pending override click exists.
     */
    @Inject(method = "consumeClick", at = @At("RETURN"), cancellable = true)
    private void lilconfig_consumeClick(CallbackInfoReturnable<Boolean> cir) {
        if (!LilConfigOwnConfig.vanillaKeyOverride.getValue()) return;
        VanillaKeybindProvider p = VanillaKeybindProvider.getInstance();
        if (!p.isInitialized()) return;
        KeyBind override = p.getComboForMapping((KeyMapping) (Object) this);
        if (override == KeyBind.NONE || override.getKeys().isEmpty()) return;
        cir.setReturnValue(p.consumeClick((KeyMapping) (Object) this));
    }
}
