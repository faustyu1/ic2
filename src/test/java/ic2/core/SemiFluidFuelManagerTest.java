package ic2.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import ic2.api.recipe.ISemiFluidFuelManager;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import org.junit.Test;

/** The semifluid generator's fuel table: energy per millibucket and the rate it may leave at. */
public class SemiFluidFuelManagerTest {

    @Test
    public void aFuelKeepsBothOfItsNumbers() {
        SemiFluidFuelManager manager = new SemiFluidFuelManager();
        manager.addFluid("ic2biogas", 32L, 16L);

        ISemiFluidFuelManager.FuelProperty property = manager.getFuelProperty(fluid("ic2biogas"));
        assertEquals(32L, property.energyPerMb);
        assertEquals(16L, property.energyPerTick);
        assertTrue(manager.acceptsFluid(fluid("ic2biogas")));
    }

    @Test
    public void anUnknownFluidIsNoFuel() {
        SemiFluidFuelManager manager = new SemiFluidFuelManager();
        manager.addFluid("ic2creosote", 3L, 8L);

        assertNull(manager.getFuelProperty(fluid("water")));
        assertFalse(manager.acceptsFluid(fluid("water")));
        assertFalse(manager.acceptsFluid(null));
    }

    @Test
    public void aRemovedFuelStopsBurning() {
        SemiFluidFuelManager manager = new SemiFluidFuelManager();
        manager.addFluid("ic2biomass", 8L, 8L);
        manager.removeFluid("ic2biomass");

        assertFalse(manager.acceptsFluid(fluid("ic2biomass")));
    }

    /**
     * The generator burns as many millibuckets per tick as it needs to reach the fuel's output
     * rate; a rich fuel like creosote gives that in less than one, so it burns one.
     */
    @Test
    public void millibucketsPerTickFollowFromTheTwoNumbers() {
        assertEquals(1, millibucketsPerTick(128L, 32L));
        assertEquals(1, millibucketsPerTick(16L, 8L));
        assertEquals(3, millibucketsPerTick(3L, 8L));
    }

    private static int millibucketsPerTick(long energyPerMb, long energyPerTick) {
        return energyPerMb >= energyPerTick ? 1 : (int) Math.ceil((double) energyPerTick / energyPerMb);
    }

    private static Fluid fluid(String name) {
        Fluid known = FluidRegistry.getFluid(name);
        if (known == null) {
            known = new Fluid(name);
            FluidRegistry.registerFluid(known);
        }
        return known;
    }
}
