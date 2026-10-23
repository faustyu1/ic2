package ic2.api.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface ITerraformingBP {
    double getConsume(ItemStack stack);
    int getRange(ItemStack stack);
    boolean canInsert(ItemStack stack, PlayerEntity player, World world, BlockPos pos);
    boolean terraform(ItemStack stack, World world, BlockPos pos);
}
