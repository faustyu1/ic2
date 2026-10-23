package ic2.core;

import ic2.api.recipe.ISemiFluidFuelManager;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

/**
 * Fabric port of ic2.core.SemiFluidFuelManager: which fluids the semifluid generator burns and
 * how much energy each millibucket is worth.
 */
public class SemiFluidFuelManager implements ISemiFluidFuelManager {
    private final Map<String, FuelProperty> fuelProperties = new HashMap<>();

    @Override
    public void addFluid(String fluidName, long energyPerMb, long energyPerTick) {
        if (fuelProperties.containsKey(fluidName)) {
            throw new RuntimeException("The fluid " + fluidName + " does already have a fuel property assigned.");
        }
        fuelProperties.put(fluidName, new FuelProperty(energyPerMb, energyPerTick));
    }

    @Override
    public void removeFluid(String fluidName) {
        fuelProperties.remove(fluidName);
    }

    @Override
    public FuelProperty getFuelProperty(Fluid fluid) {
        return fluid == null ? null : fuelProperties.get(fluid.getName());
    }

    @Override
    public boolean acceptsFluid(Fluid fluid) {
        return fluid != null && fuelProperties.containsKey(fluid.getName());
    }

    @Override
    public Set<Fluid> getAcceptedFluids() {
        Set<Fluid> ret = new HashSet<>();
        for (String fluidName : fuelProperties.keySet()) {
            Fluid fluid = FluidRegistry.getFluid(fluidName);
            if (fluid != null) {
                ret.add(fluid);
            }
        }
        return ret;
    }

    @Override
    public Map<String, FuelProperty> getFuelProperties() {
        return Collections.unmodifiableMap(fuelProperties);
    }
}
