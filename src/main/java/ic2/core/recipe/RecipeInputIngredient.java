package ic2.core.recipe;

import ic2.api.recipe.IRecipeInput;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;

/**
 * A recipe input backed by a vanilla {@link Ingredient}, so an item or tag of a datapack recipe
 * matches exactly as the recipe file describes it, with the amount the machine takes at once.
 */
public class RecipeInputIngredient implements IRecipeInput {
    public final Ingredient ingredient;
    public final int amount;

    public RecipeInputIngredient(Ingredient ingredient, int amount) {
        this.ingredient = ingredient;
        this.amount = amount;
    }

    @Override
    public boolean matches(ItemStack subject) {
        return subject != null && !subject.isEmpty() && ingredient.test(subject);
    }

    @Override
    public int getAmount() {
        return amount;
    }

    @Override
    public List<ItemStack> getInputs() {
        List<ItemStack> ret = new ArrayList<>();
        for (ItemStack stack : ingredient.getMatchingStacks()) {
            ret.add(stack.copyWithCount(amount));
        }
        return ret;
    }

    @Override
    public Ingredient getIngredient() {
        return ingredient;
    }

    @Override
    public String toString() {
        return "ingredient(" + getInputs() + " x" + amount + ")";
    }
}
