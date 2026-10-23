package ic2.api.recipe;

import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;

public interface IRecipeInputFactory {
    IRecipeInput forStack(ItemStack stack);

    IRecipeInput forStack(ItemStack stack, int amount);

    IRecipeInput forExactStack(ItemStack stack);

    IRecipeInput forExactStack(ItemStack stack, int amount);

    IRecipeInput forOreDict(String oreDict);

    IRecipeInput forOreDict(String oreDict, int amount);

    IRecipeInput forOreDict(String oreDict, int amount, int meta);

    IRecipeInput forFluidContainer(Fluid fluid);

    IRecipeInput forFluidContainer(Fluid fluid, int amount);

    IRecipeInput forAny(IRecipeInput... inputs);

    IRecipeInput forAny(Iterable<IRecipeInput> inputs);

    IRecipeInput forIngredient(Ingredient ingredient);

    Ingredient getIngredient(IRecipeInput input);
}
