package ic2.core.uu;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;

/** Port of {@code ic2.core.uu.RecipeUtil}: conversions from recipe parts to graph stacks. */
final class RecipeUtil {
    private RecipeUtil() {}

    /** One ingredient that any of the stacks can fill, each with its own amount. */
    static List<List<LeanItemStack>> convertIngredients(Collection<ItemStack> x) {
        return Collections.singletonList(convertOutputs(x));
    }

    static List<LeanItemStack> convertOutputs(Collection<ItemStack> x) {
        List<LeanItemStack> ret = new ArrayList<>(x.size());

        for (ItemStack stack : x) {
            if (!stack.isEmpty()) {
                ret.add(new LeanItemStack(stack));
            }
        }

        return ret;
    }

    /**
     * The ingredients of a crafting recipe, one of each matching stack per slot.
     *
     * <p>Blank slots of shaped recipes are skipped. 1.12.2 passed them on as ingredients nothing
     * can fill, which left every shaped recipe with a gap in its pattern without a value.
     */
    static List<List<LeanItemStack>> convertCraftingIngredients(List<Ingredient> list) {
        List<List<LeanItemStack>> ret = new ArrayList<>(list.size());

        for (Ingredient ingredient : list) {
            if (ingredient.isEmpty()) {
                continue;
            }

            ItemStack[] arr = ingredient.getMatchingStacks();
            List<LeanItemStack> toAdd = new ArrayList<>(arr.length);

            for (ItemStack stack : arr) {
                if (!stack.isEmpty()) {
                    toAdd.add(new LeanItemStack(stack, Math.max(1, stack.getCount())));
                }
            }

            ret.add(toAdd);
        }

        return ret;
    }
}
