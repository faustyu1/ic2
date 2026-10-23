package ic2.core.recipe;

import ic2.api.recipe.IRecipeInput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.item.ItemStack;

/** Fabric port of ic2.core.recipe.RecipeInputCombined - matches if any of the parts matches. */
public class RecipeInputCombined implements IRecipeInput {
    private final List<IRecipeInput> parts;
    private final int amount;

    public RecipeInputCombined(IRecipeInput... parts) {
        this(Arrays.asList(parts));
    }

    public RecipeInputCombined(Iterable<IRecipeInput> parts) {
        List<IRecipeInput> list = new ArrayList<>();
        for (IRecipeInput part : parts) {
            list.add(part);
        }
        if (list.isEmpty()) {
            throw new IllegalArgumentException("no inputs");
        }
        this.parts = list;
        this.amount = list.get(0).getAmount();
    }

    @Override
    public boolean matches(ItemStack subject) {
        for (IRecipeInput part : parts) {
            if (part.matches(subject)) return true;
        }
        return false;
    }

    @Override
    public int getAmount() {
        return amount;
    }

    @Override
    public List<ItemStack> getInputs() {
        List<ItemStack> ret = new ArrayList<>();
        for (IRecipeInput part : parts) {
            ret.addAll(part.getInputs());
        }
        return ret;
    }

    @Override
    public String toString() {
        return "any" + parts;
    }
}
