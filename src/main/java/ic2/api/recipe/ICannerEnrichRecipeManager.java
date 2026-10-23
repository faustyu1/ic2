package ic2.api.recipe;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

public interface ICannerEnrichRecipeManager extends IMachineRecipeManager<ICannerEnrichRecipeManager.Input, FluidStack, ICannerEnrichRecipeManager.RawInput> {
    @Deprecated
    void addRecipe(FluidStack fluid, IRecipeInput additive, FluidStack output);

    @Deprecated
    RecipeOutput getOutputFor(FluidStack fluid, ItemStack additive, boolean adjustInput, boolean acceptTest);

    class Input {
        public final FluidStack fluid;
        public final IRecipeInput additive;

        public Input(FluidStack fluid, IRecipeInput additive) {
            this.fluid = fluid;
            this.additive = additive;
        }

        public boolean matches(FluidStack fluid, ItemStack additive) {
            return this.fluid.isFluidEqual(fluid) && this.additive.matches(additive);
        }
    }

    class RawInput {
        public final FluidStack fluid;
        public final ItemStack additive;

        public RawInput(FluidStack fluid, ItemStack additive) {
            this.fluid = fluid;
            this.additive = additive;
        }
    }
}
