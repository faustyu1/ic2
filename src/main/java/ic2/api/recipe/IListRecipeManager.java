package ic2.api.recipe;

import java.util.List;
import net.minecraft.item.ItemStack;

public interface IListRecipeManager extends Iterable<IRecipeInput> {
    void add(IRecipeInput input);

    boolean contains(ItemStack stack);

    boolean isEmpty();

    List<IRecipeInput> getInputs();
}
