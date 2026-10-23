package ic2.core.recipe;

import ic2.api.recipe.IFluidHeatManager;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

/**
 * Fabric port of {@code ic2.core.recipe.FluidHeatManager}: what the liquid fuel firebox burns, by
 * fluid name - how many mB a second and how many HU a tick.
 */
public class FluidHeatManager implements IFluidHeatManager {
    private final Map<String, BurnProperty> burnProperties = new LinkedHashMap<>();

    @Override
    public void addFluid(String fluidName, int amount, int heat) {
        if (burnProperties.containsKey(fluidName)) {
            throw new RuntimeException("The fluid " + fluidName + " already has a burn property assigned.");
        }
        burnProperties.put(fluidName, new BurnProperty(amount, heat));
    }

    @Override
    public BurnProperty getBurnProperty(Fluid fluid) {
        return fluid == null ? null : burnProperties.get(fluid.getName());
    }

    @Override
    public Map<String, BurnProperty> getBurnProperties() {
        return Collections.unmodifiableMap(burnProperties);
    }

    @Override
    public boolean acceptsFluid(Fluid fluid) {
        return fluid != null && burnProperties.containsKey(fluid.getName());
    }

    @Override
    public Set<Fluid> getAcceptedFluids() {
        Set<Fluid> ret = new HashSet<>();
        for (String name : burnProperties.keySet()) {
            Fluid fluid = FluidRegistry.getFluid(name);
            if (fluid != null) {
                ret.add(fluid);
            }
        }
        return ret;
    }
}
