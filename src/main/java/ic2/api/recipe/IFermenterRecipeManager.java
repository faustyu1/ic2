package ic2.api.recipe;

import java.util.Map;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

public interface IFermenterRecipeManager extends ILiquidAcceptManager {
    void addRecipe(String inputFluidName, int inputAmount, int heat, String outputFluidName, int outputAmount);

    FermentationProperty getFermentationInformation(Fluid fluid);

    FluidStack getOutput(Fluid fluid);

    Map<String, FermentationProperty> getRecipeMap();

    final class FermentationProperty {
        public final int inputAmount;
        public final int heat;
        public final String output;
        public final int outputAmount;

        public FermentationProperty(int inputAmount, int heat, String output, int outputAmount) {
            this.inputAmount = inputAmount;
            this.heat = heat;
            this.output = output;
            this.outputAmount = outputAmount;
        }

        public FluidStack getOutput() {
            Fluid fluid = FluidRegistry.getFluid(output);
            return fluid == null ? null : new FluidStack(fluid, outputAmount);
        }
    }
}
