package ic2.core.item.tfbp;

import ic2.core.block.machine.tileentity.TileEntityTerraformer;
import ic2.core.util.BiomeUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.MushroomPlantBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeKeys;

/**
 * Mushroom: turns dirt and grass into mycelium (and the column into the mushroom fields biome),
 * plants mushrooms on mycelium and grows them into huge mushrooms. Each step needs the one
 * before it next to it: a huge mushroom grows only from a mushroom, a mushroom only on mycelium.
 */
class Mushroom extends TerraformerBase {
    @Override
    boolean terraform(World world, BlockPos pos) {
        pos = TileEntityTerraformer.getFirstSolidBlockFrom(world, pos, 20);
        return pos != null && growBlockWithDependancy(world, pos, Blocks.BROWN_MUSHROOM_BLOCK, Blocks.BROWN_MUSHROOM);
    }

    /**
     * Grows target at pos if what it depends on is around; otherwise works on that first. The
     * search covers the two columns x-1..x and z-1..z, from 5 above to 1 below pos - the
     * original's loops stop one short of the 3x3 they look meant for, which is kept.
     */
    private static boolean growBlockWithDependancy(World world, BlockPos pos, Block target, Block dependancy) {
        BlockPos.Mutable cPos = new BlockPos.Mutable();

        for (int xm = pos.getX() - 1; dependancy != null && xm < pos.getX() + 1; xm++) {
            for (int zm = pos.getZ() - 1; zm < pos.getZ() + 1; zm++) {
                for (int ym = pos.getY() + 5; ym > pos.getY() - 2; ym--) {
                    cPos.set(xm, ym, zm);
                    BlockState state = world.getBlockState(cPos);
                    Block block = state.getBlock();
                    if (dependancy == Blocks.MYCELIUM) {
                        if (block == dependancy || block == Blocks.BROWN_MUSHROOM_BLOCK || block == Blocks.RED_MUSHROOM_BLOCK) {
                            break;
                        }
                        if (!state.isAir() && (block == Blocks.DIRT || block == Blocks.GRASS_BLOCK)) {
                            BlockPos dstPos = cPos.toImmutable();
                            world.setBlockState(dstPos, dependancy.getDefaultState());
                            BiomeUtil.setBiome(world, dstPos, BiomeKeys.MUSHROOM_FIELDS);
                            return true;
                        }
                    } else if (dependancy == Blocks.BROWN_MUSHROOM) {
                        if (block == Blocks.BROWN_MUSHROOM || block == Blocks.RED_MUSHROOM) {
                            break;
                        }
                        if (!state.isAir() && growBlockWithDependancy(world, cPos.toImmutable(), Blocks.BROWN_MUSHROOM, Blocks.MYCELIUM)) {
                            return true;
                        }
                    }
                }
            }
        }

        if (target == Blocks.BROWN_MUSHROOM) {
            Block base = world.getBlockState(pos).getBlock();
            if (base != Blocks.MYCELIUM) {
                if (base != Blocks.BROWN_MUSHROOM_BLOCK && base != Blocks.RED_MUSHROOM_BLOCK) {
                    return false;
                }
                world.setBlockState(pos, Blocks.MYCELIUM.getDefaultState());
            }

            BlockPos above = pos.up();
            BlockState state = world.getBlockState(above);
            if (!state.isAir() && !state.isOf(Blocks.SHORT_GRASS)) {
                return false;
            }
            Block shroom = world.random.nextBoolean() ? Blocks.BROWN_MUSHROOM : Blocks.RED_MUSHROOM;
            world.setBlockState(above, shroom.getDefaultState());
            return true;
        }

        if (target == Blocks.BROWN_MUSHROOM_BLOCK) {
            BlockPos above = pos.up();
            BlockState state = world.getBlockState(above);
            if (!(state.getBlock() instanceof MushroomPlantBlock mushroom) || !(world instanceof ServerWorld serverWorld)) {
                return false;
            }

            if (mushroom.trySpawningBigMushroom(serverWorld, above, state, world.random)) {
                for (int xm = pos.getX() - 1; xm < pos.getX() + 1; xm++) {
                    for (int zm = pos.getZ() - 1; zm < pos.getZ() + 1; zm++) {
                        cPos.set(xm, above.getY(), zm);
                        Block block = world.getBlockState(cPos).getBlock();
                        if (block == Blocks.BROWN_MUSHROOM || block == Blocks.RED_MUSHROOM) {
                            world.setBlockState(cPos, Blocks.AIR.getDefaultState());
                        }
                    }
                }
                return true;
            }
        }
        return false;
    }
}
