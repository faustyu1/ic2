package ic2.api.network;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public interface INetworkItemEventListener {
    void onNetworkEvent(ItemStack stack, PlayerEntity player, int event);
}
