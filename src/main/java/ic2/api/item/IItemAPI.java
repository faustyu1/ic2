package ic2.api.item;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public interface IItemAPI {
    BlockState getBlockState(String name, String variant);
    ItemStack getItemStack(String name, String variant);
    Block getBlock(String name);
    Item getItem(String name);
}
