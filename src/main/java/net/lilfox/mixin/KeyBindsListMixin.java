package net.lilfox.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.lilfox.LilConfigOwnConfig;
import net.lilfox.hotkey.KeyBind;
import net.lilfox.vanilla.VanillaKeybindProvider;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Overrides button labels with lilConfig combo strings when the vanilla keybind
 * embed mode (Variant C) is active.
 *
 * <p>Labels are applied both on initial list construction and after each
 * {@code resetMappingAndUpdateButtons} call (which vanilla uses to refresh labels).
 */
@Mixin(KeyBindsList.class)
public class KeyBindsListMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void lilconfig_onInit(CallbackInfo ci) {
        lilconfig_applyOverrideLabels();
    }

    @Inject(method = "resetMappingAndUpdateButtons", at = @At("TAIL"))
    private void lilconfig_afterReset(CallbackInfo ci) {
        lilconfig_applyOverrideLabels();
    }

    @Unique
    private void lilconfig_applyOverrideLabels() {
        if (!LilConfigOwnConfig.getInstance().getVanillaKeyOverride().getValue()) return;
        if (!LilConfigOwnConfig.getInstance().getVanillaUiEmbed().getValue()) return;
        VanillaKeybindProvider provider = VanillaKeybindProvider.getInstance();
        if (!provider.isInitialized()) return;
        KeyBindsList self = (KeyBindsList) (Object) this;
        for (KeyBindsList.Entry entry : self.children()) {
            if (!(entry instanceof KeyBindsEntryAccessor accessor)) continue;
            KeyBind combo = provider.getComboForMapping(accessor.getKey());
            String label = combo.getKeys().isEmpty() ? "---" : combo.toDisplayString();
            accessor.getChangeButton().setMessage(Component.literal(label));
            boolean hasConflict = !combo.getKeys().isEmpty()
                    && provider.hasConflictForMapping(accessor.getKey());
            accessor.setHasCollision(hasConflict);
            InputConstants.Key defaultKey = accessor.getKey().getDefaultKey();
            KeyBind defaultBind = defaultKey.equals(InputConstants.UNKNOWN)
                    ? KeyBind.NONE
                    : KeyBind.of(defaultKey);
            accessor.getResetButton().active = !combo.equals(defaultBind);
        }
    }
}
