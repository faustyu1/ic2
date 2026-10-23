package ic2.core.gametest;

import ic2.core.block.generator.tileentity.TileEntityKineticGenerator;
import ic2.core.block.generator.tileentity.TileEntityStirlingGenerator;
import ic2.core.block.heatgenerator.tileentity.TileEntityElectricHeatGenerator;
import ic2.core.block.kineticgenerator.tileentity.TileEntityElectricKineticGenerator;
import ic2.core.block.kineticgenerator.tileentity.TileEntitySteamKineticGenerator;
import ic2.core.block.kineticgenerator.tileentity.TileEntityStirlingKineticGenerator;
import ic2.core.block.kineticgenerator.tileentity.TileEntityWindKineticGenerator;
import ic2.core.init.IC2Blocks;
import ic2.core.init.IC2ItemVariants;
import ic2.core.ref.FluidName;
import ic2.core.util.LiquidUtil;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

/**
 * The kinetic generators in a running world: motors, heat, steam and wind turned into kU, and kU
 * and heat turned into EU. Run with {@code ./gradlew runGametest}.
 */
public class KineticGameTest implements FabricGameTest {
    private static final BlockPos POS = new BlockPos(2, 2, 2);

    private static ItemStack crafting(String kind) {
        return new ItemStack(IC2ItemVariants.get("crafting", kind));
    }

