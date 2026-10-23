package ic2.core.block.transport.cover;

import java.util.Set;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/** A block that takes attachments (covers) on its sides: the pipes. The crowbar takes them off again. */
public interface ICoverHolder {
    Set<CoverProperty> getCoverProperties();

    boolean canPlaceCover(World world, BlockPos pos, Direction side, ItemStack stack);

    void placeCover(World world, BlockPos pos, Direction side, ItemStack stack);

    boolean canRemoveCover(World world, BlockPos pos, Direction side);

    void removeCover(World world, BlockPos pos, Direction side);
}
