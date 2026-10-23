package ic2.core.item.tfbp;

import ic2.core.block.machine.tileentity.TileEntityTerraformer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.Fertilizable;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * Irrigation: now and then (1 in 48 000) starts rain; otherwise turns sand into dirt, makes
 * plants grow as bone meal does, lets logs grow taller with a ring of leaves, puts out fire and
 * spreads grass.
 */
class Irrigation extends TerraformerBase {
    @Override
    boolean terraform(World world, BlockPos pos) {
        if (world.random.nextInt(48000) == 0) {
            if (world instanceof ServerWorld serverWorld) {
                // only the rain flag, as WorldInfo.setRaining did; the weather cycle picks the duration
                serverWorld.setWeather(0, 0, true, serverWorld.isThundering());
            }
            return true;
        }

        pos = TileEntityTerraformer.getFirstBlockFrom(world, pos, 10);
        if (pos == null) {
            return false;
        }
        if (TileEntityTerraformer.switchGround(world, pos, Blocks.SAND, Blocks.DIRT.getDefaultState(), true)) {
            TileEntityTerraformer.switchGround(world, pos, Blocks.SAND, Blocks.DIRT.getDefaultState(), true);
            return true;
        }

        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (block instanceof Fertilizable growable && growable.isFertilizable(world, pos, state)) {
            if (world instanceof ServerWorld serverWorld) {
                growable.grow(serverWorld, world.random, pos, state);
            }
            return true;
        }
        if (block == Blocks.SHORT_GRASS) {
            return spreadGrass(world, pos.north()) || spreadGrass(world, pos.east())
                    || spreadGrass(world, pos.south()) || spreadGrass(world, pos.west());
        }
        if (isLog(block)) {
            BlockPos above = pos.up();
            world.setBlockState(above, state);
            BlockState leaves = getLeaves(world, pos);
            if (leaves != null) {
                createLeaves(world, above, leaves);
            }
            return true;
        }
        if (block == Blocks.FIRE) {
            world.setBlockState(pos, Blocks.AIR.getDefaultState());
            return true;
        }
        return false;
    }

    /** The logs of 1.12.2's log and log2 blocks. */
    static boolean isLog(Block block) {
        return block == Blocks.OAK_LOG || block == Blocks.SPRUCE_LOG || block == Blocks.BIRCH_LOG || block == Blocks.JUNGLE_LOG
                || block == Blocks.ACACIA_LOG || block == Blocks.DARK_OAK_LOG;
    }

    private static BlockState getLeaves(World world, BlockPos pos) {
        for (Direction facing : Direction.Type.HORIZONTAL) {
            BlockState state = world.getBlockState(pos.offset(facing));
            if (state.isIn(BlockTags.LEAVES)) {
                return state;
            }
        }
        return null;
    }

    private static void createLeaves(World world, BlockPos pos, BlockState state) {
        BlockPos above = pos.up();
        if (world.isAir(above)) {
            world.setBlockState(above, state);
        }

        for (Direction facing : Direction.Type.HORIZONTAL) {
            BlockPos cPos = pos.offset(facing);
            if (world.isAir(cPos)) {
                world.setBlockState(cPos, state);
            }
        }
    }

    private static boolean spreadGrass(World world, BlockPos pos) {
        if (world.random.nextBoolean()) {
            return false;
        }

        pos = TileEntityTerraformer.getFirstBlockFrom(world, pos, 0);
        if (pos == null) {
            return false;
        }

        Block block = world.getBlockState(pos).getBlock();
        if (block == Blocks.DIRT) {
            world.setBlockState(pos, Blocks.GRASS_BLOCK.getDefaultState());
            return true;
        }
        if (block == Blocks.GRASS_BLOCK) {
            world.setBlockState(pos.up(), Blocks.SHORT_GRASS.getDefaultState());
            return true;
        }
        return false;
    }
}
