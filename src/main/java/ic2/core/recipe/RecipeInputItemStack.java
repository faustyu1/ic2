package ic2.core.recipe;

import ic2.api.recipe.IRecipeInput;
import java.util.Collections;
import java.util.List;
import net.minecraft.item.ItemStack;

/**
 * Fabric port of ic2.core.recipe.RecipeInputItemStack.
 *
 * <p>The 1.12.2 original compared item plus metadata, with {@code OreDictionary.WILDCARD_VALUE}
 * meaning "any metadata". Metadata is gone since flattening, so matching is by item identity only;
 * the strict variant additionally requires the data components to be equal, which is what the old
 * NBT-sensitive {@code RecipeInputItemStack(stack, amount, true)} did.
 */
public class RecipeInputItemStack implements IRecipeInput {
    public final ItemStack input;
    public final int amount;
    private final boolean matchComponents;

    public RecipeInputItemStack(ItemStack input) {
        this(input, input.getCount());
    }

    public RecipeInputItemStack(ItemStack input, int amount) {
        this(input, amount, false);
    }

    public RecipeInputItemStack(ItemStack input, int amount, boolean matchComponents) {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("null or empty input");
        }
        this.input = input;
        this.amount = amount;
        this.matchComponents = matchComponents;
    }

    @Override
    public boolean matches(ItemStack subject) {
        if (subject == null || subject.isEmpty() || subject.getItem() != input.getItem()) {
            return false;
        }
        return !matchComponents || ItemStack.areItemsAndComponentsEqual(subject, input);
    }

    @Override
    public int getAmount() {
        return amount;
    }

    @Override
    public List<ItemStack> getInputs() {
        ItemStack ret = input.copy();
        ret.setCount(amount);
        return Collections.singletonList(ret);
    }

    @Override
    public String toString() {
        return "stack(" + input.getItem() + " x" + amount + ")";
    }
}
