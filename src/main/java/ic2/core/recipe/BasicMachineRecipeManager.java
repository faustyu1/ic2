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
 * Minimal Fabric port of BasicMachineRecipeManager.
 * Original handled NBT metadata and OreDict. This version keeps a simple map.
 */
public class BasicMachineRecipeManager implements IBasicMachineRecipeManager {

    private final List<MachineRecipe<IRecipeInput, Collection<ItemStack>>> recipes = new ArrayList<>();

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
        for (MachineRecipe<IRecipeInput, Collection<ItemStack>> recipe : recipes) {
            if (recipe.getInput().matches(input)) {
                ItemStack adjusted = input.copy();
                if (adjustInput) {
                    int amount = recipe.getInput().getAmount();
                    adjusted.decrement(amount);
                }
                //noinspection unchecked
                return OutputUnifier.unifyResult((MachineRecipeResult<IRecipeInput, Collection<ItemStack>, ItemStack>) recipe.getResult(adjusted));
            }
        }
        return null;
    }

    @Override
    public Iterable<? extends MachineRecipe<IRecipeInput, Collection<ItemStack>>> getRecipes() {
        return recipes;
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
