package ic2.core.recipe;

import ic2.api.recipe.IRecipeInput;
import ic2.core.init.IC2ItemVariants;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;

/**
 * Fabric port of ic2.core.recipe.RecipeInputFluidContainer.
 *
 * <p>The original accepted any Forge fluid container holding the requested fluid. Fabric has no
 * such capability system in this port yet, so the match is against the IC2 fluid cell registered
 * for that fluid, which is what every {@code Fluid:<name>} entry in the config files refers to.
 */
public class RecipeInputFluidContainer implements IRecipeInput {
    public final Fluid fluid;
    public final int amount;
    private final Item cell;

    public RecipeInputFluidContainer(Fluid fluid, int amount) {
        this.fluid = fluid;
        this.amount = amount;
        this.cell = IC2ItemVariants.get("fluid_cell", fluid == null ? null : fluid.getName());
    }

    @Override
    public boolean matches(ItemStack subject) {
        return cell != null && subject != null && !subject.isEmpty() && subject.getItem() == cell;
    }

    @Override
    public int getAmount() {
        return amount;
    }

    @Override
    public List<ItemStack> getInputs() {
        List<ItemStack> ret = new ArrayList<>(1);
        if (cell != null) {
            ret.add(new ItemStack(cell, amount));
        }
        return ret;
    }

    @Override
    public String toString() {
        return "fluid(" + (fluid == null ? "null" : fluid.getName()) + " x" + amount + ")";
    }
}
