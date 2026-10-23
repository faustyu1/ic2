package ic2.api.tile;

import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public interface IWrenchable {
    Direction getFacing(World world, BlockPos pos);
    default boolean canSetFacing(World world, BlockPos pos, Direction newDirection, PlayerEntity player) {
        return true;
    }
    boolean setFacing(World world, BlockPos pos, Direction newDirection, PlayerEntity player);
    boolean wrenchCanRemove(World world, BlockPos pos, PlayerEntity player);
    List<ItemStack> getWrenchDrops(World world, BlockPos pos, BlockState state, BlockEntity tileEntity, PlayerEntity player, int fortune);
}
