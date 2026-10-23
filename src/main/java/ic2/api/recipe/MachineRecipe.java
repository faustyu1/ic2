package ic2.api.recipe;

import net.minecraft.nbt.NbtCompound;

public class MachineRecipe<I, O> {
    private final I input;
    private final O output;
    private final NbtCompound meta;

    public MachineRecipe(I input, O output) {
        this(input, output, null);
    }

    public MachineRecipe(I input, O output, NbtCompound meta) {
        this.input = input;
        this.output = output;
        this.meta = meta;
    }

    public I getInput() {
        return input;
    }

    public O getOutput() {
        return output;
    }

    public NbtCompound getMetaData() {
        return meta;
    }

    public <AI> MachineRecipeResult<I, O, AI> getResult(AI adjustedInput) {
        return new MachineRecipeResult<>(this, adjustedInput);
    }
}
