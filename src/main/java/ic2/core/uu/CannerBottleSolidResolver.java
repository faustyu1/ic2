package ic2.core.uu;

import ic2.api.recipe.ICannerBottleRecipeManager;
import ic2.api.recipe.MachineRecipe;
import ic2.api.recipe.Recipes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.item.ItemStack;

/**
 * Port of {@code ic2.core.uu.CannerBottleSolidResolver}: the solid canning machine's recipes
 * (container plus filling), free of charge. Nothing is read while the port has no bottling
 * recipe manager.
 */
class CannerBottleSolidResolver implements IRecipeResolver {
    private static final double transformCost = 0.0;

    @Override
    public List<RecipeTransformation> getTransformations() {
        List<RecipeTransformation> ret = new ArrayList<>();
        if (Recipes.cannerBottle == null || !Recipes.cannerBottle.isIterable()) {
            return ret;
        }

        for (MachineRecipe<ICannerBottleRecipeManager.Input, ItemStack> recipe : Recipes.cannerBottle.getRecipes()) {
            List<LeanItemStack> container = RecipeUtil.convertOutputs(recipe.getInput().container.getInputs());
            List<LeanItemStack> fill = RecipeUtil.convertOutputs(recipe.getInput().fill.getInputs());
            List<LeanItemStack> output = RecipeUtil.convertOutputs(Collections.singletonList(recipe.getOutput()));
            if (!container.isEmpty() && !fill.isEmpty() && !output.isEmpty()) {
                List<List<LeanItemStack>> inputs = new ArrayList<>(2);
                inputs.add(container);
                inputs.add(fill);
                ret.add(new RecipeTransformation(transformCost, inputs, output));
            }
        }

        return ret;
    }
}
