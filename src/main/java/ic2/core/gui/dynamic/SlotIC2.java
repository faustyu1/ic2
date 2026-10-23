package ic2.core.gui.dynamic;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

/**
 * A slot of a guidef-driven machine GUI.
 *
 * <p>Vanilla slots accept anything by default; IC2 machines decide per
 * {@link ic2.core.block.invslot.InvSlot} group, which the backing inventory exposes through
 * {@link Inventory#isValid}, so insertion and stack limits are answered by the machine itself.
 */
public class SlotIC2 extends Slot {
    /** A slot the player only looks at, as SlotInvSlotReadOnly showed a trade-o-mat's offer to its customers. */
    private boolean readOnly;

    public SlotIC2(Inventory inventory, int index, int x, int y) {
        super(inventory, index, x, y);
    }

    /** Makes this slot one nothing can be put in or taken out of ({@code readonly="true"} in the guidef). */
    public SlotIC2 readOnly() {
        this.readOnly = true;
        return this;
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        return !readOnly && inventory.isValid(getIndex(), stack);
    }

    @Override
    public boolean canTakeItems(net.minecraft.entity.player.PlayerEntity player) {
        return !readOnly && super.canTakeItems(player);
    }

    @Override
    public int getMaxItemCount(ItemStack stack) {
        return Math.min(super.getMaxItemCount(stack), inventory.getMaxCountPerStack());
    }
}
