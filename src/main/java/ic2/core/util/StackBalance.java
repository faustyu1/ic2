package ic2.core.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiPredicate;
import net.minecraft.item.ItemStack;

/**
 * Port of StackUtil.balanceStacks: spreads equal items evenly over the slots of a grid that hold
 * them, as the industrial workbench and the batch crafter did after each craft, so every recipe
 * slot runs out at the same time.
 */
public final class StackBalance {
    private StackBalance() {}

    /** The grid being balanced. */
    public interface Grid {
        int size();

        ItemStack get(int index);

        void set(int index, ItemStack stack);

        default int getMaxCountPerStack() {
            return 64;
        }
    }

    /**
     * @param leftOvers what did not fit back
     * @param changed   the slots the balancing touched
     */
    public record Result(List<ItemStack> leftOvers, Set<Integer> changed) {}

    /** Balances the grid among the slots already holding an item. */
    public static Result balance(Grid grid, Collection<ItemStack> additional) {
        return balance(grid, (stack, slot) -> !grid.get(slot).isEmpty(), allSlots(grid), additional);
    }

    /**
     * Balances the grid among the slots {@code canInsert} allows, adding the additional stacks.
     *
     * @param canInsert whether the stack may go into the slot
     * @param slots     the slots to work on
     */
    public static Result balance(Grid grid, BiPredicate<ItemStack, Integer> canInsert, Collection<Integer> slots,
                                 Collection<ItemStack> additional) {
        List<ItemStack> additionalStacks = new LinkedList<>();
        for (ItemStack stack : additional) {
            additionalStacks.add(stack.copy());
        }
        Set<Integer> availableSlots = new TreeSet<>(slots);
        List<ItemStack> leftOvers = new ArrayList<>();

        for (int i = 0; i < grid.size(); i++) {
            if (!availableSlots.contains(i)) {
                continue;
            }
            ItemStack stack = grid.get(i).copy();
            if (stack.isEmpty()) {
                continue;
            }

            int amount = 0;
            for (Iterator<ItemStack> it = additionalStacks.iterator(); it.hasNext(); ) {
                ItemStack current = it.next();
                if (ItemStack.areItemsAndComponentsEqual(current, stack)) {
                    it.remove();
                    amount += current.getCount();
                }
            }

            amount = distribute(grid, stack, availableSlots, canInsert, amount);
            while (amount > 0) {
                int size = Math.min(stack.getMaxCount(), amount);
                amount -= size;
                leftOvers.add(stack.copyWithCount(size));
            }
        }

        for (ItemStack stack : additionalStacks) {
            int amount = distribute(grid, stack, availableSlots, canInsert, stack.getCount());
            if (amount > 0) {
                leftOvers.add(stack.copyWithCount(amount));
            }
        }

        Set<Integer> changed = new TreeSet<>(slots);
        changed.removeAll(availableSlots);
        return new Result(leftOvers, changed);
    }

    private static List<Integer> allSlots(Grid grid) {
        List<Integer> ret = new ArrayList<>(grid.size());
        for (int i = 0; i < grid.size(); i++) {
            ret.add(i);
        }
        return ret;
    }

    /** Fills the matching slots as evenly as possible; returns what did not fit. */
    private static int distribute(Grid grid, ItemStack stack, Set<Integer> availableSlots,
                                  BiPredicate<ItemStack, Integer> canInsert, int amount) {
        List<Integer> workingSet = new ArrayList<>();
        for (Iterator<Integer> it = availableSlots.iterator(); it.hasNext(); ) {
            int slot = it.next();
            ItemStack current = grid.get(slot);
            if ((current.isEmpty() || ItemStack.areItemsAndComponentsEqual(stack, current)) && canInsert.test(stack, slot)) {
                amount += current.getCount();
                workingSet.add(slot);
                it.remove();
            }
        }

        int maxStackSize = Math.min(stack.getMaxCount(), grid.getMaxCountPerStack());
        int slotsLeft = workingSet.size();
        Iterator<Integer> it = workingSet.iterator();
        while (it.hasNext() && amount > 0) {
            int slot = it.next();
            int itemsToPut = amount / slotsLeft;
            if (amount % slotsLeft > 0) {
                itemsToPut++;
            }
            itemsToPut = Math.min(itemsToPut, maxStackSize);
            grid.set(slot, stack.copyWithCount(itemsToPut));
            amount -= itemsToPut;
            slotsLeft--;
            it.remove();
        }

        for (int slot : workingSet) {
            grid.set(slot, ItemStack.EMPTY);
        }
        return amount;
    }
}
