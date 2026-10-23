package ic2.core.item.tfbp;

import ic2.core.block.machine.tileentity.TileEntityTerraformer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SnowBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * Chilling: freezes water to ice, also under ice, piles snow layers up and turns them into snow
 * blocks once the columns around are snowed in.
 */
class Chilling extends TerraformerBase {
    @Override
    boolean terraform(World world, BlockPos pos) {
        pos = TileEntityTerraformer.getFirstBlockFrom(world, pos, 10);
        if (pos == null) {
            return false;
        }

        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (block == Blocks.WATER) {
            world.setBlockState(pos, Blocks.ICE.getDefaultState());
            return true;
        }

        if (block == Blocks.ICE) {
            BlockPos below = pos.down();
            if (world.getBlockState(below).isOf(Blocks.WATER)) {
                world.setBlockState(below, Blocks.ICE.getDefaultState());
                return true;
            }
        } else if (block == Blocks.SNOW) {
            if (isSurroundedBySnow(world, pos)) {
                world.setBlockState(pos, Blocks.SNOW_BLOCK.getDefaultState());
                return true;
            }

            int size = state.get(SnowBlock.LAYERS);
            if (SnowBlock.LAYERS.getValues().contains(size + 1)) {
                world.setBlockState(pos, state.with(SnowBlock.LAYERS, size + 1));
                return true;
            }
        }

        pos = pos.up();
        if (!canPlaceSnow(world, pos) && block != Blocks.ICE) {
            return false;
        }
        world.setBlockState(pos, Blocks.SNOW.getDefaultState());
        return true;
    }

    /** Whether a snow layer could be placed there, as BlockSnow.canPlaceBlockAt said. */
    static boolean canPlaceSnow(World world, BlockPos pos) {
        return world.getBlockState(pos).isReplaceable() && Blocks.SNOW.getDefaultState().canPlaceAt(world, pos);
    }

    private static boolean isSurroundedBySnow(World world, BlockPos pos) {
        for (Direction dir : Direction.Type.HORIZONTAL) {
            if (!isSnowHere(world, pos.offset(dir))) {
                return false;
            }
        }
        return true;
    }

    /** Whether the column is snowed in at that height; if not, it gets a snow layer when it can. */
    private static boolean isSnowHere(World world, BlockPos pos) {
        int prevY = pos.getY();
        pos = TileEntityTerraformer.getFirstBlockFrom(world, pos, 16);
        if (pos == null || prevY > pos.getY()) {
            return false;
        }

        Block block = world.getBlockState(pos).getBlock();
        if (block == Blocks.SNOW_BLOCK || block == Blocks.SNOW) {
            return true;
        }

        pos = pos.up();
        if (canPlaceSnow(world, pos) || block == Blocks.ICE) {
            world.setBlockState(pos, Blocks.SNOW.getDefaultState());
        }
        return false;
    }
}
