package ic2.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import ic2.api.recipe.ILiquidHeatExchangerManager;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import org.junit.Before;
import org.junit.Test;

/**
 * The heat exchange table: which fluid turns into which and how much heat one millibucket of it
 * carries. Getting the direction wrong would let a reactor cool itself with its own hot coolant.
 */
public class LiquidHeatExchangerManagerTest {

    @Before
    public void registerFluids() {
        register("ic2coolant", 300);
        register("ic2hot_coolant", 1200);
        register("water", 300);
        register("ic2hot_water", 350);
    }

    @Test
    public void heatingUpTurnsTheColdFluidIntoTheHotOne() {
        LiquidHeatExchangerManager heatup = new LiquidHeatExchangerManager(true);
        heatup.addFluid("ic2coolant", "ic2hot_coolant", 20);

        ILiquidHeatExchangerManager.HeatExchangeProperty property =
                heatup.getHeatExchangeProperty(FluidRegistry.getFluid("ic2coolant"));

        assertNotNull(property);
        assertEquals("ic2hot_coolant", property.outputFluid.getName());
        assertEquals(20, property.huPerMB);
        assertTrue(heatup.acceptsFluid(FluidRegistry.getFluid("ic2coolant")));
        assertFalse(heatup.acceptsFluid(FluidRegistry.getFluid("ic2hot_coolant")));
    }

    @Test
    public void aRecipeRegisteredTheWrongWayRoundIsRefused() {
        LiquidHeatExchangerManager heatup = new LiquidHeatExchangerManager(true);
        heatup.addFluid("ic2hot_coolant", "ic2coolant", 20);

        assertNull(heatup.getHeatExchangeProperty(FluidRegistry.getFluid("ic2hot_coolant")));
    }

    @Test
    public void coolingDownRefusesToTurnAColdFluidIntoAWarmOne() {
        LiquidHeatExchangerManager cooldown = new LiquidHeatExchangerManager(false);
        cooldown.addFluid("ic2coolant", "ic2hot_coolant", 20);
        assertNull(cooldown.getHeatExchangeProperty(FluidRegistry.getFluid("ic2coolant")));

        cooldown.addFluid("ic2hot_coolant", "ic2coolant", 20);
        assertNotNull(cooldown.getHeatExchangeProperty(FluidRegistry.getFluid("ic2hot_coolant")));
    }

    @Test
    public void aFluidWithoutAnEnergyValueIsIgnored() {
        LiquidHeatExchangerManager heatup = new LiquidHeatExchangerManager(true);
        heatup.addFluid("water", "ic2hot_water", 0);

        assertFalse(heatup.acceptsFluid(FluidRegistry.getFluid("water")));
    }

    @Test
    public void theSameFluidIsNotRegisteredTwice() {
        LiquidHeatExchangerManager heatup = new LiquidHeatExchangerManager(true);
        heatup.addFluid("ic2coolant", "ic2hot_coolant", 20);
        heatup.addFluid("ic2coolant", "ic2hot_water", 5);

        assertEquals(20, heatup.getHeatExchangeProperty(FluidRegistry.getFluid("ic2coolant")).huPerMB);
    }

    private static void register(String name, int temperature) {
        if (FluidRegistry.getFluid(name) == null) {
            FluidRegistry.registerFluid(new Fluid(name).setTemperature(temperature));
        }
    }
}
