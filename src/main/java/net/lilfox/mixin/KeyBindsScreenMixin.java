package net.lilfox.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.lilfox.LilConfigOwnConfig;
import net.lilfox.hotkey.KeyBind;
import net.lilfox.vanilla.VanillaKeybindProvider;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Intercepts key and mouse input on {@link KeyBindsScreen} when the vanilla
 * keybind embed mode (Variant C) is active, accumulating a multi-key combo
 * instead of binding a single key as vanilla does.
 */
@Mixin(KeyBindsScreen.class)
public class KeyBindsScreenMixin {

    @Shadow @Nullable KeyMapping selectedKey;
    @Shadow long lastKeySelection;
    @Shadow KeyBindsList keyBindsList;

    /** The combo being built during an active rebind session. */
    @Unique private KeyBind lilconfig_pendingBind = KeyBind.NONE;

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void lilconfig_keyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (!LilConfigOwnConfig.vanillaKeyOverride.getValue()) return;
        if (!LilConfigOwnConfig.vanillaUiEmbed.getValue()) return;
        if (selectedKey == null) return;

        int key = event.key();
        boolean isFinish = key == GLFW.GLFW_KEY_ESCAPE
                || key == GLFW.GLFW_KEY_ENTER
                || key == GLFW.GLFW_KEY_KP_ENTER;

        if (isFinish) {
            KeyBind result = lilconfig_pendingBind.getKeys().isEmpty()
                    ? KeyBind.NONE : lilconfig_pendingBind;
            VanillaKeybindProvider.getInstance().setComboForMapping(selectedKey, result);
            lilconfig_pendingBind = KeyBind.NONE;
            selectedKey = null;
            lastKeySelection = Util.getMillis();
            keyBindsList.resetMappingAndUpdateButtons();
        } else {
            InputConstants.Key inputKey = InputConstants.Type.KEYSYM.getOrCreate(key);
            lilconfig_pendingBind = lilconfig_pendingBind.withKey(inputKey);
            lilconfig_setButtonLabel(selectedKey,
                    lilconfig_pendingBind.toDisplayString() + " ...");
        }
        cir.setReturnValue(true);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void lilconfig_mouseClicked(MouseButtonEvent event, boolean doubleClick,
                                        CallbackInfoReturnable<Boolean> cir) {
        if (!LilConfigOwnConfig.vanillaKeyOverride.getValue()) return;
        if (!LilConfigOwnConfig.vanillaUiEmbed.getValue()) return;
        if (selectedKey == null) return;

        Button currentButton = lilconfig_findChangeButton(selectedKey);
        if (currentButton == null || !currentButton.isHovered()) {
            // Click outside the active rebind button — commit and stop rebinding
            KeyBind result = lilconfig_pendingBind.getKeys().isEmpty()
                    ? KeyBind.NONE : lilconfig_pendingBind;
            VanillaKeybindProvider.getInstance().setComboForMapping(selectedKey, result);
            lilconfig_pendingBind = KeyBind.NONE;
            selectedKey = null;
            lastKeySelection = Util.getMillis();
            keyBindsList.resetMappingAndUpdateButtons();
            // Do not cancel — let vanilla handle whatever was clicked
            return;
        }

        // Click on the active rebind button — add mouse button to combo
        InputConstants.Key mouseKey = InputConstants.Type.MOUSE.getOrCreate(event.button());
        lilconfig_pendingBind = lilconfig_pendingBind.withKey(mouseKey);
        lilconfig_setButtonLabel(selectedKey,
                lilconfig_pendingBind.toDisplayString() + " ...");
        cir.setReturnValue(true);
    }

    @Unique
    private @Nullable Button lilconfig_findChangeButton(KeyMapping km) {
        for (KeyBindsList.Entry entry : keyBindsList.children()) {
            if (entry instanceof KeyBindsEntryAccessor accessor && accessor.getKey() == km) {
                return accessor.getChangeButton();
            }
        }
        return null;
    }

    @Unique
    private void lilconfig_setButtonLabel(KeyMapping km, String label) {
        Button btn = lilconfig_findChangeButton(km);
        if (btn != null) btn.setMessage(Component.literal(label));
    }
}
