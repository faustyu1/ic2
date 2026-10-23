package ic2.api.recipe;

import net.minecraft.item.ItemStack;

public interface ICannerBottleRecipeManager extends IMachineRecipeManager<ICannerBottleRecipeManager.Input, ItemStack, ICannerBottleRecipeManager.RawInput> {
    boolean addRecipe(IRecipeInput container, IRecipeInput fill, ItemStack output, boolean replace);

    @Deprecated
    void addRecipe(IRecipeInput container, IRecipeInput fill, ItemStack output);

    @Deprecated
    RecipeOutput getOutputFor(ItemStack container, ItemStack fill, boolean adjustInput, boolean acceptTest);

    class Input {
        public final IRecipeInput container;
        public final IRecipeInput fill;

        public Input(IRecipeInput container, IRecipeInput fill) {
            this.container = container;
            this.fill = fill;
        }

        public boolean matches(ItemStack container, ItemStack fill) {
            return this.container.matches(container) && this.fill.matches(fill);
        }
    }

    class RawInput {
        public final ItemStack container;
        public final ItemStack fill;

        public RawInput(ItemStack container, ItemStack fill) {
            this.container = container;
            this.fill = fill;
        }
    }
}
