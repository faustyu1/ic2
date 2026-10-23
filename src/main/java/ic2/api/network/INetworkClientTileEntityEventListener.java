package ic2.api.network;

import net.minecraft.entity.player.PlayerEntity;

public interface INetworkClientTileEntityEventListener {
    void onNetworkEvent(PlayerEntity player, int event);
}
