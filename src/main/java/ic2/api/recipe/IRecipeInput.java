package ic2.api.recipe;

import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;

public interface IRecipeInput {
    boolean matches(ItemStack stack);

    int getAmount();

    List<ItemStack> getInputs();

    default Ingredient getIngredient() {
        return Recipes.inputFactory.getIngredient(this);
    }
}
