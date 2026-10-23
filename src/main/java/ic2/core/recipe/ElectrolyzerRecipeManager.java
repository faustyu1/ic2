package ic2.core.recipe;

import ic2.api.recipe.IElectrolyzerRecipeManager;
import ic2.core.util.LiquidUtil;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.fluid.Fluid;

/** Fabric port of ic2.core.recipe.ElectrolyzerRecipeManager: one recipe per input fluid, 200 ticks unless given. */
public class ElectrolyzerRecipeManager implements IElectrolyzerRecipeManager {
    private final Map<String, ElectrolyzerRecipe> fluidMap = new HashMap<>();

    @Override
    public void addRecipe(String input, int inputAmount, int EUaTick, ElectrolyzerOutput... outputs) {
        addRecipe(input, inputAmount, EUaTick, 200, outputs);
    }

    @Override
    public void addRecipe(String input, int inputAmount, int EUaTick, int ticksNeeded, ElectrolyzerOutput... outputs) {
        if (fluidMap.containsKey(input)) {
            throw new RuntimeException("The fluid " + input + " already has an output assigned.");
        }
        fluidMap.put(input, new ElectrolyzerRecipe(inputAmount, EUaTick, ticksNeeded, outputs));
    }

    @Override
    public ElectrolyzerRecipe getElectrolysisInformation(Fluid fluid) {
        return fluid == null ? null : fluidMap.get(LiquidUtil.nameOf(fluid));
    }

    @Override
    public ElectrolyzerOutput[] getOutput(Fluid input) {
        ElectrolyzerRecipe recipe = getElectrolysisInformation(input);
        return recipe == null ? null : recipe.outputs;
    }

    @Override
    public boolean acceptsFluid(Fluid fluid) {
        return fluid != null && fluidMap.containsKey(LiquidUtil.nameOf(fluid));
    }

    @Override
    public Set<Fluid> getAcceptedFluids() {
        Set<Fluid> ret = new HashSet<>();
        for (String fluidName : fluidMap.keySet()) {
            Fluid fluid = LiquidUtil.getFluid(fluidName);
            if (fluid != null) {
                ret.add(fluid);
            }
        }
        return ret;
    }

    @Override
    public Map<String, ElectrolyzerRecipe> getRecipeMap() {
        return Collections.unmodifiableMap(fluidMap);
    }
}
