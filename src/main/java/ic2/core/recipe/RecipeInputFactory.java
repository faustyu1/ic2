package ic2.core.recipe;

import ic2.api.recipe.IRecipeInput;
import ic2.api.recipe.IRecipeInputFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;

/**
 * Fabric port of ic2.core.recipe.RecipeInputFactory, the implementation behind
 * {@link ic2.api.recipe.Recipes#inputFactory}.
 */
public class RecipeInputFactory implements IRecipeInputFactory {
    @Override
    public IRecipeInput forStack(ItemStack stack) {
        return new RecipeInputItemStack(stack);
    }

    @Override
    public IRecipeInput forStack(ItemStack stack, int amount) {
        return new RecipeInputItemStack(stack, amount);
    }

    @Override
    public IRecipeInput forExactStack(ItemStack stack) {
        return new RecipeInputItemStack(stack, stack.getCount(), true);
    }

    @Override
    public IRecipeInput forExactStack(ItemStack stack, int amount) {
        return new RecipeInputItemStack(stack, amount, true);
    }

    @Override
    public IRecipeInput forOreDict(String oreDict) {
        return forOreDict(oreDict, 1);
    }

    @Override
    public IRecipeInput forOreDict(String oreDict, int amount) {
        return new RecipeInputTag(oreDict, OreDictionaryCompat.getTag(oreDict), amount);
    }

    @Override
    public IRecipeInput forOreDict(String oreDict, int amount, int meta) {
        // metadata disappeared with the 1.13 flattening; the tag already covers the variants
        return forOreDict(oreDict, amount);
    }

    @Override
    public IRecipeInput forFluidContainer(Fluid fluid) {
        return forFluidContainer(fluid, 1);
    }

    @Override
    public IRecipeInput forFluidContainer(Fluid fluid, int amount) {
        return new RecipeInputFluidContainer(fluid, amount);
    }

    @Override
    public IRecipeInput forAny(IRecipeInput... inputs) {
        return new RecipeInputCombined(inputs);
    }

    @Override
    public IRecipeInput forAny(Iterable<IRecipeInput> inputs) {
        return new RecipeInputCombined(inputs);
    }

    @Override
    public IRecipeInput forIngredient(Ingredient ingredient) {
        List<ItemStack> stacks = new ArrayList<>(Arrays.asList(ingredient.getMatchingStacks()));
        if (stacks.isEmpty()) {
            throw new IllegalArgumentException("empty ingredient");
        }
        if (stacks.size() == 1) {
            return new RecipeInputItemStack(stacks.get(0));
        }
        List<IRecipeInput> parts = new ArrayList<>(stacks.size());
        for (ItemStack stack : stacks) {
            parts.add(new RecipeInputItemStack(stack));
        }
        return new RecipeInputCombined(parts);
    }

    @Override
    public Ingredient getIngredient(IRecipeInput input) {
        if (input instanceof RecipeInputTag tagInput) {
            return Ingredient.fromTag(tagInput.tag);
        }
        List<ItemStack> stacks = input.getInputs();
        Item[] items = new Item[stacks.size()];
        for (int i = 0; i < items.length; i++) {
            items[i] = stacks.get(i).getItem();
        }
        return Ingredient.ofItems(items);
    }
}
