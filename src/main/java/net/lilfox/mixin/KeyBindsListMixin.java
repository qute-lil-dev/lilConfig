package net.lilfox.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.lilfox.LilConfigOwnConfig;
import net.lilfox.hotkey.KeyBind;
import net.lilfox.vanilla.VanillaKeybindProvider;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
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
        KeyBindsScreen screen = ((KeyBindsListAccessor) self).getKeyBindsScreen();
        KeyMapping selectedKey = screen != null ? screen.selectedKey : null;
        for (KeyBindsList.Entry entry : self.children()) {
            if (!(entry instanceof KeyBindsEntryAccessor accessor)) continue;
            // Vanilla shows its own editing indicator ("> KEY <" in yellow) via refreshEntry()
            // when selectedKey == this entry's key; skip it so we don't overwrite that label.
            if (accessor.getKey() == selectedKey) continue;
            KeyBind combo = provider.getComboForMapping(accessor.getKey());
            // No override set — let vanilla's refreshEntry() label (e.g. "None") stand as-is.
            if (combo.getKeys().isEmpty()) continue;
            accessor.getChangeButton().setMessage(Component.literal(combo.toDisplayString()));
            accessor.setHasCollision(provider.hasConflictForMapping(accessor.getKey()));
            InputConstants.Key defaultKey = accessor.getKey().getDefaultKey();
            KeyBind defaultBind = defaultKey.equals(InputConstants.UNKNOWN)
                    ? KeyBind.NONE
                    : KeyBind.of(defaultKey);
            accessor.getResetButton().active = !combo.equals(defaultBind);
        }
    }
}
