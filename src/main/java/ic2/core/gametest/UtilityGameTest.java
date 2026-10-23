package ic2.core.gametest;

import ic2.core.block.BlockTileEntity;
import ic2.core.block.machine.tileentity.TileEntityBlockCutter;
import ic2.core.block.machine.tileentity.TileEntityIronFurnace;
import ic2.core.block.machine.tileentity.TileEntityTesla;
import ic2.core.block.wiring.TileEntityElectricBlock;
import ic2.core.block.wiring.TileEntityLuminator;
import ic2.core.init.IC2Blocks;
import ic2.core.init.IC2ItemVariants;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/** The utility machines and storage blocks in a running world. Run with {@code ./gradlew runGametest}. */
public class UtilityGameTest implements FabricGameTest {
    private static final BlockPos POS = new BlockPos(2, 2, 2);

    /** Coal smelts raw iron, vanilla's recipe, in 160 ticks; the experience waits in the furnace. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void ironFurnaceSmeltsWithFuel(TestContext ctx) {
        ctx.setBlockState(POS, IC2Blocks.IRON_FURNACE_BLOCK.getDefaultState());
        TileEntityIronFurnace furnace = ctx.getBlockEntity(POS);
        furnace.inputSlot.put(new ItemStack(Items.RAW_IRON));
        furnace.fuelSlot.put(new ItemStack(Items.COAL));
        for (int i = 0; i < 159; i++) {
            furnace.tick();
        }
        ctx.assertTrue(furnace.outputSlot.isEmpty(), "done before 160 ticks");
        furnace.tick();
        ctx.assertTrue(furnace.outputSlot.get().isOf(Items.IRON_INGOT), "output " + furnace.outputSlot.get());
        ctx.assertTrue(furnace.getXp() > 0, "no experience");
        ctx.assertTrue(furnace.getActive(), "fuel not burning");
        ctx.complete();
    }

    /** Without a blade, or with one too soft, the cutter waits; a hard enough blade cuts gold into plates. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void blockCutterNeedsABlade(TestContext ctx) {
        ctx.setBlockState(POS, IC2Blocks.BLOCK_CUTTER_BLOCK.getDefaultState());
        TileEntityBlockCutter cutter = ctx.getBlockEntity(POS);
        cutter.setInput(new ItemStack(Items.GOLD_BLOCK));
        cutter.injectEnergy(Direction.UP, 10000, 32);
        cutter.tick();
        ctx.assertTrue(cutter.isBladeTooWeak() && !cutter.getActive(), "cuts without a blade");
        cutter.cutterSlot.put(new ItemStack(IC2ItemVariants.get("block_cutting_blade", "iron")));
        cutter.tick();
        ctx.assertTrue(!cutter.isBladeTooWeak() && cutter.getActive(), "iron blade (3) refused hardness 2");
        ctx.complete();
    }

    /** A storage block takes energy on every side but its face and gives it only through its face. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void storageBlockSidesAndRedstone(TestContext ctx) {
        ctx.setBlockState(POS, IC2Blocks.BATBOX_BLOCK.getDefaultState());
        TileEntityElectricBlock batbox = ctx.getBlockEntity(POS);
        batbox.setFacing(Direction.EAST);
        ctx.assertTrue(ctx.getBlockState(POS).get(BlockTileEntity.FACING) == Direction.EAST, "facing not in the block state");
        ctx.assertTrue(!batbox.acceptsEnergyFrom(null, Direction.EAST) && batbox.acceptsEnergyFrom(null, Direction.WEST), "input sides");
        ctx.assertTrue(batbox.emitsEnergyTo(null, Direction.EAST) && !batbox.emitsEnergyTo(null, Direction.WEST), "output sides");

        // mode 4: a signal while empty
        for (int i = 0; i < 4; i++) {
            batbox.onGuiEvent(null, "redstone");
        }
        batbox.tick();
        ctx.assertTrue(batbox.getRedstoneOutput() == 15, "empty batbox in mode 4 gives no signal");
        batbox.injectEnergy(Direction.WEST, 1000, 32);
        batbox.tick();
        ctx.assertTrue(batbox.getRedstoneOutput() == 0, "filled batbox in mode 4 still signals");
        ctx.complete();
    }

    /** Powered by redstone, the luminator lights up for 0.25 EU a tick and gives light 15. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void luminatorLightsWithRedstone(TestContext ctx) {
        ctx.setBlockState(POS.down(), Blocks.STONE.getDefaultState());
        ctx.setBlockState(POS, IC2Blocks.LUMINATOR_FLAT_BLOCK.getDefaultState().with(BlockTileEntity.FACING, Direction.UP));
        TileEntityLuminator luminator = ctx.getBlockEntity(POS);
        luminator.injectEnergy(Direction.DOWN, 5, 32);
        luminator.tick();
        ctx.assertTrue(!luminator.getActive(), "lit without a signal");
        ctx.setBlockState(POS.east(), Blocks.REDSTONE_BLOCK.getDefaultState());
        luminator.tick();
        ctx.assertTrue(luminator.getActive(), "not lit with a signal");
        ctx.assertTrue(ctx.getBlockState(POS).getLuminance() == 15, "light " + ctx.getBlockState(POS).getLuminance());
        ctx.assertTrue(luminator.getEnergy() == 4.75, "energy " + luminator.getEnergy());
        ctx.complete();
    }

    /** With a signal and 4000 EU the tesla coil hits a pig next to it for 10 every 32 ticks. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void teslaCoilShocks(TestContext ctx) {
        ctx.setBlockState(POS, IC2Blocks.TESLA_BLOCK.getDefaultState());
        ctx.setBlockState(POS.down(), Blocks.REDSTONE_BLOCK.getDefaultState());
        TileEntityTesla tesla = ctx.getBlockEntity(POS);
        PigEntity pig = ctx.spawnEntity(EntityType.PIG, POS.east().east());
        tesla.injectEnergy(Direction.UP, 10000, 128);
        for (int i = 0; i < 32; i++) {
            tesla.tick();
        }
        ctx.assertTrue(pig.getHealth() < pig.getMaxHealth(), "pig not shocked");
        ctx.assertTrue(tesla.getEnergy() < 10000 - 32 - 1000, "energy " + tesla.getEnergy());
        ctx.complete();
    }

    /** The creative generator fills a storage block through the energy net. */
    @GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 100)
    public void creativeGeneratorFillsAStorageBlock(TestContext ctx) {
        ctx.setBlockState(POS, IC2Blocks.CREATIVE_GENERATOR_BLOCK.getDefaultState());
        ctx.setBlockState(POS.east(), IC2Blocks.BATBOX_BLOCK.getDefaultState());
        TileEntityElectricBlock batbox = ctx.getBlockEntity(POS.east());
        batbox.setFacing(Direction.UP);
        ctx.waitAndRun(40, () -> {
            ctx.assertTrue(batbox.getStored() > 0, "batbox got nothing");
            ctx.complete();
        });
    }
}
