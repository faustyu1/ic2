package ic2.core.gametest;

import ic2.api.item.ElectricItem;
import ic2.core.block.machine.BlockMiningPipe;
import ic2.core.block.machine.tileentity.TileEntityAdvMiner;
import ic2.core.block.machine.tileentity.TileEntityMiner;
import ic2.core.init.IC2Blocks;
import ic2.core.init.IC2ClassicBlocks;
import ic2.core.init.IC2ItemVariants;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/** The miners in a running world. Run with {@code ./gradlew runGametest}. */
public class MinerGameTest implements FabricGameTest {
    private static ItemStack charged(String id) {
        ItemStack stack = new ItemStack(Registries.ITEM.get(Identifier.of("ic2", id)));
        ElectricItem.manager.charge(stack, Double.MAX_VALUE, Integer.MAX_VALUE, true, false);
        return stack;
    }

    private static int count(TileEntityMiner miner, net.minecraft.item.Item item) {
        int n = 0;
        for (ItemStack stack : miner.buffer) {
            if (stack.isOf(item)) {
                n += stack.getCount();
            }
        }
        return n;
    }

    /** With a drill a block takes 200 ticks; the miner leaves a pipe tip, and pulls the pipe back without a drill. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void minerDigsAndWithdraws(TestContext ctx) {
        BlockPos minerPos = new BlockPos(2, 4, 2);
        ctx.setBlockState(minerPos, IC2Blocks.MINER_BLOCK.getDefaultState());
        ctx.setBlockState(minerPos.down(), Blocks.STONE.getDefaultState());
        TileEntityMiner miner = ctx.getBlockEntity(minerPos);
        miner.drillSlot.put(charged("drill"));
        miner.pipeSlot.put(new ItemStack(IC2ItemVariants.get("mining_pipe", "pipe"), 10));

        for (int i = 0; i < 199; i++) {
            miner.injectEnergy(Direction.UP, 32, 32);
            miner.tick();
        }
        ctx.assertTrue(ctx.getBlockState(minerPos.down()).isOf(Blocks.STONE), "mined before 200 ticks");
        for (int i = 0; i < 3; i++) {
            miner.injectEnergy(Direction.UP, 32, 32);
            miner.tick();
        }
        ctx.assertTrue(ctx.getBlockState(minerPos.down()) == IC2ClassicBlocks.MINING_PIPE.getState(BlockMiningPipe.MiningPipeType.tip),
                "no pipe tip: " + ctx.getBlockState(minerPos.down()));
        ctx.assertTrue(count(miner, Items.COBBLESTONE) == 1, "no cobblestone in the buffer");
        ctx.assertTrue(miner.pipeSlot.get().getCount() == 9, "pipes " + miner.pipeSlot.get());

        miner.drillSlot.clear();
        for (int i = 0; i < 22; i++) {
            miner.injectEnergy(Direction.UP, 32, 32);
            miner.tick();
        }
        ctx.assertTrue(ctx.getBlockState(minerPos.down()).isAir(), "pipe not withdrawn: " + ctx.getBlockState(minerPos.down()));
        ctx.assertTrue(count(miner, IC2ItemVariants.get("mining_pipe", "pipe")) == 1, "pipe not back in the buffer");
        ctx.complete();
    }

    /** The advanced miner's filter, silk touch, and drops into the inventory next to it. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void advancedMinerFiltersAndMines(TestContext ctx) {
        BlockPos minerPos = new BlockPos(2, 3, 2);
        ctx.setBlockState(minerPos, IC2Blocks.ADV_MINER_BLOCK.getDefaultState());
        ctx.setBlockState(minerPos.east(), Blocks.CHEST.getDefaultState());
        BlockPos stone = new BlockPos(3, 1, 3);
        ctx.setBlockState(stone, Blocks.STONE.getDefaultState());
        TileEntityAdvMiner miner = ctx.getBlockEntity(minerPos);
        BlockPos absStone = ctx.getAbsolutePos(stone);

        ctx.assertTrue(miner.canMine(absStone, Blocks.STONE.getDefaultState()), "empty blacklist refuses stone");
        miner.filterSlot.put(0, new ItemStack(Items.COBBLESTONE));
        ctx.assertTrue(!miner.canMine(absStone, Blocks.STONE.getDefaultState()), "blacklisted cobblestone mined");
        miner.onGuiEvent(null, "mode");
        ctx.assertTrue(miner.canMine(absStone, Blocks.STONE.getDefaultState()), "whitelisted cobblestone refused");
        ctx.assertTrue(!miner.canMine(ctx.getAbsolutePos(new BlockPos(3, 1, 4)), Blocks.BEDROCK.getDefaultState()), "bedrock mineable");

        miner.onGuiEvent(null, "silktouch");
        miner.injectEnergy(Direction.UP, 10000, 512);
        miner.doMine(absStone, Blocks.STONE.getDefaultState());
        ChestBlockEntity chest = ctx.getBlockEntity(minerPos.east());
        ctx.assertTrue(chest.getStack(0).isOf(Items.STONE), "chest got " + chest.getStack(0));
        ctx.assertTrue(ctx.getBlockState(stone).isAir(), "stone still there");
        ctx.assertTrue(miner.getEnergy() == 10000 - 512, "energy " + miner.getEnergy());
        ctx.complete();
    }
}
