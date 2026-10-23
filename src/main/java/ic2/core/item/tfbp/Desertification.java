package ic2.core.item.tfbp;

import ic2.core.block.machine.tileentity.TileEntityTerraformer;
import ic2.core.init.IC2ClassicBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Desertification: turns dirt, grass and farmland into sand, dries up water, removes snow, leaves
 * and plants, melts ice and snow blocks into water and now and then sets wood on fire.
 */
class Desertification extends TerraformerBase {
    @Override
    boolean terraform(World world, BlockPos pos) {
        pos = TileEntityTerraformer.getFirstBlockFrom(world, pos, 10);
        if (pos == null) {
            return false;
        }

        BlockState sand = Blocks.SAND.getDefaultState();
        if (TileEntityTerraformer.switchGround(world, pos, Blocks.DIRT, sand, false)
                || TileEntityTerraformer.switchGround(world, pos, Blocks.GRASS_BLOCK, sand, false)
                || TileEntityTerraformer.switchGround(world, pos, Blocks.FARMLAND, sand, false)) {
            TileEntityTerraformer.switchGround(world, pos, Blocks.DIRT, sand, false);
            return true;
        }

        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (block == Blocks.WATER || block == Blocks.SNOW || isVanillaLeaves(block) || block == IC2ClassicBlocks.RUBBER_LEAVES
                || Cultivation.isPlant(block)) {
            world.setBlockState(pos, Blocks.AIR.getDefaultState());
            if (Cultivation.isPlant(world.getBlockState(pos.up()).getBlock())) {
                world.setBlockState(pos.up(), Blocks.AIR.getDefaultState());
            }
            return true;
        }

        if (block == Blocks.ICE || block == Blocks.SNOW_BLOCK) {
            world.setBlockState(pos, Blocks.WATER.getDefaultState());
            return true;
        }

        if ((state.isIn(BlockTags.PLANKS) || isFirstLog(block) || block == IC2ClassicBlocks.RUBBER_WOOD) && world.random.nextInt(15) == 0) {
            world.setBlockState(pos, Blocks.FIRE.getDefaultState());
            return true;
        }
        return false;
    }

    /** The leaves of 1.12.2's leaves and leaves2 blocks. */
    static boolean isVanillaLeaves(Block block) {
        return block == Blocks.OAK_LEAVES || block == Blocks.SPRUCE_LEAVES || block == Blocks.BIRCH_LEAVES
                || block == Blocks.JUNGLE_LEAVES || block == Blocks.ACACIA_LEAVES || block == Blocks.DARK_OAK_LEAVES;
    }

    /** The logs of 1.12.2's first log block, the only one the original set on fire. */
    static boolean isFirstLog(Block block) {
        return block == Blocks.OAK_LOG || block == Blocks.SPRUCE_LOG || block == Blocks.BIRCH_LOG || block == Blocks.JUNGLE_LOG;
    }
}
