package ic2.core.block.invslot;

import ic2.api.item.IElectricItem;
import ic2.core.block.IInventorySlotHolder;
import net.minecraft.item.ItemStack;

/**
 * Fabric port of ic2.core.block.invslot.InvSlotDischarge - the slot a machine drains batteries in.
 *
 * <p>The tier check is what the original used to keep a high tier battery out of a low tier
 * machine; the actual energy transfer lives in the machine's tick.
 */
public class InvSlotDischarge extends InvSlotConsumable {
    private final int tier;

    public InvSlotDischarge(IInventorySlotHolder<?> base, String name, int tier) {
        this(base, name, tier, 1);
    }

    public InvSlotDischarge(IInventorySlotHolder<?> base, String name, int tier, int count) {
        super(base, name, Access.I, count, InvSide.BOTTOM);
        this.tier = tier;
    }

    @Override
    public boolean accepts(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof IElectricItem electric)) {
            return false;
        }
        return electric.canProvideEnergy(stack) && electric.getTier(stack) <= tier;
    }

    public int getTier() {
        return tier;
    }
}
