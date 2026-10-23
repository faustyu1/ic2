package ic2.api.recipe;

public class MachineRecipeResult<RI, RO, I> {
    private final MachineRecipe<RI, RO> recipe;
    private final I adjustedInput;

    public MachineRecipeResult(MachineRecipe<RI, RO> recipe, I adjustedInput) {
        this.recipe = recipe;
        this.adjustedInput = adjustedInput;
    }

    public MachineRecipe<RI, RO> getRecipe() {
        return recipe;
    }

    public RO getOutput() {
        return recipe.getOutput();
    }

    public I getAdjustedInput() {
        return adjustedInput;
    }
}
