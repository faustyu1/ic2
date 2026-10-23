package ic2.core.recipe;

import ic2.api.recipe.IListRecipeManager;
import ic2.api.recipe.IRecipeInput;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.item.ItemStack;

public class BasicListRecipeManager implements IListRecipeManager {
    private final List<IRecipeInput> inputs = new ArrayList<>();

    @Override public void add(IRecipeInput input) { inputs.add(input); }
    @Override public boolean contains(ItemStack stack) {
        for (IRecipeInput in : inputs) if (in.matches(stack)) return true;
        return false;
    }
    @Override public boolean isEmpty() { return inputs.isEmpty(); }
    @Override public List<IRecipeInput> getInputs() { return inputs; }
    @Override public Iterator<IRecipeInput> iterator() { return inputs.iterator(); }
}
