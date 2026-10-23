package ic2.core.gui.dynamic;

import ic2.core.block.TileEntityInventory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

/**
 * A ghost slot, port of SlotHologramSlot: clicking an item into it stores a copy and leaves the
 * player's stack alone. Left click with an item adds the whole stack (up to the limit), right
 * click adds one; with an empty hand a left click clears it and a right click halves it.
 *
 * <p>The stacks live in an array of the block (see {@link TileEntityInventory#getHologramSlots}),
 * so vanilla syncs them to the client like any slot without them being part of the inventory.
 */
public class SlotHologram extends Slot {
    private final TileEntityInventory tile;
    private final String name;
    private final ItemStack[] stacks;
    private final int stackSizeLimit;

    public SlotHologram(TileEntityInventory tile, String name, ItemStack[] stacks, int index, int x, int y, int stackSizeLimit) {
        super(new ArrayInventory(stacks), index, x, y);
        this.tile = tile;
        this.name = name;
        this.stacks = stacks;
        this.stackSizeLimit = stackSizeLimit;
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canTakeItems(PlayerEntity player) {
        return false;
    }

    @Override
    public ItemStack takeStack(int amount) {
        return ItemStack.EMPTY;
    }

    @Override
    public int getMaxItemCount() {
        return stackSizeLimit;
    }

    /** Handles a click the way SlotHologramSlot.slotClick did; every other kind of click does nothing. */
    public void click(int button, SlotActionType actionType, ItemStack cursor, PlayerEntity player) {
        if (actionType != SlotActionType.PICKUP || button != 0 && button != 1) {
            return;
        }

        int index = getIndex();
        ItemStack slotStack = stacks[index] == null ? ItemStack.EMPTY : stacks[index];
        if (!cursor.isEmpty()) {
            int curSize = slotStack.getCount();
            int extraSize = button == 0 ? cursor.getCount() : 1;
            int limit = Math.min(cursor.getMaxCount(), stackSizeLimit);
            if (curSize + extraSize > limit) {
                extraSize = Math.max(0, limit - curSize);
            }

            if (curSize == 0) {
                stacks[index] = cursor.copyWithCount(extraSize);
            } else if (ItemStack.areItemsAndComponentsEqual(cursor, slotStack)) {
                stacks[index] = slotStack.copyWithCount(curSize + extraSize);
            } else {
                stacks[index] = cursor.copyWithCount(Math.min(cursor.getCount(), limit));
            }
        } else if (!slotStack.isEmpty()) {
            int newSize = button == 0 ? 0 : slotStack.getCount() / 2;
            stacks[index] = newSize <= 0 ? ItemStack.EMPTY : slotStack.copyWithCount(newSize);
        }

        if (stacks[index].isEmpty()) {
            stacks[index] = ItemStack.EMPTY;
        }
        if (!player.getWorld().isClient) {
            tile.onHologramChanged(name, index);
            tile.markDirty();
        }
    }

    /** The array as an inventory, which is all a vanilla slot needs to read and sync it. */
    private record ArrayInventory(ItemStack[] stacks) implements Inventory {
        @Override
        public int size() {
            return stacks.length;
        }

        @Override
        public boolean isEmpty() {
            for (ItemStack stack : stacks) {
                if (stack != null && !stack.isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public ItemStack getStack(int slot) {
            return stacks[slot] == null ? ItemStack.EMPTY : stacks[slot];
        }

        @Override
        public ItemStack removeStack(int slot, int amount) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeStack(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public void setStack(int slot, ItemStack stack) {
            stacks[slot] = stack;
        }

        @Override
        public void markDirty() {
        }

        @Override
        public boolean canPlayerUse(PlayerEntity player) {
            return true;
        }

        @Override
        public void clear() {
        }
    }
}
