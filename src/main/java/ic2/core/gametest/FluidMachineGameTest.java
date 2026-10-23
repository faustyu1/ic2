package ic2.core.gametest;

import ic2.core.block.kineticgenerator.tileentity.TileEntitySteamKineticGenerator;
import ic2.core.block.machine.tileentity.TileEntityCondenser;
import ic2.core.block.machine.tileentity.TileEntityElectrolyzer;
import ic2.core.block.machine.tileentity.TileEntityFluidBottler;
import ic2.core.block.machine.tileentity.TileEntityFluidDistributor;
import ic2.core.block.machine.tileentity.TileEntityFluidRegulator;
import ic2.core.block.machine.tileentity.TileEntityLiquidHeatExchanger;
import ic2.core.block.machine.tileentity.TileEntityOreWashing;
import ic2.core.block.machine.tileentity.TileEntityPump;
import ic2.core.block.machine.tileentity.TileEntityTank;
import ic2.core.block.machine.tileentity.TileEntityWeightedFluidDistributor;
import ic2.core.init.IC2Blocks;
import ic2.core.init.IC2ItemVariants;
import ic2.core.ref.FluidName;
import ic2.core.util.LiquidUtil;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

/**
 * The fluid machines in a running world, and fluid moving between blocks.
 * Run with {@code ./gradlew runGametest}.
 */
public class FluidMachineGameTest implements FabricGameTest {
    private static final BlockPos POS = new BlockPos(2, 2, 2);

    private static FluidStack water(int amount) {
        return new FluidStack(FluidRegistry.getFluid("water"), amount);
    }

    private static TileEntityTank tank(TestContext ctx, BlockPos at) {
        ctx.setBlockState(at, IC2Blocks.TANK_BLOCK.getDefaultState());
        return ctx.getBlockEntity(at);
    }

