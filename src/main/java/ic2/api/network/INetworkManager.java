package ic2.api.network;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public interface INetworkManager {
    void updateTileEntityField(BlockEntity tileEntity, String field);
    void initiateTileEntityEvent(BlockEntity tileEntity, int event, boolean limitRange);
    void initiateItemEvent(PlayerEntity player, ItemStack stack, int event, boolean limitRange);
    void initiateClientTileEntityEvent(BlockEntity tileEntity, int event);
    void initiateClientItemEvent(ItemStack stack, int event);
    void sendInitialData(BlockEntity tileEntity);
}
