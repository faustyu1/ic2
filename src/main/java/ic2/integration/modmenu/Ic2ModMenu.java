package ic2.integration.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Mod Menu's config button. The settings screen needs Cloth Config; without it there is no
 * button and the settings are edited in {@code config/IC2.ini}.
 */
public final class Ic2ModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (!FabricLoader.getInstance().isModLoaded("cloth-config")) {
            return parent -> null;
        }
        return Ic2ConfigScreen::create;
    }
}
