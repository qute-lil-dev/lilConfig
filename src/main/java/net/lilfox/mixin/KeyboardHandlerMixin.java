package net.lilfox.mixin;

import net.lilfox.hotkey.HeldKeysTracker;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Maintains {@link HeldKeysTracker} at the lowest available interception point,
 * before any Minecraft screen routing or event filtering.
 */
@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {

    @Inject(method = "keyPress(JILnet/minecraft/client/input/KeyEvent;)V", at = @At("HEAD"))
    private void lilconfig_keyPress(long handle, int action, KeyEvent event, CallbackInfo ci) {
        if (action == GLFW.GLFW_PRESS) {
            HeldKeysTracker.onPress(event.key());
        } else if (action == GLFW.GLFW_RELEASE) {
            HeldKeysTracker.onRelease(event.key());
        }
    }
}
