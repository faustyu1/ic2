package ic2.core.block.invslot;

import ic2.core.block.IInventorySlotHolder;
import net.minecraft.item.ItemStack;

/**
 * Fabric port of ic2.core.block.invslot.InvSlotConsumable - an input slot group that only takes
 * what the machine can actually process.
 *
 * <p>The 1.12.2 container-item handling (buckets leaving an empty bucket behind) becomes the
 * vanilla recipe remainder in 1.21, so {@link #consume} asks the item for its remainder instead of
 * Forge's {@code hasContainerItem}.
 */
public abstract class InvSlotConsumable extends InvSlot {
    public InvSlotConsumable(IInventorySlotHolder<?> base, String name, int count) {
        super(base, name, Access.I, count, InvSide.TOP);
    }

    public InvSlotConsumable(IInventorySlotHolder<?> base, String name, Access access, int count, InvSide preferredSide) {
        super(base, name, access, count, preferredSide);
    }

    @Override
    public abstract boolean accepts(ItemStack stack);

    @Override
    public boolean canOutput() {
        // something that is no longer accepted may leave again, e.g. an emptied cell
        return super.canOutput() || (getAccess() != Access.NONE && !isEmpty() && !accepts(get()));
    }

    /**
     * Wears the items in this group down, using up an item that reaches its full damage and going
     * on with the next one of the same kind - a rotor or a heat vent wearing out.
     *
     * @return the damage that was applied
     */
    public int damage(int amount, boolean simulate) {
        int applied = 0;
        ItemStack target = ItemStack.EMPTY;

        for (int i = 0; i < size() && amount > 0; i++) {
            ItemStack stack = get(i);
            if (stack.isEmpty() || !accepts(stack) || ic2.core.item.DamageHandler.getMaxDamage(stack) <= 0
                    || !target.isEmpty() && !ItemStack.areItemsAndComponentsEqual(stack, target)) {
                continue;
            }
            if (target.isEmpty()) {
                target = stack.copy();
            }

            stack = stack.copy();
            int maxDamage = ic2.core.item.DamageHandler.getMaxDamage(stack);
            while (amount > 0 && !stack.isEmpty()) {
                int current = Math.min(amount, maxDamage - ic2.core.item.DamageHandler.getDamage(stack));
                ic2.core.item.DamageHandler.setDamage(stack, ic2.core.item.DamageHandler.getDamage(stack) + current, false);
                applied += current;
                amount -= current;
                if (ic2.core.item.DamageHandler.getDamage(stack) >= maxDamage) {
                    stack.decrement(1);
                    if (!stack.isEmpty()) {
                        ic2.core.item.DamageHandler.setDamage(stack, 0, false);
                    }
                }
            }

            if (!simulate) {
                put(i, stack);
            }
        }
        return applied;
    }

    public ItemStack consume(int amount) {
        return consume(amount, false, false);
    }

    /**
     * Takes up to {@code amount} items out of this slot group.
     *
     * @param simulate          when true, nothing is actually removed
     * @param consumeContainers when true, an item with a recipe remainder is consumed whole
     * @return what was taken, or {@link ItemStack#EMPTY} if nothing matched
     */
    public ItemStack consume(int amount, boolean simulate, boolean consumeContainers) {
        ItemStack ret = ItemStack.EMPTY;

        for (int i = 0; i < size() && amount > 0; i++) {
            ItemStack stack = get(i);
            if (stack.getCount() < 1 || !accepts(stack)) {
                continue;
            }
            if (!ret.isEmpty() && !ItemStack.areItemsAndComponentsEqual(stack, ret)) {
                continue;
            }

            boolean hasRemainder = stack.getItem().hasRecipeRemainder();
            if (stack.getCount() != 1 && !consumeContainers && hasRemainder) {
                continue;
            }

            int currentAmount = Math.min(amount, stack.getCount());
            amount -= currentAmount;

            if (!simulate) {
                if (stack.getCount() == currentAmount) {
                    if (!consumeContainers && hasRemainder) {
                        put(i, stack.getItem().getRecipeRemainder().getDefaultStack());
                    } else {
                        clear(i);
                    }
                } else {
                    ItemStack left = stack.copy();
                    left.decrement(currentAmount);
                    put(i, left);
                }
            }

            if (ret.isEmpty()) {
                ret = stack.copyWithCount(currentAmount);
            } else {
                ret.increment(currentAmount);
            }
        }

        return ret;
    }
}
