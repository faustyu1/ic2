package ic2.api.recipe;

import ic2.core.util.LiquidUtil;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount;
import net.minecraft.fluid.Fluid;
import net.minecraft.util.math.Direction;
import org.apache.commons.lang3.tuple.Pair;

public interface IElectrolyzerRecipeManager extends ILiquidAcceptManager {
    void addRecipe(String inputFluidName, int inputAmount, int energyPerTick, ElectrolyzerOutput... outputs);

    void addRecipe(String inputFluidName, int inputAmount, int energyPerTick, int ticksNeeded, ElectrolyzerOutput... outputs);

    ElectrolyzerRecipe getElectrolysisInformation(Fluid fluid);

    ElectrolyzerOutput[] getOutput(Fluid fluid);

    Map<String, ElectrolyzerRecipe> getRecipeMap();

    final class ElectrolyzerOutput {
        public final String fluidName;
        public final int fluidAmount;
        public final Direction tankDirection;

        public ElectrolyzerOutput(String fluidName, int fluidAmount, Direction tankDirection) {
            this.fluidName = fluidName;
            this.fluidAmount = fluidAmount;
            this.tankDirection = tankDirection;
        }

        /** The output in droplets; {@link #fluidAmount} is millibuckets. */
        public ResourceAmount<FluidVariant> getOutput() {
            Fluid fluid = LiquidUtil.getFluid(fluidName);
            return fluid == null ? null : new ResourceAmount<>(FluidVariant.of(fluid), LiquidUtil.mb(fluidAmount));
        }

        public Pair<ResourceAmount<FluidVariant>, Direction> getFullOutput() {
            return Pair.of(getOutput(), tankDirection);
        }
    }

    final class ElectrolyzerRecipe {
        public final int inputAmount;
        public final int EUaTick;
        public final int ticksNeeded;
        public final ElectrolyzerOutput[] outputs;

        public ElectrolyzerRecipe(int inputAmount, int EUaTick, int ticksNeeded, ElectrolyzerOutput... outputs) {
            this.inputAmount = inputAmount;
            this.EUaTick = EUaTick;
            this.ticksNeeded = ticksNeeded;
            this.outputs = validateOutputs(outputs);
        }

        private ElectrolyzerOutput[] validateOutputs(ElectrolyzerOutput[] outputs) {
            if (outputs.length >= 1 && outputs.length <= 5) {
                Set<Direction> directions = new HashSet<>(outputs.length * 2, 0.5F);
                for (ElectrolyzerOutput output : outputs) {
                    if (!directions.add(output.tankDirection)) {
                        throw new RuntimeException("Duplicate direction in Electrolzer outputs (" + output.tankDirection + ')');
                    }
                }
                return outputs;
            } else {
                throw new RuntimeException("Cannot have " + outputs.length + " outputs of an Electrolzer recipe, must be between 1 and 5");
            }
        }
    }
}
