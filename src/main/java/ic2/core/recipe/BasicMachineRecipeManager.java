package ic2.core.recipe;

import ic2.api.recipe.IBasicMachineRecipeManager;
import ic2.api.recipe.IRecipeInput;
import ic2.api.recipe.MachineRecipe;
import ic2.api.recipe.MachineRecipeResult;
import ic2.api.recipe.RecipeOutput;
import java.util.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

/**
 * Fabric port of BasicMachineRecipeManager. It holds two sets of recipes: those other mods add
 * through the API, and the datapack recipes {@link ic2.core.recipe.data.MachineRecipeSync} puts in
 * on every recipe reload, which took the place of the 1.12.2 machine configs.
 */
public class BasicMachineRecipeManager implements IBasicMachineRecipeManager {

    private final List<MachineRecipe<IRecipeInput, Collection<ItemStack>>> recipes = new ArrayList<>();
    /** The recipes of the loaded datapacks, replaced as a whole on every reload. */
    private List<MachineRecipe<IRecipeInput, Collection<ItemStack>>> dataRecipes = List.of();

    /** Replaces the datapack recipes; those added through the API stay and are tried first. */
    public void setDataRecipes(List<MachineRecipe<IRecipeInput, Collection<ItemStack>>> recipes) {
        dataRecipes = List.copyOf(recipes);
    }

    @Override
    public boolean addRecipe(IRecipeInput input, NbtCompound metadata, boolean replace, ItemStack... outputs) {
        // Check if recipe already exists
        for (MachineRecipe<IRecipeInput, Collection<ItemStack>> r : recipes) {
            if (r.getInput().equals(input)) {
                if (!replace) return false;
                recipes.remove(r);
                break;
            }
        }
        List<ItemStack> outList = Arrays.asList(outputs);
        recipes.add(new MachineRecipe<>(input, outList, metadata));
        return true;
    }

    // IMachineRecipeManager parent method - also required
    public boolean addRecipe(IRecipeInput input, Collection<ItemStack> output, NbtCompound metadata, boolean replace) {
        return addRecipe(input, metadata, replace, output.toArray(new ItemStack[0]));
    }

    @Override
    public MachineRecipeResult<IRecipeInput, Collection<ItemStack>, ItemStack> apply(ItemStack input, boolean adjustInput) {
        if (input == null || input.isEmpty()) return null;
        MachineRecipe<IRecipeInput, Collection<ItemStack>> recipe = find(recipes, input);
        if (recipe == null) recipe = find(dataRecipes, input);
        if (recipe == null) return null;

        ItemStack adjusted = input.copy();
        if (adjustInput) {
            adjusted.decrement(recipe.getInput().getAmount());
        }
        //noinspection unchecked
        return OutputUnifier.unifyResult((MachineRecipeResult<IRecipeInput, Collection<ItemStack>, ItemStack>) recipe.getResult(adjusted));
    }

    private static MachineRecipe<IRecipeInput, Collection<ItemStack>> find(List<MachineRecipe<IRecipeInput, Collection<ItemStack>>> list, ItemStack input) {
        for (MachineRecipe<IRecipeInput, Collection<ItemStack>> recipe : list) {
            if (recipe.getInput().matches(input)) {
                return recipe;
            }
        }
        return null;
    }

    @Override
    public List<MachineRecipe<IRecipeInput, Collection<ItemStack>>> getRecipes() {
        if (dataRecipes.isEmpty()) {
            return recipes;
        }
        List<MachineRecipe<IRecipeInput, Collection<ItemStack>>> ret = new ArrayList<>(recipes);
        ret.addAll(dataRecipes);
        return ret;
    }

    @Override
    public boolean isIterable() { return true; }

    @Override @Deprecated
    public RecipeOutput getOutputFor(ItemStack input, boolean adjustInput) {
        MachineRecipeResult<IRecipeInput, Collection<ItemStack>, ItemStack> result = apply(input, adjustInput);
        if (result == null) return null;
        return new RecipeOutput(result.getRecipe().getMetaData(), new ArrayList<>(result.getOutput()));
    }
}
