package ic2.core.item.tfbp;

import ic2.core.block.machine.tileentity.TileEntityTerraformer;
import ic2.core.init.IC2ClassicBlocks;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Flatification: levels the land to the terraformer's height, filling dips with dirt and
 * removing natural blocks - ground, stone, wood, leaves and plants - that stick out above it.
 */
class Flatification extends TerraformerBase {
    private static Set<Block> removable;

    private static boolean canRemove(Block block) {
        if (removable == null) {
            Set<Block> set = Collections.newSetFromMap(new IdentityHashMap<>());
            Collections.addAll(set, Blocks.SNOW_BLOCK, Blocks.ICE, Blocks.GRASS_BLOCK, Blocks.STONE, Blocks.GRAVEL, Blocks.SAND,
                    Blocks.DIRT,
                    // 1.12.2's leaves, leaves2 and log blocks
                    Blocks.OAK_LEAVES, Blocks.SPRUCE_LEAVES, Blocks.BIRCH_LEAVES, Blocks.JUNGLE_LEAVES, Blocks.ACACIA_LEAVES,
                    Blocks.DARK_OAK_LEAVES, Blocks.OAK_LOG, Blocks.SPRUCE_LOG, Blocks.BIRCH_LOG, Blocks.JUNGLE_LOG,
                    // tallgrass, red_flower, yellow_flower, sapling
                    Blocks.SHORT_GRASS, Blocks.FERN, Blocks.POPPY, Blocks.BLUE_ORCHID, Blocks.ALLIUM, Blocks.AZURE_BLUET,
                    Blocks.RED_TULIP, Blocks.ORANGE_TULIP, Blocks.WHITE_TULIP, Blocks.PINK_TULIP, Blocks.OXEYE_DAISY,
                    Blocks.DANDELION, Blocks.OAK_SAPLING, Blocks.SPRUCE_SAPLING, Blocks.BIRCH_SAPLING, Blocks.JUNGLE_SAPLING,
                    Blocks.ACACIA_SAPLING, Blocks.DARK_OAK_SAPLING,
                    Blocks.WHEAT, Blocks.RED_MUSHROOM, Blocks.BROWN_MUSHROOM, Blocks.PUMPKIN, Blocks.MELON,
                    IC2ClassicBlocks.RUBBER_LEAVES, IC2ClassicBlocks.RUBBER_SAPLING, IC2ClassicBlocks.RUBBER_WOOD);
            removable = set;
        }
        return removable.contains(block);
    }

    @Override
    boolean terraform(World world, BlockPos pos) {
        BlockPos workPos = TileEntityTerraformer.getFirstBlockFrom(world, pos, 20);
        if (workPos == null) {
            return false;
        }

        if (world.getBlockState(workPos).isOf(Blocks.SNOW)) {
            workPos = workPos.down();
        }

        if (pos.getY() == workPos.getY()) {
            return false;
        }
        if (workPos.getY() < pos.getY()) {
            world.setBlockState(workPos.up(), Blocks.DIRT.getDefaultState());
            return true;
        }
        if (canRemove(world.getBlockState(workPos).getBlock())) {
            world.setBlockState(workPos, Blocks.AIR.getDefaultState());
            return true;
        }
        return false;
    }
}
