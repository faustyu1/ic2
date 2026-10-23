package ic2.api.network;

import ic2.api.info.Info;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public final class NetworkHelper {
    private static INetworkManager serverInstance;
    private static INetworkManager clientInstance;

    public static void updateTileEntityField(BlockEntity tileEntity, String field) {
        getNetworkManager(tileEntity.getWorld() != null && tileEntity.getWorld().isClient).updateTileEntityField(tileEntity, field);
    }

    public static void initiateTileEntityEvent(BlockEntity tileEntity, int event, boolean limitRange) {
        getNetworkManager(tileEntity.getWorld() != null && tileEntity.getWorld().isClient).initiateTileEntityEvent(tileEntity, event, limitRange);
    }

    public static void initiateItemEvent(PlayerEntity player, ItemStack stack, int event, boolean limitRange) {
        getNetworkManager(player.getWorld().isClient).initiateItemEvent(player, stack, event, limitRange);
    }

    public static void sendInitialData(BlockEntity tileEntity) {
        getNetworkManager(tileEntity.getWorld() != null && tileEntity.getWorld().isClient).sendInitialData(tileEntity);
    }

    public static void initiateClientTileEntityEvent(BlockEntity tileEntity, int event) {
        getNetworkManager(tileEntity.getWorld() != null && tileEntity.getWorld().isClient).initiateClientTileEntityEvent(tileEntity, event);
    }

    public static void initiateClientItemEvent(ItemStack stack, int event) {
        // Item events require a player context; fallback to client instance if available
        (clientInstance != null ? clientInstance : serverInstance).initiateClientItemEvent(stack, event);
    }

    public static INetworkManager getNetworkManager(boolean isClient) {
        return isClient ? clientInstance : serverInstance;
    }

    public static void setInstance(INetworkManager server, INetworkManager client) {
        if (FabricLoader.getInstance().isModLoaded(Info.MOD_ID)) {
            serverInstance = server;
            clientInstance = client;
        } else {
            throw new IllegalAccessError("Only IC2 can set the instance");
        }
    }

    private NetworkHelper() {}
}
