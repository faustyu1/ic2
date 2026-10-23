package ic2.core.block.invslot;

import ic2.api.recipe.IMachineRecipeManager;
import ic2.api.recipe.MachineRecipeResult;
import ic2.core.block.IInventorySlotHolder;
import net.minecraft.item.ItemStack;

/**
 * Fabric port of ic2.core.block.invslot.InvSlotProcessable - an input slot group backed by a
 * recipe manager, so what it accepts is exactly what the machine has a recipe for.
 */
public class InvSlotProcessable<RI, RO, I> extends InvSlotConsumable {
    protected final IMachineRecipeManager<RI, RO, I> recipeManager;

    public InvSlotProcessable(IInventorySlotHolder<?> base, String name, int count,
                              IMachineRecipeManager<RI, RO, I> recipeManager) {
        super(base, name, count);
        this.recipeManager = recipeManager;
    }

    @Override
    public boolean accepts(ItemStack stack) {
        return process(stack) != null;
    }

    /** @return the recipe result for the given input, or null when there is none */
    @SuppressWarnings("unchecked")
    public MachineRecipeResult<RI, RO, I> process(ItemStack input) {
        if (input == null || input.isEmpty() || recipeManager == null) {
            return null;
        }
        return recipeManager.apply((I) input, false);
    }

    /** @return the recipe result for the current content, or null when there is none */
    public MachineRecipeResult<RI, RO, I> process() {
        return process(get());
    }

    /** Removes what the recipe consumes from this slot. */
    public void consume(MachineRecipeResult<RI, RO, I> result) {
        if (result == null) {
            return;
        }
        ItemStack input = get();
        ItemStack adjusted = (ItemStack) result.getAdjustedInput();
        put(0, adjusted == null ? ItemStack.EMPTY : adjusted.copyWithCount(
                Math.max(0, input.getCount() - (input.getCount() - adjusted.getCount()))));
    }
}
