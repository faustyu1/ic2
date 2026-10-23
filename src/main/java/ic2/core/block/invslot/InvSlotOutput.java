package ic2.core.block.invslot;

import ic2.core.block.IInventorySlotHolder;
import java.util.Collection;
import java.util.Collections;
import net.minecraft.item.ItemStack;

/**
 * Fabric port of ic2.core.block.invslot.InvSlotOutput - a machine's result slots.
 *
 * <p>Nothing may be inserted from the outside; {@link #add} is what the machine itself uses, and
 * {@link #canAdd} answers whether an operation would fit before it is started.
 */
public class InvSlotOutput extends InvSlot {
    public InvSlotOutput(IInventorySlotHolder<?> base, String name, int count) {
        this(base, name, count, InvSide.BOTTOM);
    }

    public InvSlotOutput(IInventorySlotHolder<?> base, String name, int count, InvSide side) {
        super(base, name, Access.O, count, side);
    }

    @Override
    public boolean accepts(ItemStack stack) {
        return false;
    }

    /** @return the amount that did not fit */
    public int add(Collection<ItemStack> stacks) {
        return add(stacks, false);
    }

    /** @return the amount that did not fit */
    public int add(ItemStack stack) {
        if (stack == null) {
            throw new NullPointerException("null ItemStack");
        }
        return add(Collections.singletonList(stack), false);
    }

    public boolean canAdd(Collection<ItemStack> stacks) {
        return add(stacks, true) == 0;
    }

    public boolean canAdd(ItemStack stack) {
        if (stack == null) {
            throw new NullPointerException("null ItemStack");
        }
        return add(Collections.singletonList(stack), true) == 0;
    }

    private int add(Collection<ItemStack> stacks, boolean simulate) {
        if (stacks == null || stacks.isEmpty()) {
            return 0;
        }

        ItemStack[] backup = simulate ? backup() : null;
        int remaining = 0;

        for (ItemStack stack : stacks) {
            int amount = stack.getCount();
            if (amount <= 0) {
                continue;
            }

            // first pass merges into matching stacks, second pass fills empty slots
            for (int pass = 0; pass < 2 && amount > 0; pass++) {
                for (int i = 0; i < size() && amount > 0; i++) {
                    ItemStack existing = get(i);
                    int space = getStackSizeLimit();
                    if (!existing.isEmpty()) {
                        space = Math.min(space, existing.getMaxCount()) - existing.getCount();
                    }
                    if (space <= 0) {
                        continue;
                    }

                    if (pass == 0 && !existing.isEmpty() && ItemStack.areItemsAndComponentsEqual(stack, existing)) {
                        int added = Math.min(space, amount);
                        ItemStack merged = existing.copy();
                        merged.increment(added);
                        put(i, merged);
                        amount -= added;
                    } else if (pass == 1 && existing.isEmpty()) {
                        int added = Math.min(space, amount);
                        put(i, stack.copyWithCount(added));
                        amount -= added;
                    }
                }
            }

            remaining += amount;
        }

        if (simulate) {
            restore(backup);
        }

        return remaining;
    }
}
