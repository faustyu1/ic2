package ic2.core.gametest;

import ic2.core.block.machine.tileentity.TileEntityStandardMachine;
import ic2.core.block.wiring.TileEntityElectricBlock;
import ic2.core.init.IC2Blocks;
import ic2.core.init.IC2ItemVariants;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Machines in a running world: what the upgrades do to them, a whole operation, the ejector, and
 * a machine that was never placed by a player still joining the energy net.
 * Run with {@code ./gradlew runGametest}.
 */
public class MachineGameTest implements FabricGameTest {
    private static final BlockPos MACHINE = new BlockPos(2, 2, 2);

    private static ItemStack upgrade(String kind, int count) {
        Item item = IC2ItemVariants.get("upgrade", kind);
        return new ItemStack(item, count);
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void upgradesChangeTheMachinesRates(TestContext ctx) {
        ctx.setBlockState(MACHINE, IC2Blocks.MACERATOR_BLOCK.getDefaultState());
        TileEntityStandardMachine<?, ?, ?> macerator = ctx.getBlockEntity(MACHINE);
        ctx.assertTrue(macerator.getGuiCapacity("energy") == 1200, "default buffer " + macerator.getGuiCapacity("energy"));

        // two overclockers: 300 * 0.49 = 147 ticks, 2 * 2.56 = 5 EU a tick
        macerator.upgradeSlot.put(0, upgrade("overclocker", 2));
        ctx.assertTrue(macerator.getOperationLength() == 147, "length " + macerator.getOperationLength());
        ctx.assertTrue(macerator.getEnergyConsume() == 5, "consume " + macerator.getEnergyConsume());

        // a transformer raises the tier, a storage upgrade adds 10000 EU
        macerator.upgradeSlot.put(1, upgrade("transformer", 1));
        macerator.upgradeSlot.put(2, upgrade("energy_storage", 1));
        ctx.assertTrue(macerator.getSinkTier() == 2, "tier " + macerator.getSinkTier());
        // 600 of its own, a whole operation of 147 * 5, and the 10000 of the upgrade
        ctx.assertTrue(macerator.getGuiCapacity("energy") == 600 + 147 * 5 + 10000, "buffer " + macerator.getGuiCapacity("energy"));

        // an upgrade a machine cannot use is not taken
        ctx.assertTrue(!macerator.upgradeSlot.accepts(upgrade("fluid_ejector", 1)), "macerator takes a fluid ejector");
        ctx.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void maceratorGrindsAndTheEjectorEmptiesIt(TestContext ctx) {
        ctx.setBlockState(MACHINE, IC2Blocks.MACERATOR_BLOCK.getDefaultState());
        ctx.setBlockState(MACHINE.east(), Blocks.CHEST.getDefaultState());
        TileEntityStandardMachine<?, ?, ?> macerator = ctx.getBlockEntity(MACHINE);

        macerator.setInput(new ItemStack(Items.COBBLESTONE, 2));
        macerator.injectEnergy(Direction.UP, 1200, 32);
        for (int i = 0; i < 300; i++) {
            macerator.tick();
        }
        ctx.assertTrue(macerator.getOutput(0).isOf(Items.SAND), "no sand: " + macerator.getOutput(0));

        macerator.upgradeSlot.put(0, upgrade("ejector", 1));
        macerator.tick();
        ChestBlockEntity chest = ctx.getBlockEntity(MACHINE.east());
        ctx.assertTrue(chest.getStack(0).isOf(Items.SAND), "ejector moved nothing: " + chest.getStack(0));
        ctx.complete();
    }

    /** Placed without a player, as a chunk loading from disk does, the machine still joins the net. */
    @GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 200)
    public void machinesJoinTheEnergyNetWithoutBeingPlaced(TestContext ctx) {
        ctx.setBlockState(MACHINE, IC2Blocks.MACERATOR_BLOCK.getDefaultState());
        ctx.setBlockState(MACHINE.west(), IC2Blocks.BATBOX_BLOCK.getDefaultState());
        TileEntityElectricBlock batbox = ctx.getBlockEntity(MACHINE.west());
        // a storage block gives energy only through its face
        batbox.setFacing(Direction.EAST);
        batbox.injectEnergy(Direction.UP, 40000, 32);

        TileEntityStandardMachine<?, ?, ?> macerator = ctx.getBlockEntity(MACHINE);
        ctx.waitAndRun(100, () -> {
            ctx.assertTrue(macerator.getEnergy() > 0, "macerator got no energy from the BatBox");
            ctx.complete();
        });
    }
}
