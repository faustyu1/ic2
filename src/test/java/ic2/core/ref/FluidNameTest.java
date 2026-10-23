package ic2.core.ref;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import org.junit.Test;

/**
 * The fluids and the number that names them on the network.
 *
 * <p>A tank tells the client which fluid it holds by the id {@link FluidRegistry#getFluidId}
 * derives from the fluid's name, so two fluids sharing an id would show up as each other.
 */
public class FluidNameTest {

    @Test
    public void everyFluidIsRegisteredUnderItsIc2Name() {
        FluidName.registerAll();

        for (FluidName name : FluidName.values()) {
            Fluid fluid = name.getInstance();
            assertNotNull(name.name(), fluid);
            assertEquals("ic2" + name.name(), fluid.getName());
            assertNotNull(name.name(), FluidRegistry.getRegisteredFluids().get(fluid.getName()));
        }
    }

    @Test
    public void theVanillaFluidsAreRegisteredToo() {
        FluidName.registerAll();

        assertNotNull(FluidRegistry.getRegisteredFluids().get("water"));
        assertNotNull(FluidRegistry.getRegisteredFluids().get("lava"));
        assertEquals(0x3F76E4, FluidRegistry.getRegisteredFluids().get("water").getColor());
    }

    @Test
    public void fluidIdsDoNotCollide() {
        FluidName.registerAll();

        Map<Integer, String> byId = new HashMap<>();
        TreeSet<String> collisions = new TreeSet<>();

        for (Fluid fluid : FluidRegistry.getRegisteredFluids().values()) {
            String other = byId.put(FluidRegistry.getFluidId(fluid), fluid.getName());
            if (other != null) {
                collisions.add(other + " / " + fluid.getName());
            }
        }

        assertTrue("fluids sharing a network id: " + collisions, collisions.isEmpty());
    }

    @Test
    public void aFluidIdResolvesBackToItsFluid() {
        FluidName.registerAll();

        Fluid coolant = FluidName.coolant.getInstance();
        assertEquals(coolant, FluidRegistry.byId(FluidRegistry.getFluidId(coolant)));
    }

    @Test
    public void everyFluidHasItsOwnTexture() {
        FluidName.registerAll();

        assertEquals("ic2:block/fluid/coolant_still", FluidName.coolant.getInstance().getStillTexture().toString());
        assertEquals("ic2:block/fluid/molten_metal", FluidName.molten_iron.getTextureLocation(false).toString());
    }
}
