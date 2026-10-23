package ic2.api.recipe;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.util.math.Direction;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
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

        public FluidStack getOutput() {
            Fluid fluid = FluidRegistry.getFluid(fluidName);
            return fluid == null ? null : new FluidStack(fluid, fluidAmount);
        }

        public Pair<FluidStack, Direction> getFullOutput() {
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