    /** Four EU make 16 kU; three motors let 300 kU a tick out of the front and nothing elsewhere. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void electricMotorTurnsEuIntoKu(TestContext ctx) {
        ctx.setBlockState(POS, IC2Blocks.ELECTRIC_KINETIC_BLOCK.getDefaultState());
        TileEntityElectricKineticGenerator motor = ctx.getBlockEntity(POS);
        motor.setFacing(Direction.EAST);
        for (int i = 0; i < 3; i++) {
            motor.slotMotor.put(i, crafting("electric_motor"));
        }
        ctx.assertTrue(motor.getMaxKU() == 300, "max " + motor.getMaxKU());
        ctx.assertTrue(motor.injectEnergy(Direction.WEST, 1000, 128) == 0, "energy refused");
        ctx.assertTrue(!motor.acceptsEnergyFrom(null, Direction.EAST), "takes energy through its front");

        motor.tick();
        ctx.assertTrue(motor.ku == 1000, "ku " + motor.ku);
        ctx.assertTrue(motor.getEnergy() == 750, "energy " + motor.getEnergy());
        ctx.assertTrue(motor.drawKineticEnergy(Direction.WEST, 1000, false) == 0, "kU out of the back");
        ctx.assertTrue(motor.drawKineticEnergy(Direction.EAST, 1000, false) == 300, "front gave the wrong amount");
        ctx.complete();
    }

    /** The kinetic generator offers a quarter EU for each kU of the motor in front of it. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void kineticGeneratorTurnsKuIntoEu(TestContext ctx) {
        ctx.setBlockState(POS, IC2Blocks.ELECTRIC_KINETIC_BLOCK.getDefaultState());
        ctx.setBlockState(POS.east(), IC2Blocks.KINETIC_BLOCK.getDefaultState());
        TileEntityElectricKineticGenerator motor = ctx.getBlockEntity(POS);
        TileEntityKineticGenerator generator = ctx.getBlockEntity(POS.east());
        motor.setFacing(Direction.EAST);
        generator.setFacing(Direction.WEST);
        motor.slotMotor.put(0, crafting("electric_motor"));
        motor.injectEnergy(Direction.WEST, 1000, 128);
        motor.tick();

        ctx.assertTrue(generator.getOfferedEnergy() == 25, "offered " + generator.getOfferedEnergy());
        generator.drawEnergy(25);
        ctx.assertTrue(motor.ku == 900, "motor kept " + motor.ku);
        ctx.complete();
    }

    /** Heat from an electric heater beside it becomes kU and hot water; coolant it leaves to the heat exchangers. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void stirlingKineticGeneratorHeatsWater(TestContext ctx) {
        ctx.setBlockState(POS, IC2Blocks.STIRLING_KINETIC_BLOCK.getDefaultState());
        ctx.setBlockState(POS.west(), IC2Blocks.ELECTRIC_HEAT_BLOCK.getDefaultState());
        TileEntityStirlingKineticGenerator stirling = ctx.getBlockEntity(POS);
        TileEntityElectricHeatGenerator heater = ctx.getBlockEntity(POS.west());
        stirling.setFacing(Direction.EAST);
        heater.setFacing(Direction.EAST);
        for (int i = 0; i < 10; i++) {
            heater.coilSlot.put(i, crafting("coil"));
        }
        heater.injectEnergy(Direction.WEST, 10000, 128);
        ctx.assertTrue(LiquidUtil.fillTile(stirling, Direction.UP, new FluidStack(FluidName.coolant.getInstance(), 1000), true) == 0,
                "takes coolant, which the heat exchangers turn both ways");
        ctx.assertTrue(LiquidUtil.fillTile(stirling, Direction.UP, new FluidStack(FluidRegistry.getFluid("water"), 1000), false) == 1000,
                "refuses water");

        for (int i = 0; i < 40; i++) {
            heater.tick();
            stirling.tick();
        }
        ctx.assertTrue(stirling.getKuBuffer() > 0, "no kU");
        ctx.assertTrue(stirling.outputTank.getFluidAmount() > 0, "no hot water");
        ctx.assertTrue(stirling.outputTank.getFluid().getFluid() == FluidName.hot_water.getInstance(), "wrong output fluid");
        ctx.assertTrue(stirling.drawKineticEnergy(Direction.WEST, 100, true) == 0, "kU out of the back");
        ctx.assertTrue(stirling.drawKineticEnergy(Direction.EAST, 100, true) > 0, "no kU out of the front");
        ctx.complete();
    }

    /** Superheated steam gives 4 kU a mB, and goes on as steam into the next turbine. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void steamTurbineRunsOnSuperheatedSteam(TestContext ctx) {
        ctx.setBlockState(POS, IC2Blocks.STEAM_KINETIC_BLOCK.getDefaultState());
        ctx.setBlockState(POS.up(), IC2Blocks.STEAM_KINETIC_BLOCK.getDefaultState());
        TileEntitySteamKineticGenerator turbine = ctx.getBlockEntity(POS);
        TileEntitySteamKineticGenerator next = ctx.getBlockEntity(POS.up());
        turbine.setFacing(Direction.EAST);
        turbine.turbineSlot.put(crafting("steam_turbine"));

        int filled = LiquidUtil.fillTile(turbine, Direction.WEST, new FluidStack(FluidName.superheated_steam.getInstance(), 100), false);
        ctx.assertTrue(filled == 100, "filled " + filled);
        ctx.assertTrue(LiquidUtil.fillTile(turbine, Direction.WEST, new FluidStack(FluidName.coolant.getInstance(), 100), true) == 0,
                "turbine takes coolant");

        turbine.tick();
        ctx.assertTrue(turbine.getKUoutput() == 400, "output " + turbine.getKUoutput());
        ctx.assertTrue(turbine.drawKineticEnergy(Direction.EAST, 1, false) == 400, "front gave the wrong amount");
        ctx.assertTrue(!turbine.isVentingSteam(), "steam was vented");
        ctx.assertTrue(next.getSteamTank().getFluidAmount() == 100, "next turbine got " + next.getSteamTank().getFluidAmount());
        ctx.assertTrue(next.getSteamTank().getFluid().getFluid() == FluidName.steam.getInstance(), "went on as superheated steam");
        ctx.complete();
    }

    /** Heat into EU at one for two. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void stirlingGeneratorTurnsHeatIntoEu(TestContext ctx) {
        ctx.setBlockState(POS, IC2Blocks.STIRLING_BLOCK.getDefaultState());
        ctx.setBlockState(POS.west(), IC2Blocks.ELECTRIC_HEAT_BLOCK.getDefaultState());
        TileEntityStirlingGenerator generator = ctx.getBlockEntity(POS);
        TileEntityElectricHeatGenerator heater = ctx.getBlockEntity(POS.west());
        generator.setFacing(Direction.WEST);
        heater.setFacing(Direction.EAST);
        heater.coilSlot.put(0, crafting("coil"));
        heater.injectEnergy(Direction.WEST, 1000, 128);
        heater.tick();

        ctx.assertTrue(generator.getOfferedEnergy() == 5, "offered " + generator.getOfferedEnergy());
        ctx.complete();
    }

    /** A rotor in the open air turns; one boxed in by stone does not. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void windRotorNeedsRoom(TestContext ctx) {
        ctx.setBlockState(POS, IC2Blocks.WIND_KINETIC_BLOCK.getDefaultState());
        TileEntityWindKineticGenerator wind = ctx.getBlockEntity(POS);
        wind.setFacing(Direction.EAST);
        wind.rotorSlot.put(new ItemStack(net.minecraft.registry.Registries.ITEM.get(net.minecraft.util.Identifier.of("ic2", "rotor_wood"))));
        ctx.assertTrue(wind.getRotorDiameter() == 5, "diameter " + wind.getRotorDiameter());
        ctx.assertTrue(wind.rotorHasSpace(), "no room in the air");

        ctx.setBlockState(POS.east().up(), Blocks.STONE.getDefaultState());
        ctx.assertTrue(!wind.rotorHasSpace(), "a stone in the rotor's way is ignored");
        ctx.complete();
    }
}
