package ic2.core.item.tfbp;

import ic2.core.block.machine.tileentity.TileEntityTerraformer;
import ic2.core.init.IC2ClassicBlocks;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * Cultivation: turns sand and end stone into dirt, dirt into grass, and plants grass, flowers,
 * saplings, mushrooms, wheat, pumpkins and melons on grass.
 */
class Cultivation extends TerraformerBase {
    private static List<BlockState> plants;
    private static Set<Block> plantBlocks;

    /** The plants cultivation picks from; grass twice as likely as the rest, as in 1.12.2. */
    static List<BlockState> getPlants() {
        if (plants == null) {
            List<BlockState> list = new ArrayList<>();
            list.add(Blocks.SHORT_GRASS.getDefaultState());
            list.add(Blocks.SHORT_GRASS.getDefaultState());
            list.add(Blocks.FERN.getDefaultState());
            list.add(Blocks.POPPY.getDefaultState());
            list.add(Blocks.DANDELION.getDefaultState());
            list.add(Blocks.TALL_GRASS.getDefaultState());
            list.add(Blocks.ROSE_BUSH.getDefaultState());
            list.add(Blocks.SUNFLOWER.getDefaultState());
            // the six saplings of 1.12.2
            list.add(Blocks.OAK_SAPLING.getDefaultState());
            list.add(Blocks.SPRUCE_SAPLING.getDefaultState());
            list.add(Blocks.BIRCH_SAPLING.getDefaultState());
            list.add(Blocks.JUNGLE_SAPLING.getDefaultState());
            list.add(Blocks.ACACIA_SAPLING.getDefaultState());
            list.add(Blocks.DARK_OAK_SAPLING.getDefaultState());
            list.add(Blocks.WHEAT.getDefaultState());
            list.add(Blocks.RED_MUSHROOM.getDefaultState());
            list.add(Blocks.BROWN_MUSHROOM.getDefaultState());
            list.add(Blocks.PUMPKIN.getDefaultState());
            list.add(Blocks.MELON.getDefaultState());
            list.add(IC2ClassicBlocks.RUBBER_SAPLING.getDefaultState());
            plants = list;
        }
        return plants;
    }

    /**
     * The blocks that count as a plant for desertification. 1.12.2 compared blocks, and its
     * red_flower, double_plant and tallgrass blocks held every variant, so their 1.21 siblings
     * are listed too.
     */
    static boolean isPlant(Block block) {
        if (plantBlocks == null) {
            Set<Block> set = Collections.newSetFromMap(new IdentityHashMap<>());
            for (BlockState state : getPlants()) {
                set.add(state.getBlock());
            }
            Collections.addAll(set, Blocks.BLUE_ORCHID, Blocks.ALLIUM, Blocks.AZURE_BLUET, Blocks.RED_TULIP,
                    Blocks.ORANGE_TULIP, Blocks.WHITE_TULIP, Blocks.PINK_TULIP, Blocks.OXEYE_DAISY,
                    Blocks.LILAC, Blocks.PEONY, Blocks.LARGE_FERN);
            plantBlocks = set;
        }
        return plantBlocks.contains(block);
    }

    @Override
    boolean terraform(World world, BlockPos pos) {
        pos = TileEntityTerraformer.getFirstSolidBlockFrom(world, pos, 10);
        if (pos == null) {
            return false;
        }
        if (TileEntityTerraformer.switchGround(world, pos, Blocks.SAND, Blocks.DIRT.getDefaultState(), true)) {
            return true;
        }

        if (TileEntityTerraformer.switchGround(world, pos, Blocks.END_STONE, Blocks.DIRT.getDefaultState(), true)) {
            int i = 4;
            while (--i > 0 && TileEntityTerraformer.switchGround(world, pos, Blocks.END_STONE, Blocks.DIRT.getDefaultState(), true)) {
            }
        }

        Block block = world.getBlockState(pos).getBlock();
        if (block == Blocks.DIRT) {
            world.setBlockState(pos, Blocks.GRASS_BLOCK.getDefaultState());
            return true;
        }
        return block == Blocks.GRASS_BLOCK && growPlantsOn(world, pos);
    }

    private static boolean growPlantsOn(World world, BlockPos pos) {
        BlockPos above = pos.up();
        BlockState state = world.getBlockState(above);
        Block block = state.getBlock();
        if (state.isAir() || block == Blocks.SHORT_GRASS && world.random.nextInt(4) == 0) {
            BlockState plant = pickRandomPlant(world.random);
            if (plant.getBlock() instanceof CropBlock) {
                world.setBlockState(pos, Blocks.FARMLAND.getDefaultState());
            } else if (plant.getBlock() instanceof TallPlantBlock) {
                TallPlantBlock.placeAt(world, plant, above, 3);
                return true;
            }

            world.setBlockState(above, plant);
            return true;
        }
        return false;
    }

    private static BlockState pickRandomPlant(Random random) {
        List<BlockState> list = getPlants();
        return list.get(random.nextInt(list.size()));
    }
}
