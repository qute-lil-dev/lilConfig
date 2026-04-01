package net.lilfox;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.lilfox.gui.LilConfigScreen;
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
        LilConfigManager.getInstance().register(LilConfigOwnConfig.getInstance());
        ClientLifecycleEvents.CLIENT_STARTED.register(client ->
                LilConfigManager.getInstance().register(VanillaKeybindProvider.getInstance()));

        ClientLifecycleEvents.CLIENT_STOPPING.register(
                client -> LilConfigManager.getInstance().saveAll());

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.level == null) return;
            if (LilConfigOwnConfig.getInstance().getVanillaKeyOverride().getValue())
                VanillaKeybindProvider.getInstance().tick();
            LilConfigManager manager = LilConfigManager.getInstance();
            if (client.screen == null) {
                for (IConfigProvider provider : manager.pollFiredMenuKeys()) {
                    LilConfigScreen.open(provider);
                    return;
                }
            }
            manager.tickHotkeys(client.screen);
        });
    }
}
