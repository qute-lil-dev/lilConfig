package net.lilfox.mixin;

import net.lilfox.LilConfigOwnConfig;
import net.lilfox.vanilla.VanillaKeybindProvider;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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
     * If the vanilla method returns {@code false} and a lilConfig override combo is
     * currently held for this mapping, returns {@code true} instead.
     */
    @Inject(method = "isDown", at = @At("RETURN"), cancellable = true)
    private void lilconfig_isDown(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) return;
        if (!LilConfigOwnConfig.getInstance().getVanillaKeyOverride().getValue()) return;
        VanillaKeybindProvider p = VanillaKeybindProvider.getInstance();
        if (!p.isInitialized()) return;
        if (p.isComboHeld((KeyMapping) (Object) this)) cir.setReturnValue(true);
    }

    /**
     * If the vanilla method returns {@code false} and a pending override click exists
     * for this mapping, consumes it and returns {@code true} instead.
     */
    @Inject(method = "consumeClick", at = @At("RETURN"), cancellable = true)
    private void lilconfig_consumeClick(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) return;
        if (!LilConfigOwnConfig.getInstance().getVanillaKeyOverride().getValue()) return;
        VanillaKeybindProvider p = VanillaKeybindProvider.getInstance();
        if (!p.isInitialized()) return;
        if (p.consumeClick((KeyMapping) (Object) this)) cir.setReturnValue(true);
    }
}
