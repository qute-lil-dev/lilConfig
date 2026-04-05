package net.lilfox.mixin;

import net.lilfox.LilConfigOwnConfig;
import net.lilfox.gui.LilConfigScreen;
import net.lilfox.vanilla.VanillaKeybindProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Redirects {@link KeyBindsScreen} to the lilConfig vanilla keybind screen
 * when the vanilla keybind override is active.
 *
 * <p>The injection fires at {@code HEAD} before {@code Minecraft.screen} is
 * replaced, so the current screen is captured as the parent for the replacement.
 * Cancelling the original call and immediately issuing a new {@code setScreen}
 * for {@link LilConfigScreen} means the full {@code setScreen} logic still
 * runs — just for the lilConfig screen rather than the vanilla one.
 */
@Mixin(Minecraft.class)
public class ScreenRedirectMixin {

    /**
     * If the incoming screen is a {@link KeyBindsScreen} and the vanilla keybind override
     * is enabled, decides which variant to use:
     * <ul>
     *   <li>Variant B ({@code vanilla_ui_embed=false}): cancels and opens a flat
     *       {@link LilConfigScreen} via {@link VanillaKeybindProvider#asFlatProvider()}.</li>
     *   <li>Variant C ({@code vanilla_ui_embed=true}): lets the original {@link KeyBindsScreen}
     *       open; key input is intercepted by {@code KeyBindsScreenMixin}.</li>
     * </ul>
     */
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void lilconfig_redirectKeyBindsScreen(@Nullable Screen screen, CallbackInfo ci) {
        if (!(screen instanceof KeyBindsScreen)) return;
        if (!LilConfigOwnConfig.getInstance().getVanillaKeyOverride().getValue()) return;
        if (LilConfigOwnConfig.getInstance().getVanillaUiEmbed().getValue()) return; // Variant C
        VanillaKeybindProvider p = VanillaKeybindProvider.getInstance();
        if (!p.isInitialized()) return;
        Screen parent = ((Minecraft) (Object) this).screen;
        ci.cancel();
        ((Minecraft) (Object) this).setScreen(LilConfigScreen.create(parent, p.asFlatProvider()));
    }
}
