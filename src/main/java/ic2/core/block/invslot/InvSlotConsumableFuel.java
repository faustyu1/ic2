package ic2.core.block.invslot;

import ic2.core.block.IInventorySlotHolder;
import ic2.core.util.FuelValues;
import net.minecraft.item.ItemStack;

/** Slot taking anything a furnace burns, port of InvSlotConsumableFuel. */
public class InvSlotConsumableFuel extends InvSlotConsumable {
    public final boolean allowLava;

    public InvSlotConsumableFuel(IInventorySlotHolder<?> base, String name, int count, boolean allowLava) {
        super(base, name, Access.I, count, InvSide.SIDE);
        this.allowLava = allowLava;
    }

    @Override
    public boolean accepts(ItemStack stack) {
        return FuelValues.get(stack, allowLava) > 0;
    }

    /**
     * Takes one item out of the slot.
     *
     * @return its burn value in ticks, or 0 when there was nothing to burn
     */
    public int consumeFuel() {
        ItemStack fuel = consume(1);
        return fuel == null || fuel.isEmpty() ? 0 : FuelValues.get(fuel, allowLava);
    }
}