    /** 100 mB of steam a tick condense, 100 more for each vent at 2 EU; 10 000 mB make 100 mB of distilled water. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void condenserTurnsSteamIntoDistilledWater(TestContext ctx) {
        ctx.setBlockState(POS, IC2Blocks.CONDENSER_BLOCK.getDefaultState());
        TileEntityCondenser condenser = ctx.getBlockEntity(POS);
        ctx.assertTrue(LiquidUtil.fillTile(condenser, Direction.UP, new FluidStack(FluidName.steam.getInstance(), 20000), false) == 20000,
                "steam refused");
        ItemStack vent = new ItemStack(Registries.ITEM.get(Identifier.of("ic2", "heat_vent")));
        condenser.ventSlots.put(0, vent.copy());
        condenser.ventSlots.put(1, vent.copy());
        condenser.injectEnergy(Direction.UP, 1000, 32);

        condenser.tick();
        ctx.assertTrue(condenser.progress == 300, "progress " + condenser.progress);
        ctx.assertTrue(condenser.getEnergy() == 996, "energy " + condenser.getEnergy());
        for (int i = 0; i < 34; i++) {
            condenser.tick();
        }
        ctx.assertTrue(condenser.getOutputTank().getFluidAmount() == 100, "water " + condenser.getOutputTank().getFluidAmount());
        ctx.assertTrue(condenser.getOutputTank().getFluid().getFluid() == FluidName.distilled_water.getInstance(), "not distilled");
        ctx.complete();
    }

    /** The turbine hands its used steam to the condenser beside it instead of venting it. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void turbineFeedsTheCondenser(TestContext ctx) {
        ctx.setBlockState(POS, IC2Blocks.STEAM_KINETIC_BLOCK.getDefaultState());
        ctx.setBlockState(POS.up(), IC2Blocks.CONDENSER_BLOCK.getDefaultState());
        TileEntitySteamKineticGenerator turbine = ctx.getBlockEntity(POS);
        TileEntityCondenser condenser = ctx.getBlockEntity(POS.up());
        turbine.turbineSlot.put(new ItemStack(IC2ItemVariants.get("crafting", "steam_turbine")));
        LiquidUtil.fillTile(turbine, Direction.WEST, new FluidStack(FluidName.steam.getInstance(), 100), false);
        turbine.tick();
        // a tenth of plain steam condenses inside the turbine
        ctx.assertTrue(condenser.getInputTank().getFluidAmount() == 90, "condenser got " + condenser.getInputTank().getFluidAmount());
        ctx.assertTrue(!turbine.isVentingSteam(), "turbine vented");
        ctx.complete();
    }

    /** Distributing shares evenly between the sides; concentrating pushes all out of the face. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void distributorSharesAndConcentrates(TestContext ctx) {
        ctx.setBlockState(POS, IC2Blocks.FLUID_DISTRIBUTOR_BLOCK.getDefaultState());
        TileEntityFluidDistributor distributor = ctx.getBlockEntity(POS);
        distributor.setFacing(Direction.NORTH);
        TileEntityTank east = tank(ctx, POS.east());
        TileEntityTank west = tank(ctx, POS.west());
        TileEntityTank north = tank(ctx, POS.north());

        ctx.assertTrue(LiquidUtil.fillTile(distributor, Direction.EAST, water(100), true) == 0, "takes fluid through a side");
        ctx.assertTrue(LiquidUtil.fillTile(distributor, Direction.NORTH, water(1000), false) == 1000, "refuses fluid through its face");
        distributor.tick();
        ctx.assertTrue(east.getTank().getFluidAmount() == 500 && west.getTank().getFluidAmount() == 500,
                "shared " + east.getTank().getFluidAmount() + "/" + west.getTank().getFluidAmount());
        ctx.assertTrue(north.getTank().getFluidAmount() == 0, "gave to its face");

        distributor.onGuiEvent(null, "mode");
        ctx.assertTrue(LiquidUtil.fillTile(distributor, Direction.EAST, water(800), false) == 800, "concentrating refuses the side");
        distributor.tick();
        ctx.assertTrue(north.getTank().getFluidAmount() == 800, "concentrated " + north.getTank().getFluidAmount());
        ctx.complete();
    }

    /** The weighted distributor fills its first priority before the second. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void weightedDistributorFollowsPriority(TestContext ctx) {
        ctx.setBlockState(POS, IC2Blocks.WEIGHTED_FLUID_DISTRIBUTOR_BLOCK.getDefaultState());
        TileEntityWeightedFluidDistributor distributor = ctx.getBlockEntity(POS);
        distributor.setFacing(Direction.NORTH);
        TileEntityTank east = tank(ctx, POS.east());
        TileEntityTank west = tank(ctx, POS.west());

        // columns are U N S W E D: west first, east second
        distributor.onGuiEvent(null, "prio:0:3");
        distributor.onGuiEvent(null, "prio:1:4");
        ctx.assertTrue(distributor.getPriority().equals(java.util.List.of(Direction.WEST, Direction.EAST)), "priority " + distributor.getPriority());
        // clicking a side into a higher row moves it there
        distributor.onGuiEvent(null, "prio:0:4");
        ctx.assertTrue(distributor.getPriority().equals(java.util.List.of(Direction.EAST)), "priority " + distributor.getPriority());
        distributor.onGuiEvent(null, "prio:1:3");

        LiquidUtil.fillTile(distributor, Direction.NORTH, water(1000), false);
        distributor.tick();
        ctx.assertTrue(east.getTank().getFluidAmount() == 1000 && west.getTank().getFluidAmount() == 0,
                "filled " + east.getTank().getFluidAmount() + "/" + west.getTank().getFluidAmount());
        ctx.complete();
    }

    /** The regulator pushes its set portion out of its face each tick in tick mode, for 10 EU. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void regulatorMetersFluid(TestContext ctx) {
        ctx.setBlockState(POS, IC2Blocks.FLUID_REGULATOR_BLOCK.getDefaultState());
        TileEntityFluidRegulator regulator = ctx.getBlockEntity(POS);
        regulator.setFacing(Direction.EAST);
        TileEntityTank east = tank(ctx, POS.east());
        regulator.injectEnergy(Direction.UP, 1000, 128);
        LiquidUtil.fillTile(regulator, Direction.WEST, water(1000), false);
        regulator.onGuiEvent(null, "out:100");
        regulator.onGuiEvent(null, "tick");

        regulator.tick();
        regulator.tick();
        ctx.assertTrue(east.getTank().getFluidAmount() == 200, "metered " + east.getTank().getFluidAmount());
        ctx.assertTrue(regulator.getEnergy() == 980, "energy " + regulator.getEnergy());
        ctx.complete();
    }

    /** A water bucket is emptied into the tank in 100 ticks, then a cell is filled from it. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void bottlerEmptiesThenFills(TestContext ctx) {
        ctx.setBlockState(POS, IC2Blocks.FLUID_BOTTLER_BLOCK.getDefaultState());
        TileEntityFluidBottler bottler = ctx.getBlockEntity(POS);
        bottler.injectEnergy(Direction.UP, 1000, 32);
        bottler.drainInputSlot.put(new ItemStack(Items.WATER_BUCKET));

        for (int i = 0; i < 100; i++) {
            bottler.tick();
        }
        ctx.assertTrue(bottler.fluidTank.getFluidAmount() == 1000, "tank " + bottler.fluidTank.getFluidAmount());
        ctx.assertTrue(bottler.getOutput(0).isOf(Items.BUCKET), "output " + bottler.getOutput(0));
        ctx.complete();
    }

    /** 40 mB of water become 26 mB of hydrogen below and 13 mB of oxygen above in 200 ticks. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void electrolyzerSplitsWater(TestContext ctx) {
        ctx.setBlockState(POS, IC2Blocks.ELECTROLYZER_BLOCK.getDefaultState());
        TileEntityElectrolyzer electrolyzer = ctx.getBlockEntity(POS);
        TileEntityTank below = tank(ctx, POS.down());
        TileEntityTank above = tank(ctx, POS.up());
        electrolyzer.injectEnergy(Direction.NORTH, 32000, 128);
        LiquidUtil.fillTile(electrolyzer, Direction.NORTH, water(1000), false);

        for (int i = 0; i < 200; i++) {
            electrolyzer.tick();
        }
        ctx.assertTrue(below.getTank().getFluidAmount() == 26 && below.getTank().getFluid().getFluid() == FluidName.hydrogen.getInstance(),
                "below " + LiquidUtil.toStringSafe(below.getTank().getFluid()));
        ctx.assertTrue(above.getTank().getFluidAmount() == 13 && above.getTank().getFluid().getFluid() == FluidName.oxygen.getInstance(),
                "above " + LiquidUtil.toStringSafe(above.getTank().getFluid()));
        ctx.assertTrue(electrolyzer.getInput().getFluidAmount() == 960, "water left " + electrolyzer.getInput().getFluidAmount());
        ctx.assertTrue(electrolyzer.getEnergy() == 32000 - 200 * 32, "energy " + electrolyzer.getEnergy());
        ctx.complete();
    }

    /** Ten heat conductors: 100 HU a tick out of 5 mB of hot coolant, which leaves as coolant. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void heatExchangerCoolsHotCoolant(TestContext ctx) {
        ctx.setBlockState(POS, IC2Blocks.LIQUID_HEAT_EXCHANGER_BLOCK.getDefaultState());
        TileEntityLiquidHeatExchanger exchanger = ctx.getBlockEntity(POS);
        for (int i = 0; i < 10; i++) {
            exchanger.heatexchangerslots.put(i, new ItemStack(IC2ItemVariants.get("crafting", "heat_conductor")));
        }
        ctx.assertTrue(LiquidUtil.fillTile(exchanger, Direction.UP, new FluidStack(FluidName.hot_coolant.getInstance(), 1000), false) == 1000,
                "hot coolant refused");
        exchanger.tick();
        ctx.assertTrue(exchanger.getHeatBuffer() == 100, "heat " + exchanger.getHeatBuffer());
        ctx.assertTrue(exchanger.getOutputTank().getFluidAmount() == 5, "coolant " + exchanger.getOutputTank().getFluidAmount());
        ctx.assertTrue(exchanger.getOutputTank().getFluid().getFluid() == FluidName.coolant.getInstance(), "not coolant");
        ctx.complete();
    }

    /** The pump takes the water source in front of it every 20 ticks. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void pumpDrainsASource(TestContext ctx) {
        ctx.setBlockState(POS, IC2Blocks.PUMP_BLOCK.getDefaultState());
        for (Direction dir : new Direction[] {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.DOWN}) {
            ctx.setBlockState(POS.east().offset(dir), Blocks.STONE.getDefaultState());
        }
        ctx.setBlockState(POS.east(), Blocks.WATER.getDefaultState());
        TileEntityPump pump = ctx.getBlockEntity(POS);
        pump.setFacing(Direction.EAST);
        pump.injectEnergy(Direction.UP, 1000, 32);

        for (int i = 0; i < 21; i++) {
            pump.tick();
        }
        ctx.assertTrue(pump.getFluidTank().getFluidAmount() == 1000, "pumped " + pump.getFluidTank().getFluidAmount());
        ctx.assertTrue(ctx.getBlockState(POS.east()).isAir(), "source still there");
        ctx.complete();
    }

    /** Other mods reach an IC2 tank through Fabric's fluid storage; an aborted transaction leaves it as it was. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void tanksAreFabricFluidStorage(TestContext ctx) {
        TileEntityTank tank = tank(ctx, POS);
        Storage<FluidVariant> storage = FluidStorage.SIDED.find(ctx.getWorld(), ctx.getAbsolutePos(POS), Direction.UP);
        ctx.assertTrue(storage != null, "no fluid storage");

        try (Transaction tx = Transaction.openOuter()) {
            storage.insert(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, tx);
        }
        ctx.assertTrue(tank.getTank().getFluidAmount() == 0, "aborted insert kept " + tank.getTank().getFluidAmount());

        try (Transaction tx = Transaction.openOuter()) {
            long inserted = storage.insert(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, tx);
            ctx.assertTrue(inserted == FluidConstants.BUCKET, "inserted " + inserted);
            tx.commit();
        }
        ctx.assertTrue(tank.getTank().getFluidAmount() == 1000, "tank " + tank.getTank().getFluidAmount());
        ctx.complete();
    }

    /** A fluid ejector moves 50 mB a tick into the next tank. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void fluidEjectorEmptiesATank(TestContext ctx) {
        TileEntityTank source = tank(ctx, POS);
        TileEntityTank target = tank(ctx, POS.east());
        source.getTank().fillInternal(water(1000), true);
        source.upgradeSlot.put(0, new ItemStack(IC2ItemVariants.get("upgrade", "fluid_ejector")));
        source.tick();
        ctx.assertTrue(target.getTank().getFluidAmount() == 50, "moved " + target.getTank().getFluidAmount());
        ctx.complete();
    }

    /** The ore washing plant waits for its water, takes the 1000 mB the recipe names, and fills from buckets. */
    @GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 200)
    public void oreWashingPlantUsesWater(TestContext ctx) {
        ctx.setBlockState(POS, IC2Blocks.ORE_WASHING_BLOCK.getDefaultState());
        TileEntityOreWashing washer = ctx.getBlockEntity(POS);
        washer.inputSlot.put(new ItemStack(IC2ItemVariants.get("crushed", "iron"), 2));
        for (int i = 0; i < 600; i++) {
            washer.injectEnergy(Direction.UP, 100000, 2048);
            washer.tick();
        }
        ctx.assertTrue(washer.outputSlot.isEmpty(), "washed without water: " + washer.outputSlot.get());
        ctx.assertTrue(washer.inputSlot.get().getCount() == 2, "ore used without water");

        washer.fluidSlot.put(new ItemStack(Items.WATER_BUCKET));
        washer.tick();
        ctx.assertTrue(washer.fluidTank.getFluidAmount() == 1000, "tank " + washer.fluidTank.getFluidAmount());
        ctx.assertTrue(washer.cellSlot.get().isOf(Items.BUCKET), "no empty bucket: " + washer.cellSlot.get());

        for (int i = 0; i < 600; i++) {
            washer.injectEnergy(Direction.UP, 100000, 2048);
            washer.tick();
        }
        ctx.assertTrue(washer.inputSlot.get().getCount() == 1, "washed " + (2 - washer.inputSlot.get().getCount()) + " ores");
        ctx.assertTrue(washer.fluidTank.getFluidAmount() == 0, "water left " + washer.fluidTank.getFluidAmount());
        ctx.assertTrue(washer.outputSlot.get(0).isOf(IC2ItemVariants.get("purified", "iron")), "output " + washer.outputSlot.get(0));

        // pipes and other mods fill it through the fluid handler
        int filled = washer.getFluidHandler(Direction.UP).fill(water(5000), true);
        ctx.assertTrue(filled == 5000 && washer.fluidTank.getFluidAmount() == 5000, "piped in " + filled);
        ctx.complete();
    }
}
