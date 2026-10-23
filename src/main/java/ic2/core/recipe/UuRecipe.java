package ic2.core.recipe;

import ic2.api.recipe.IRecipeInput;
import java.util.List;
import java.util.Map;
import net.minecraft.item.ItemStack;

/**
 * A pattern recipe from {@code uu_recipes.ini} - the UU-matter shapes the assembly bench (mass
 * fabricator pattern crafting) turns into an item.
 *
 * <p>The original built these as {@code AdvRecipe}/{@code TileEntityAssemblyBench.UuRecipe}
 * instances registered into Forge's recipe registry. Here they stay a plain list owned by
 * {@link ic2.core.block.machine.tileentity.TileEntityAssemblyBench}, because the assembly bench
 * consumes them directly rather than through the crafting system.
 */
public final class UuRecipe {
    /** Pattern rows, padded to equal width; a space means an empty slot. */
    public final List<String> pattern;
    /** Ingredient per pattern symbol. */
    public final Map<Character, IRecipeInput> inputs;
    public final ItemStack output;
    /** True when UU-matter is the only ingredient, which is the common case. */
    public final boolean uuOnly;

    public UuRecipe(List<String> pattern, Map<Character, IRecipeInput> inputs, ItemStack output, boolean uuOnly) {
        this.pattern = List.copyOf(pattern);
        this.inputs = Map.copyOf(inputs);
        this.output = output;
        this.uuOnly = uuOnly;
    }

    /** Number of pattern cells that are not empty, i.e. how much UU-matter the shape costs. */
    public int getSize() {
        int ret = 0;
        for (String row : pattern) {
            for (int i = 0; i < row.length(); i++) {
                if (row.charAt(i) != ' ') {
                    ret++;
                }
            }
        }
        return ret;
    }

    @Override
    public String toString() {
        return "UuRecipe" + pattern + " -> " + output;
    }
}
