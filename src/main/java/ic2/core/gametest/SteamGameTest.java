package ic2.core.gametest;

import ic2.core.block.heatgenerator.tileentity.TileEntityElectricHeatGenerator;
import ic2.core.block.kineticgenerator.tileentity.TileEntitySteamKineticGenerator;
import ic2.core.block.steam.TileEntityBurningBox;
import ic2.core.block.steam.TileEntityCokeKiln;
import ic2.core.block.steam.TileEntityCokeKilnGrate;
import ic2.core.block.steam.TileEntityCokeKilnHatch;
import ic2.core.block.steam.TileEntitySteamGenerator;
import ic2.core.init.IC2Blocks;
import ic2.core.init.IC2ClassicBlocks;
import ic2.core.init.IC2ItemVariants;
import ic2.core.init.IC2Items;
import ic2.core.ref.FluidName;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

/**
 * The steam age blocks in a running world: the firebox, the coke kiln multiblock and the steam
 * boiler. Run with {@code ./gradlew runGametest}.
 */
public class SteamGameTest implements FabricGameTest {
    private static final BlockPos POS = new BlockPos(2, 2, 2);

    /** Coal burns for a quarter of its furnace time and the top warms along the cubic curve. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void burningBoxBurnsCoal(TestContext ctx) {
        ctx.setBlockState(POS, IC2Blocks.BURNING_BOX_BLOCK.getDefaultState());
        TileEntityBurningBox box = ctx.getBlockEntity(POS);
        box.fuelSlot.put(new ItemStack(Items.COAL, 2));
        box.tick();
        ctx.assertTrue(box.fuel == 399, "fuel " + box.fuel);
        ctx.assertTrue(box.fuelSlot.get().getCount() == 1, "coal used " + box.fuelSlot.get());
        // 1400 - 0.008 * 55^3
        ctx.assertTrue(box.getProvidedHeat(Direction.UP) == 69, "heat " + box.getProvidedHeat(Direction.UP));
        ctx.assertTrue(box.getProvidedHeat(Direction.NORTH) == 0, "heat to the side");
        ctx.assertTrue(box.getActive(), "not burning");
        ctx.complete();
    }

    /** A built kiln bakes coal from the hatch into coke and creosote in 1800 ticks; a broken one does nothing. */
    @GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 100)
    public void cokeKilnBakesCoal(TestContext ctx) {
        BlockPos centre = new BlockPos(2, 2, 2);
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    ctx.setBlockState(centre.add(dx, dy, dz), IC2ClassicBlocks.REFRACTORY_BRICKS.getDefaultState());
                }
            }
        }
        ctx.setBlockState(centre, net.minecraft.block.Blocks.AIR.getDefaultState());
        ctx.setBlockState(centre.down(), IC2Blocks.COKE_GRATE_BLOCK.getDefaultState());
        ctx.setBlockState(centre.up(), IC2Blocks.COKE_HATCH_BLOCK.getDefaultState());
        BlockPos kilnPos = centre.north();
        ctx.setBlockState(kilnPos, IC2Blocks.COKE_KILN_BLOCK.getDefaultState());

        TileEntityCokeKiln kiln = ctx.getBlockEntity(kilnPos);
        kiln.setFacing(Direction.NORTH);
        TileEntityCokeKilnHatch hatch = ctx.getBlockEntity(centre.up());
        TileEntityCokeKilnGrate grate = ctx.getBlockEntity(centre.down());
        hatch.getInventory().put(new ItemStack(Items.COAL, 2));

        ctx.assertTrue(kiln.hasValidStructure(), "structure not recognised");
        for (int i = 0; i < 20 * 89; i++) {
            kiln.tick();
        }
        ctx.assertTrue(kiln.getActive(), "kiln not working");
        ctx.assertTrue(grate.getFluidTank().getFluidAmount() == 0, "done early");
        for (int i = 0; i < 20; i++) {
            kiln.tick();
        }
        ItemStack output = kiln.getStack(0);
        ctx.assertTrue(output.isOf(IC2Items.COKE) && output.getCount() == 1, "output " + output);
        ctx.assertTrue(grate.getFluidTank().getFluidAmount() == 500, "creosote " + grate.getFluidTank().getFluidAmount());
        ctx.assertTrue(grate.getFluidTank().getFluid().getFluid() == FluidName.creosote.getInstance(), "not creosote");
        ctx.assertTrue(hatch.getInventory().get().getCount() == 1, "hatch " + hatch.getInventory().get());

        // breaking a wall stops it
        ctx.setBlockState(centre.east(), net.minecraft.block.Blocks.STONE.getDefaultState());
        ctx.assertTrue(!kiln.hasValidStructure(), "broken kiln still counts as built");
        ctx.complete();
    }

    private static TileEntityElectricHeatGenerator heater(TestContext ctx, BlockPos at, Direction facing) {
        ctx.setBlockState(at, IC2Blocks.ELECTRIC_HEAT_BLOCK.getDefaultState());
        TileEntityElectricHeatGenerator heater = ctx.getBlockEntity(at);
        heater.setFacing(facing);
        for (int i = 0; i < 10; i++) {
            heater.coilSlot.put(i, new ItemStack(IC2ItemVariants.get("crafting", "coil")));
        }
        heater.injectEnergy(Direction.UP, 10000, 128);
        return heater;
    }

    /** Below boiling point at zero pressure the boiler only passes its water on. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void coldBoilerPassesWaterOn(TestContext ctx) {
        ctx.setBlockState(POS, IC2Blocks.STEAM_GENERATOR_BLOCK.getDefaultState());
        ctx.setBlockState(POS.east(), IC2Blocks.STEAM_GENERATOR_BLOCK.getDefaultState());
        TileEntitySteamGenerator boiler = ctx.getBlockEntity(POS);
        TileEntitySteamGenerator next = ctx.getBlockEntity(POS.east());
        TileEntityElectricHeatGenerator heater = heater(ctx, POS.west(), Direction.EAST);
        boiler.waterTank.fillInternal(new FluidStack(FluidRegistry.getFluid("water"), 1000), true);
        // the first tick without heat settles it at the temperature of the biome
        boiler.tick();
        boiler.setInputMB(10);

        float before = boiler.getSystemHeat();
        heater.tick();
        boiler.tick();
        ctx.assertTrue(boiler.getHeatInput() == 100, "heat input " + boiler.getHeatInput());
        ctx.assertTrue(Math.abs(boiler.getSystemHeat() - before - 0.05F) < 0.051F, "heated to " + boiler.getSystemHeat());
        ctx.assertTrue(next.waterTank.getFluidAmount() == 10, "passed on " + next.waterTank.getFluidAmount());
        ctx.assertTrue(boiler.waterTank.getFluidAmount() == 990, "left " + boiler.waterTank.getFluidAmount());
        ctx.complete();
    }

    /** At 100 C, 100 HU boil one mB of water into 100 mB of steam for the turbine next to it. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void hotBoilerMakesSteam(TestContext ctx) {
        ctx.setBlockState(POS, IC2Blocks.STEAM_GENERATOR_BLOCK.getDefaultState());
        ctx.setBlockState(POS.east(), IC2Blocks.STEAM_KINETIC_BLOCK.getDefaultState());
        TileEntitySteamGenerator boiler = ctx.getBlockEntity(POS);
        TileEntitySteamKineticGenerator turbine = ctx.getBlockEntity(POS.east());
        TileEntityElectricHeatGenerator heater = heater(ctx, POS.west(), Direction.EAST);

        NbtCompound nbt = new NbtCompound();
        nbt.putFloat("systemheat", 100.0F);
        boiler.readNbt(nbt, ctx.getWorld().getRegistryManager());
        boiler.waterTank.fillInternal(new FluidStack(FluidRegistry.getFluid("water"), 1000), true);
        boiler.setInputMB(10);

        heater.tick();
        boiler.tick();
        ctx.assertTrue(boiler.getOutputMB() == 100, "output " + boiler.getOutputMB());
        ctx.assertTrue(turbine.getSteamTank().getFluidAmount() == 100, "turbine got " + turbine.getSteamTank().getFluidAmount());
        ctx.assertTrue(turbine.getSteamTank().getFluid().getFluid() == FluidName.steam.getInstance(), "not steam");
        ctx.assertTrue(boiler.waterTank.getFluidAmount() == 999, "left " + boiler.waterTank.getFluidAmount());
        NbtCompound saved = new NbtCompound();
        boiler.writeNbt(saved, ctx.getWorld().getRegistryManager());
        ctx.assertTrue(saved.getInt("calcification") == 1, "calcification " + saved.getInt("calcification"));
        ctx.complete();
    }

    /** The window's buttons move the water input and the pressure valve within their limits. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void boilerButtonsSetInputAndPressure(TestContext ctx) {
        ctx.setBlockState(POS, IC2Blocks.STEAM_GENERATOR_BLOCK.getDefaultState());
        TileEntitySteamGenerator boiler = ctx.getBlockEntity(POS);
        boiler.onGuiEvent(null, "input:1000");
        boiler.onGuiEvent(null, "input:1000");
        boiler.onGuiEvent(null, "input:-10");
        boiler.onGuiEvent(null, "pressure:100");
        boiler.onGuiEvent(null, "pressure:100");
        boiler.onGuiEvent(null, "pressure:100");
        boiler.onGuiEvent(null, "pressure:100");
        ctx.assertTrue(boiler.getInputMB() == 990, "input " + boiler.getInputMB());
        ctx.assertTrue(boiler.getPressure() == 300, "pressure " + boiler.getPressure());
        ctx.complete();
    }
}
