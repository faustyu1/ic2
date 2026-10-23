package ic2.core.block.invslot;

import ic2.api.item.IKineticRotor;
import ic2.core.block.IInventorySlotHolder;
import net.minecraft.item.ItemStack;

/** Fabric port of InvSlotConsumableKineticRotor - takes a rotor that fits the gearbox. */
public class InvSlotConsumableKineticRotor extends InvSlotConsumable {
    private final IKineticRotor.GearboxType type;

    public InvSlotConsumableKineticRotor(IInventorySlotHolder<?> base, String name, Access access, int count,
                                         InvSide preferredSide, IKineticRotor.GearboxType type) {
        super(base, name, access, count, preferredSide);
        this.type = type;
    }

    @Override
    public boolean accepts(ItemStack stack) {
        return stack.getItem() instanceof IKineticRotor rotor && rotor.isAcceptedType(stack, type);
    }
}
