package ic2.api.recipe;

import java.util.Collection;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

public interface IBasicMachineRecipeManager extends IMachineRecipeManager<IRecipeInput, Collection<ItemStack>, ItemStack> {
    boolean addRecipe(IRecipeInput input, NbtCompound metadata, boolean replace, ItemStack... outputs);

    @Deprecated
    RecipeOutput getOutputFor(ItemStack input, boolean adjustInput);
}
