package ic2.api.recipe;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount;
import net.minecraft.item.ItemStack;

public interface ICannerEnrichRecipeManager extends IMachineRecipeManager<ICannerEnrichRecipeManager.Input, ResourceAmount<FluidVariant>, ICannerEnrichRecipeManager.RawInput> {
    @Deprecated
    void addRecipe(ResourceAmount<FluidVariant> fluid, IRecipeInput additive, ResourceAmount<FluidVariant> output);

    @Deprecated
    RecipeOutput getOutputFor(ResourceAmount<FluidVariant> fluid, ItemStack additive, boolean adjustInput, boolean acceptTest);

    class Input {
        public final ResourceAmount<FluidVariant> fluid;
        public final IRecipeInput additive;

        public Input(ResourceAmount<FluidVariant> fluid, IRecipeInput additive) {
            this.fluid = fluid;
            this.additive = additive;
        }

        public boolean matches(ResourceAmount<FluidVariant> fluid, ItemStack additive) {
            return fluid != null && this.fluid.resource().equals(fluid.resource()) && this.additive.matches(additive);
        }
    }

    class RawInput {
        public final ResourceAmount<FluidVariant> fluid;
        public final ItemStack additive;

        public RawInput(ResourceAmount<FluidVariant> fluid, ItemStack additive) {
            this.fluid = fluid;
            this.additive = additive;
        }
    }
}
