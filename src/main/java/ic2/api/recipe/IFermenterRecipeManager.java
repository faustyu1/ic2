package ic2.api.recipe;

import ic2.core.util.LiquidUtil;
import java.util.Map;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount;
import net.minecraft.fluid.Fluid;

public interface IFermenterRecipeManager extends ILiquidAcceptManager {
    void addRecipe(String inputFluidName, int inputAmount, int heat, String outputFluidName, int outputAmount);

    FermentationProperty getFermentationInformation(Fluid fluid);

    ResourceAmount<FluidVariant> getOutput(Fluid fluid);

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

        /** The output in droplets; the amounts above are millibuckets. */
        public ResourceAmount<FluidVariant> getOutput() {
            Fluid fluid = LiquidUtil.getFluid(output);
            return fluid == null ? null : new ResourceAmount<>(FluidVariant.of(fluid), LiquidUtil.mb(outputAmount));
        }
    }
}
