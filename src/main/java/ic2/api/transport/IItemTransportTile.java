package ic2.api.transport;

import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;

public interface IItemTransportTile extends IPipe {
    int putItems(ItemStack stack, Direction direction, boolean simulate);
    ItemStack getContents();
    void setContents(ItemStack stack);
    int getMaxStackSizeAllowed();
    int getTransferRate();
}
