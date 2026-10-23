package ic2.api.item;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface IMiningDrill {
    int energyUse(ItemStack stack, World world, BlockPos pos, BlockState state);
    int breakTime(ItemStack stack, World world, BlockPos pos, BlockState state);
    boolean breakBlock(ItemStack stack, World world, BlockPos pos, BlockState state);
    default boolean tryUsePower(ItemStack drill, double amount) {
        return ElectricItem.manager.use(drill, amount, null);
    }
}
