package ic2.core.uu;

import ic2.api.recipe.IRecipeInput;
import ic2.api.recipe.MachineRecipeResult;
import ic2.api.recipe.Recipes;
import ic2.core.block.machine.tileentity.TileEntityRecycler;
import ic2.core.init.IC2ItemVariants;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Port of {@code ic2.core.uu.RecyclerResolver}: scrap is worth the cheapest obtainable item the
 * recycler turns into scrap, eight of it (one in eight gives scrap), plus the recycler's energy
 * for those eight runs expressed in UU (55 * 8 / 4000 * 107, about 11.8).
 */
class RecyclerResolver implements ILateRecipeResolver {
    private static final double transformCost = 55.0 * TileEntityRecycler.recycleChance() / 4000.0 * 107.0;

    @Override
    public List<RecipeTransformation> getTransformations(Iterable<LeanItemStack> obtainableStacks) {
        Item scrap = IC2ItemVariants.get("crafting", "scrap");
        if (Recipes.recycler == null || scrap == null) {
            return Collections.emptyList();
        }

        List<LeanItemStack> input = new ArrayList<>();

        for (LeanItemStack obtainableStack : obtainableStacks) {
            ItemStack stack = obtainableStack.toMcStack();
            if (stack.isEmpty()) {
                continue;
            }

            MachineRecipeResult<IRecipeInput, Collection<ItemStack>, ItemStack> result = Recipes.recycler.apply(stack, false);
            if (result != null && !result.getOutput().isEmpty()) {
                input.add(new LeanItemStack(stack, TileEntityRecycler.recycleChance()));
            }
        }

        return Collections.singletonList(
                new RecipeTransformation(transformCost, Collections.singletonList(input), new LeanItemStack(scrap, 1)));
    }
}
