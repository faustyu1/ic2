package ic2.core.gui.dynamic;

import ic2.core.block.TileEntityInventory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

/**
 * The result slot of a crafting grid inside a machine window, as the industrial workbench's
 * SlotCrafting were. It shows what {@link TileEntityInventory#getCraftingResult} offers; taking it
 * takes the whole stack and lets the block use up the ingredients.
 */
public class SlotCraftResult extends Slot {
    private final TileEntityInventory tile;
    public final String name;

    public SlotCraftResult(TileEntityInventory tile, String name, Inventory results, int index, int x, int y) {
        super(results, index, x, y);
        this.tile = tile;
        this.name = name;
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        return false;
    }

    /** A craft is all or nothing, so the stack never splits. */
    @Override
    public boolean canTakePartial(PlayerEntity player) {
        return false;
    }

    @Override
    public ItemStack takeStack(int amount) {
        return super.takeStack(getStack().getCount());
    }

    @Override
    public void onTakeItem(PlayerEntity player, ItemStack stack) {
        if (!player.getWorld().isClient) {
            tile.onCraftingResultTaken(name, player, stack);
            refresh();
        }
        super.onTakeItem(player, stack);
    }

    /** Shows what the block offers now. */
    public void refresh() {
        ItemStack result = tile.getCraftingResult(name);
        inventory.setStack(getIndex(), result == null ? ItemStack.EMPTY : result.copy());
    }
}
