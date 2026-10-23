package ic2.api.tile;

import ic2.api.info.Info;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.entity.BlockEntity;

public class RotorRegistry {
    private static IRotorRegistry INSTANCE;

    public static <T extends BlockEntity & IRotorProvider> void registerRotorProvider(Class<T> clazz) {
        if (INSTANCE != null) {
            INSTANCE.registerRotorProvider(clazz);
        }
    }

    public static void setInstance(IRotorRegistry instance) {
        if (FabricLoader.getInstance().isModLoaded(Info.MOD_ID)) {
            INSTANCE = instance;
        } else {
            throw new IllegalAccessError("Only IC2 can set the instance");
        }
    }

    public interface IRotorRegistry {
        <T extends BlockEntity & IRotorProvider> void registerRotorProvider(Class<T> clazz);
    }
}
