package ic2.api.recipe;

import net.minecraft.nbt.NbtCompound;

public interface IMachineRecipeManager<RI, RO, I> {
    boolean addRecipe(RI input, RO output, NbtCompound metadata, boolean replace);

    MachineRecipeResult<RI, RO, I> apply(I input, boolean adjustInput);

    Iterable<? extends MachineRecipe<RI, RO>> getRecipes();

    boolean isIterable();
}
