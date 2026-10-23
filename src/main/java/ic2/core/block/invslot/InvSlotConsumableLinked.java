package ic2.core.block.invslot;

import ic2.core.block.IInventorySlotHolder;
import net.minecraft.item.ItemStack;

/**
 * Fabric port of ic2.core.block.invslot.InvSlotConsumableLinked - an input that takes only what
 * another slot shows, such as the payment a trade-o-mat asks for in its demand slot.
 */
public class InvSlotConsumableLinked extends InvSlotConsumable {
    public final InvSlot linkedSlot;

    public InvSlotConsumableLinked(IInventorySlotHolder<?> base, String name, int count, InvSlot linkedSlot) {
        super(base, name, count);
        this.linkedSlot = linkedSlot;
    }

    @Override
    public boolean accepts(ItemStack stack) {
        ItemStack required = linkedSlot.get();
        return !required.isEmpty() && ItemStack.areItemsAndComponentsEqual(required, stack);
    }

    /**
     * Takes as many items as the linked slot shows, all or nothing.
     *
     * @return what was (or would be) taken, or null when there is not enough
     */
    public ItemStack consumeLinked(boolean simulate) {
        ItemStack required = linkedSlot.get();
        if (required.isEmpty()) {
            return null;
        }
        int reqAmount = required.getCount();
        ItemStack available = consume(reqAmount, true, true);
        return !available.isEmpty() && available.getCount() == reqAmount ? consume(reqAmount, simulate, true) : null;
    }
}
