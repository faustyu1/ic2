package ic2.core.block.comp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import net.minecraft.nbt.NbtCompound;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import org.junit.Test;

/** The named tanks a machine owns: what they accept, what they hold and what they save. */
public class FluidsTest {

    @Test
    public void tanksAreAddressedByName() {
        Fluids fluids = new Fluids();
        fluids.addTank("input", 10000);
        fluids.addTank("output", 5000);

        assertNotNull(fluids.getFluidTank("input"));
        assertEquals(5000, fluids.getFluidTank("output").getCapacity());
        assertNull(fluids.getFluidTank("nothing"));
    }

    @Test
    public void aTankOnlyTakesWhatItAccepts() {
        Fluid coolant = fluid("ic2coolant");
        Fluid lava = fluid("lava");

        Fluids fluids = new Fluids();
        Fluids.InternalFluidTank tank = fluids.addTank("input", 10000, Fluids.fluidPredicate(coolant));

        assertEquals(0, tank.fill(new FluidStack(lava, 1000), true));
        assertEquals(1000, tank.fill(new FluidStack(coolant, 1000), true));
        assertEquals(1000, tank.getFluidAmount());
    }

    @Test
    public void theMachineItselfBypassesThePredicate() {
        Fluids fluids = new Fluids();
        Fluids.InternalFluidTank tank = fluids.addTank("output", 1000, Fluids.fluidPredicate());

        assertEquals(1000, tank.fillInternal(new FluidStack(fluid("ic2steam"), 4000), true));
        assertEquals(1000, tank.getFluidAmount());
        assertEquals(400, tank.drainInternal(400, true).amount);
        assertEquals(600, tank.getFluidAmount());
    }

    @Test
    public void tanksSurviveSaving() {
        Fluid coolant = fluid("ic2coolant");

        Fluids saved = new Fluids();
        saved.addTank("contents", 32000).fillInternal(new FluidStack(coolant, 12345), true);
        NbtCompound nbt = saved.writeToNbt();

        Fluids loaded = new Fluids();
        Fluids.InternalFluidTank tank = loaded.addTank("contents", 32000);
        loaded.readFromNbt(nbt);

        assertNotNull(tank.getFluid());
        assertEquals(12345, tank.getFluidAmount());
        assertTrue(tank.getFluid().getFluid().getName().equals("ic2coolant"));
    }

    @Test
    public void anEmptyTankSavesNothing() {
        Fluids fluids = new Fluids();
        fluids.addTank("contents", 1000);

        assertTrue(fluids.writeToNbt().getCompound("contents").isEmpty());
    }

    /**
     * Registers the fluid outright rather than looking it up: an unknown name sends the registry
     * shim to the Minecraft registry, which is not bootstrapped in a plain unit test.
     */
    private static Fluid fluid(String name) {
        Fluid known = FluidRegistry.getRegisteredFluids().get(name);
        if (known != null) {
            return known;
        }

        Fluid fluid = new Fluid(name);
        FluidRegistry.registerFluid(fluid);
        return fluid;
    }
}
