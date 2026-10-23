package ic2.integration.rei;

import ic2.core.gui.DynamicContainer;
import ic2.core.gui.GuiIC2;
import ic2.core.gui.IRecipeTransferTarget;
import ic2.core.gui.dynamic.SlotCraftResult;
import ic2.core.gui.dynamic.SlotHologram;
import ic2.core.network.RecipeTransferPayload;
import java.util.ArrayList;
import java.util.List;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandler;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import me.shedaniel.rei.plugin.common.displays.crafting.DefaultCraftingDisplay;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

/**
 * The "+" of a crafting recipe in the industrial workbench and the batch crafter, port of
 * TransferHandlerIndustrialWorkbench and TransferHandlerBatchCrafter.
 *
 * <p>The recipe goes to the server as what each cell of a 3x3 grid may take; the block does the
 * rest. For the workbench the button is only active when the window holds one craft's worth,
 * counting the storage, the grid and the tool spots as the server does, and the player's
 * inventory.
 */
final class Ic2TransferHandler implements TransferHandler {
    /**
     * Ahead of REI's own handlers: they would otherwise take an IC2 window for the player's
     * inventory and fill the player's 2x2 crafting grid instead.
     */
    @Override
    public double getPriority() {
        return 100;
    }

    @Override
    public Result handle(Context context) {
        if (!(context.getContainerScreen() instanceof GuiIC2)
                || !(context.getMenu() instanceof DynamicContainer container)
                || !(container.getTile() instanceof IRecipeTransferTarget target)
                || !(context.getDisplay() instanceof DefaultCraftingDisplay<?> display)) {
            return Result.createNotApplicable();
        }

        List<EntryIngredient> grid = display.getOrganisedInputEntries(3, 3);
        List<List<ItemStack>> cells = new ArrayList<>(grid.size());
        for (EntryIngredient ingredient : grid) {
            List<ItemStack> cell = new ArrayList<>();
            for (EntryStack<?> stack : ingredient) {
                if (stack.getType() == VanillaEntryTypes.ITEM) {
                    ItemStack item = stack.castValue();
                    if (!item.isEmpty() && cell.size() < RecipeTransferPayload.MAX_ALTERNATIVES) {
                        cell.add(item.copyWithCount(1));
                    }
                }
            }
            cells.add(cell);
        }
        if (cells.stream().allMatch(List::isEmpty)) {
            return Result.createNotApplicable();
        }

        if (target.needsItems()) {
            List<EntryIngredient> missing = missing(container, grid, cells);
            if (!missing.isEmpty()) {
                return Result.createFailed(Text.translatable("error.rei.not.enough.materials")).tooltipMissing(missing);
            }
        }

        if (context.isActuallyCrafting()) {
            ClientPlayNetworking.send(new RecipeTransferPayload(container.syncId, cells, context.isStackedCrafting()));
        }
        return Result.createSuccessful().blocksFurtherHandling();
    }

    /** The cells the window and the player's inventory hold nothing for, taking each item once. */
    private static List<EntryIngredient> missing(DynamicContainer container, List<EntryIngredient> grid, List<List<ItemStack>> cells) {
        List<ItemStack> available = new ArrayList<>();
        for (Slot slot : container.slots) {
            if (!(slot instanceof SlotCraftResult) && !(slot instanceof SlotHologram) && slot.hasStack()) {
                available.add(slot.getStack().copy());
            }
        }

        List<EntryIngredient> missing = new ArrayList<>();
        for (int i = 0; i < cells.size(); i++) {
            List<ItemStack> cell = cells.get(i);
            if (cell.isEmpty()) {
                continue;
            }
            ItemStack found = null;
            for (ItemStack stack : available) {
                if (!stack.isEmpty() && IRecipeTransferTarget.matches(stack, cell)) {
                    found = stack;
                    break;
                }
            }
            if (found == null) {
                missing.add(grid.get(i));
            } else {
                found.decrement(1);
            }
        }
        return missing;
    }
}
