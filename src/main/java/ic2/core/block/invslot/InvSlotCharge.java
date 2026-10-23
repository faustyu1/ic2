package ic2.core.block.invslot;

import ic2.api.item.ElectricItem;
import ic2.core.block.IInventorySlotHolder;
import net.minecraft.item.ItemStack;

/**
 * The slot that charges an electric item from the block's buffer, e.g. the battery slot of a
 * generator. Port of InvSlotCharge; the energy component managed the slot in 1.12.2, here the
 * block hands the free energy over each tick.
 */
public class InvSlotCharge extends InvSlot {
    private int tier;

    public InvSlotCharge(IInventorySlotHolder<?> base, int tier) {
        super(base, "charge", Access.IO, 1, InvSide.TOP);
        this.tier = tier;
    }

    @Override
    public boolean accepts(ItemStack stack) {
        return ElectricItem.manager != null
                && ElectricItem.manager.charge(stack, Double.POSITIVE_INFINITY, tier, true, true) > 0.0;
    }

    /**
     * Moves energy into the item.
     *
     * @return how much of the offered amount the item took
     */
    public double charge(double amount) {
        if (amount <= 0.0) {
            return 0.0;
        }

        ItemStack stack = get(0);
        if (stack.isEmpty() || ElectricItem.manager == null) {
            return 0.0;
        }
        return ElectricItem.manager.charge(stack, amount, tier, false, false);
    }

    public int getTier() {
        return tier;
    }

    public void setTier(int tier) {
        this.tier = tier;
    }
}
