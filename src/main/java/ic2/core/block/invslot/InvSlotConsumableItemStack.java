package ic2.core.block.invslot;

import ic2.core.block.IInventorySlotHolder;
import java.util.Arrays;
import java.util.List;
import net.minecraft.item.ItemStack;

/** Fabric port of InvSlotConsumableItemStack - accepts a fixed set of items. */
public class InvSlotConsumableItemStack extends InvSlotConsumable {
    private final List<ItemStack> accepted;

    public InvSlotConsumableItemStack(IInventorySlotHolder<?> base, String name, int count, ItemStack... accepted) {
        super(base, name, count);
        this.accepted = Arrays.asList(accepted);
    }

    @Override
    public boolean accepts(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        for (ItemStack candidate : accepted) {
            if (ItemStack.areItemsAndComponentsEqual(candidate, stack)) {
                return true;
            }
        }
        return false;
    }
}
