package ic2.core.gametest;

import ic2.core.block.machine.tileentity.TileEntityMassFabricator;
import ic2.core.block.machine.tileentity.TileEntityMatter;
import ic2.core.block.machine.tileentity.TileEntityPatternStorage;
import ic2.core.block.machine.tileentity.TileEntityReplicator;
import ic2.core.block.machine.tileentity.TileEntityScanner;
import ic2.core.init.IC2Blocks;
import ic2.core.init.IC2Items;
import ic2.core.item.ItemCrystalMemory;
import ic2.core.ref.FluidName;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraftforge.fluids.FluidStack;

/** The UU-matter machines in a running world. Run with {@code ./gradlew runGametest}. */
public class UuGameTest implements FabricGameTest {
    private static final BlockPos POS = new BlockPos(2, 2, 2);

    /** A full million EU buffer becomes 1 mB of UU; scrap makes each incoming EU count six times. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void matterFabricatorMakesUu(TestContext ctx) {
        ctx.setBlockState(POS, IC2Blocks.MATTER_BLOCK.getDefaultState());
        TileEntityMatter matter = ctx.getBlockEntity(POS);
        matter.injectEnergy(Direction.UP, 1000000, 2048);
        matter.tick();
        ctx.assertTrue(matter.fluidTank.getFluidAmount() == 1, "uu " + matter.fluidTank.getFluidAmount());
        ctx.assertTrue(matter.fluidTank.getFluid().getFluid() == FluidName.uu_matter.getInstance(), "not uu");
        ctx.assertTrue(matter.getEnergy() == 0, "energy " + matter.getEnergy());

        matter.amplifierSlot.put(new ItemStack(IC2Items.SCRAP));
        matter.injectEnergy(Direction.UP, 1000, 2048);
        matter.tick();
        ctx.assertTrue(matter.scrap == 5000, "scrap " + matter.scrap);
        matter.injectEnergy(Direction.UP, 1000, 2048);
        matter.tick();
        ctx.assertTrue(matter.getEnergy() == 7000, "amplified energy " + matter.getEnergy());
        ctx.assertTrue(matter.scrap == 4000, "scrap left " + matter.scrap);
        ctx.complete();
    }

    /** The mass fabricator burns scrap value no faster than its tier's power. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void massFabricatorBurnsScrap(TestContext ctx) {
        ctx.setBlockState(POS, IC2Blocks.MASS_FABRICATOR_BLOCK.getDefaultState());
        TileEntityMassFabricator fabricator = ctx.getBlockEntity(POS);
        fabricator.amplifierSlot.put(new ItemStack(IC2Items.SCRAP));
        fabricator.injectEnergy(Direction.UP, 100000, 2048);
        fabricator.tick();
        ctx.assertTrue(fabricator.consumedScrap == 512, "consumed " + fabricator.consumedScrap);
        ctx.assertTrue(fabricator.scrap == 50000 - 512, "scrap " + fabricator.scrap);
        ctx.complete();
    }

    /** Scan cobblestone into the pattern storage, then replicate it from UU-matter. */
    @GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 400)
    public void scanStoreAndReplicate(TestContext ctx) {
        ctx.setBlockState(POS, IC2Blocks.SCANNER_BLOCK.getDefaultState());
        ctx.setBlockState(POS.east(), IC2Blocks.PATTERN_STORAGE_BLOCK.getDefaultState());
        ctx.setBlockState(POS.east().east(), IC2Blocks.REPLICATOR_BLOCK.getDefaultState());
        TileEntityScanner scanner = ctx.getBlockEntity(POS);
        TileEntityPatternStorage storage = ctx.getBlockEntity(POS.east());
        TileEntityReplicator replicator = ctx.getBlockEntity(POS.east().east());

        ctx.assertTrue(!scanner.inputSlot.accepts(new ItemStack(Items.BEDROCK)), "bedrock has no UU value but is scannable");
        scanner.inputSlot.put(new ItemStack(Items.COBBLESTONE));
        for (int i = 0; i < TileEntityScanner.DURATION; i++) {
            scanner.injectEnergy(Direction.UP, 256, 2048);
            scanner.tick();
        }
        ctx.assertTrue(scanner.getState() == TileEntityScanner.State.COMPLETED, "state " + scanner.getState());
        ctx.assertTrue(scanner.inputSlot.isEmpty(), "cobblestone not used up");
        scanner.onGuiEvent(null, "save");
        ctx.assertTrue(storage.getPatterns().size() == 1 && storage.getPatterns().get(0).isOf(Items.COBBLESTONE),
                "storage " + storage.getPatterns());

        // the same item again is refused
        scanner.inputSlot.put(new ItemStack(Items.COBBLESTONE));
        scanner.injectEnergy(Direction.UP, 256, 2048);
        scanner.tick();
        ctx.assertTrue(scanner.getState() == TileEntityScanner.State.ALREADY_RECORDED, "state " + scanner.getState());

        replicator.fluidTank.fillInternal(new FluidStack(FluidName.uu_matter.getInstance(), 1000), true);
        replicator.injectEnergy(Direction.UP, 10000, 2048);
        replicator.onGuiEvent(null, "single");
        ctx.assertTrue(replicator.getMode() == TileEntityReplicator.Mode.SINGLE, "mode " + replicator.getMode());
        replicator.tick();
        ctx.assertTrue(replicator.outputSlot.get().isOf(Items.COBBLESTONE), "output " + replicator.outputSlot.get());
        ctx.assertTrue(replicator.fluidTank.getFluidAmount() == 999, "uu " + replicator.fluidTank.getFluidAmount());
        ctx.assertTrue(replicator.getMode() == TileEntityReplicator.Mode.STOPPED, "single run kept going");
        ctx.complete();
    }

    /** The pattern storage writes the current pattern to a crystal memory and reads it back. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void crystalMemoryCarriesAPattern(TestContext ctx) {
        ctx.setBlockState(POS, IC2Blocks.PATTERN_STORAGE_BLOCK.getDefaultState());
        ctx.setBlockState(POS.east(), IC2Blocks.PATTERN_STORAGE_BLOCK.getDefaultState());
        TileEntityPatternStorage first = ctx.getBlockEntity(POS);
        TileEntityPatternStorage second = ctx.getBlockEntity(POS.east());
        first.addPattern(new ItemStack(Items.DIRT));
        ItemStack memory = new ItemStack(Registries.ITEM.get(Identifier.of("ic2", "crystal_memory")));
        first.diskSlot.put(memory);
        first.onGuiEvent(null, "export");
        ItemStack written = first.diskSlot.get();
        ctx.assertTrue(((ItemCrystalMemory) written.getItem()).readItemStack(written).isOf(Items.DIRT), "crystal not written");

        first.diskSlot.clear();
        second.diskSlot.put(written);
        second.onGuiEvent(null, "import");
        ctx.assertTrue(second.getPatterns().size() == 1 && second.getPatterns().get(0).isOf(Items.DIRT), "not imported");
        ctx.complete();
    }
}
