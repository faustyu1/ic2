package ic2.core;

import ic2.api.recipe.ILiquidAcceptManager;
import ic2.api.recipe.ILiquidHeatExchangerManager;
import ic2.api.recipe.Recipes;
import ic2.core.util.LiquidUtil;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.fluid.Fluid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric port of ic2.core.LiquidHeatExchangerManager: the table of "this fluid turns into that
 * one and carries so much heat per millibucket".
 *
 * <p>Two of these exist, one per direction. Each checks the temperatures of the two fluids so a
 * recipe registered the wrong way round is caught where it is added rather than where it runs.
 * The original threw for an invalid recipe unless the config said otherwise; here it logs and
 * skips, since a single bad recipe must not stop the mod from loading.
 */
public class LiquidHeatExchangerManager implements ILiquidHeatExchangerManager {
    private static final Logger LOG = LoggerFactory.getLogger("IC2-HeatExchanger");

    private final boolean heatup;
    private final SingleDirectionManager singleDirectionManager = new SingleDirectionManager();
    private final Map<String, HeatExchangeProperty> map = new HashMap<>();

    public LiquidHeatExchangerManager(boolean heatup) {
        this.heatup = heatup;
    }

    @Override
    public boolean acceptsFluid(Fluid fluid) {
        return fluid != null && map.containsKey(LiquidUtil.nameOf(fluid));
    }

    @Override
    public Set<Fluid> getAcceptedFluids() {
        Set<Fluid> fluids = new HashSet<>();
        for (String fluidName : map.keySet()) {
            Fluid fluid = LiquidUtil.getFluid(fluidName);
            if (fluid != null) {
                fluids.add(fluid);
            }
        }
        return fluids;
    }

    @Override
    public void addFluid(String fluidName, String fluidOutput, int huPerMB) {
        if (map.containsKey(fluidName)) {
            displayError("The fluid " + fluidName + " does already have a HeatExchangerProperty assigned.");
            return;
        }
        if (huPerMB == 0) {
            displayError("A fluid was registered for the HeatExchanging recipe without an energy value. Ignoring...");
            return;
        }

        Fluid input = LiquidUtil.getFluid(fluidName);
        Fluid output = LiquidUtil.getFluid(fluidOutput);
        if (input == null || output == null) {
            displayError("Could not get both fluids for " + fluidName + " and " + fluidOutput + ".");
            return;
        }

        if (heatup) {
            if (FluidVariantAttributes.getTemperature(FluidVariant.of(input)) >= FluidVariantAttributes.getTemperature(FluidVariant.of(output))) {
                displayError("Cannot heat up a warm liquid into a cold one. " + fluidName + " -> " + fluidOutput);
                return;
            }
        } else if (FluidVariantAttributes.getTemperature(FluidVariant.of(input)) <= FluidVariantAttributes.getTemperature(FluidVariant.of(output))) {
            displayError("Cannot cool down a cold liquid into a warm one. " + fluidName + " -> " + fluidOutput);
            return;
        }

        map.put(fluidName, new HeatExchangeProperty(output, Math.abs(huPerMB)));
    }

    @Override
    public HeatExchangeProperty getHeatExchangeProperty(Fluid fluid) {
        return fluid == null ? null : map.get(LiquidUtil.nameOf(fluid));
    }

    @Override
    public Map<String, HeatExchangeProperty> getHeatExchangeProperties() {
        return map;
    }

    private void displayError(String msg) {
        LOG.warn("[IC2] {}", msg);
    }

    @Override
    public ILiquidAcceptManager getSingleDirectionLiquidManager() {
        return singleDirectionManager;
    }

    /** The manager for the other direction; null until both have been created. */
    public ILiquidHeatExchangerManager getOpposite() {
        return heatup ? Recipes.liquidCooldownManager : Recipes.liquidHeatupManager;
    }

    /**
     * The fluids that only go one way, i.e. whose output cannot be turned straight back. A slot
     * bound to this will not take a coolant that the machine would immediately convert back.
     */
    public class SingleDirectionManager implements ILiquidAcceptManager {
        @Override
        public boolean acceptsFluid(Fluid fluid) {
            if (!LiquidHeatExchangerManager.this.acceptsFluid(fluid)) {
                return false;
            }

            ILiquidHeatExchangerManager opposite = getOpposite();
            if (opposite == null) {
                return true;
            }

            HeatExchangeProperty property = getHeatExchangeProperty(fluid);
            return !opposite.acceptsFluid(property.outputFluid);
        }

        @Override
        public Set<Fluid> getAcceptedFluids() {
            Set<Fluid> ret = new HashSet<>();
            ILiquidHeatExchangerManager opposite = getOpposite();

            for (Map.Entry<String, HeatExchangeProperty> entry : map.entrySet()) {
                if (opposite == null || !opposite.acceptsFluid(entry.getValue().outputFluid)) {
                    Fluid fluid = LiquidUtil.getFluid(entry.getKey());
                    if (fluid != null) {
                        ret.add(fluid);
                    }
                }
            }

            return ret;
        }
    }
}
