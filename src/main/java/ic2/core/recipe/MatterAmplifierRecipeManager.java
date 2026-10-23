package ic2.core.recipe;

import ic2.api.recipe.IMachineRecipeManager;
import ic2.api.recipe.IRecipeInput;
import ic2.api.recipe.MachineRecipe;
import ic2.api.recipe.MachineRecipeResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

/**
 * Fabric port of MatterAmplifierRecipeManager: what the matter fabricators take as amplifier and
 * how much amplification each item gives (scrap 5000, a scrap box 45 000).
 */
public class MatterAmplifierRecipeManager implements IMachineRecipeManager<IRecipeInput, Integer, ItemStack> {
    private final List<MachineRecipe<IRecipeInput, Integer>> recipes = new ArrayList<>();

    @Override
    public boolean addRecipe(IRecipeInput input, Integer output, NbtCompound metadata, boolean replace) {
        for (int i = 0; i < recipes.size(); i++) {
            for (ItemStack stack : input.getInputs()) {
                if (recipes.get(i).getInput().matches(stack)) {
                    if (!replace) {
                        return false;
                    }
                    recipes.set(i, new MachineRecipe<>(input, output, metadata));
                    return true;
                }
            }
        }
        recipes.add(new MachineRecipe<>(input, output, metadata));
        return true;
    }

    @Override
    public MachineRecipeResult<IRecipeInput, Integer, ItemStack> apply(ItemStack input, boolean adjustInput) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        for (MachineRecipe<IRecipeInput, Integer> recipe : recipes) {
            IRecipeInput recipeInput = recipe.getInput();
            if (recipeInput.matches(input) && input.getCount() >= recipeInput.getAmount()) {
                ItemStack rest = input.copy();
                rest.decrement(recipeInput.getAmount());
                return new MachineRecipeResult<>(recipe, rest);
            }
        }
        return null;
    }

    @Override
    public Iterable<? extends MachineRecipe<IRecipeInput, Integer>> getRecipes() {
        return Collections.unmodifiableList(recipes);
    }

    @Override
    public boolean isIterable() {
        return true;
    }
}
