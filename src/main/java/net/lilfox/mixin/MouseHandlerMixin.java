package net.lilfox.mixin;

import net.lilfox.hotkey.MouseButtonTracker;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts raw mouse button events at the earliest safe point on the main
 * thread ({@code MouseHandler.onButton}) to populate {@link MouseButtonTracker}.
 *
 * <p>Fires for ALL mouse buttons, in-game and in GUI, before screen dispatch.
 * Mouse buttons never produce GLFW_REPEAT, so only PRESS (1) and RELEASE (0)
 * are expected.
 */
@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @Inject(
        method = "onButton(JLnet/minecraft/client/input/MouseButtonInfo;I)V",
        at = @At("HEAD")
    )
    private void lilconfig_onButton(long handle,
                                    MouseButtonInfo rawButtonInfo,
                                    int action,
                                    CallbackInfo ci) {
        MouseButtonTracker.onMouseButton(rawButtonInfo.button(), action);
    }
}
