package net.lilfox;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.lilfox.gui.LilConfigScreen;
import net.lilfox.hotkey.MouseButtonTracker;
import net.lilfox.manager.IConfigProvider;
import net.lilfox.manager.LilConfigManager;
import net.lilfox.vanilla.VanillaKeybindProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-side entrypoint for lilConfig.
 *
 * <p>Registers Fabric lifecycle and tick events needed by the library:
 * config persistence on shutdown and hotkey polling for the config screen.
 */
public class LilConfigClient implements ClientModInitializer {

    public static final String MOD_ID = "lilconfig";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LilConfigManager.getInstance().register(LilConfigOwnConfig.class);
        ClientLifecycleEvents.CLIENT_STARTED.register(client ->
                LilConfigManager.getInstance().register(VanillaKeybindProvider.getInstance()));

        ClientLifecycleEvents.CLIENT_STOPPING.register(
                client -> LilConfigManager.getInstance().saveAll());

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            // Check hotkeys BEFORE clearing the sticky mouse-button window.
            // GLFW mouse events are enqueued via Minecraft.execute() and processed
            // before START_CLIENT_TICK fires, so pressedThisTick is already populated
            // here. Clearing first would discard that data before it can be used.
            try {
                if (client.level == null) return;
                if (LilConfigOwnConfig.VANILLA_KEY_OVERRIDE.getValue())
                    VanillaKeybindProvider.getInstance().tick();
                LilConfigManager manager = LilConfigManager.getInstance();
                if (client.screen == null) {
                    for (IConfigProvider provider : manager.pollFiredMenuKeys()) {
                        LilConfigScreen.open(provider);
                        return;
                    }
                }
                manager.tickHotkeys(client.screen);
            } finally {
                // Always clear after all checks, including on early return paths.
                MouseButtonTracker.clearTickWindow();
            }
        });
    }
}
