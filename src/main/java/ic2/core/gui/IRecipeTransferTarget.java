package ic2.core.gui;

import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * A block whose window can take a crafting recipe from a recipe viewer, what the JEI transfer
 * handlers of the industrial workbench and the batch crafter did in 1.12.2.
 */
public interface IRecipeTransferTarget {
    /**
     * Sets the recipe up.
     *
     * @param cells the nine cells of a 3x3 grid, row by row, each with the items that may go there
     *              (empty for an empty cell)
     * @param max   whether to fill as many crafts as there are items for, rather than one
     * @return whether anything changed
     */
    boolean transferRecipe(ServerPlayerEntity player, List<List<ItemStack>> cells, boolean max);

    /**
     * Whether a stack is one of a cell's items. A plain item in the recipe takes any stack of it,
     * a worn tool too, as the recipe's ingredient would; one with data takes only that data.
     */
    static boolean matches(ItemStack stack, List<ItemStack> cell) {
        for (ItemStack wanted : cell) {
            if (stack.isOf(wanted.getItem())
                    && (wanted.getComponentChanges().isEmpty() || ItemStack.areItemsAndComponentsEqual(stack, wanted))) {
                return true;
            }
        }
        return false;
    }

    /** Whether the player has the items for one craft, as the client sees the window; the batch crafter needs none. */
    default boolean needsItems() {
        return true;
    }
}
