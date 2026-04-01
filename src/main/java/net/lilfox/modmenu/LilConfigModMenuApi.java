package net.lilfox.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.lilfox.LilConfigOwnConfig;
import net.lilfox.gui.LilConfigScreen;

/**
 * ModMenu integration entrypoint for lilConfig.
 *
 * <p>Provides the config screen factory so ModMenu can open the lilConfig
 * settings screen from its mod list. This entrypoint is only invoked when
 * ModMenu is present at runtime; if it is absent, Fabric simply ignores the
 * {@code modmenu} entrypoint entry.
 */
public class LilConfigModMenuApi implements ModMenuApi {

    /** {@inheritDoc} */
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> LilConfigScreen.create(parent, LilConfigOwnConfig.getInstance());
    }
}
