package ic2.api.item;

import ic2.api.info.Info;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.ItemStack;

public final class IC2Items {
    private static IItemAPI instance;

    public static ItemStack getItem(String name, String variant) {
        return instance == null ? null : instance.getItemStack(name, variant);
    }

    public static ItemStack getItem(String name) {
        return getItem(name, null);
    }

    public static IItemAPI getItemAPI() { return instance; }

    public static void setInstance(IItemAPI api) {
        if (FabricLoader.getInstance().isModLoaded(Info.MOD_ID)) {
            instance = api;
        } else {
            throw new IllegalAccessError("Only IC2 can set the instance");
        }
    }
}
