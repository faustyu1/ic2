package ic2.core.block;

import ic2.core.block.invslot.InvSlot;

/**
 * Fabric port of ic2.core.block.IInventorySlotHolder - implemented by whatever owns
 * {@link InvSlot} instances, normally a {@link TileEntityInventory}.
 */
public interface IInventorySlotHolder<T> {
    /** Called from the InvSlot constructor to register the slot with its owner. */
    void addInventorySlot(InvSlot slot);

    /** The owning object, used for log messages. */
    T getParent();

    /** Marks the owner dirty after a slot changed. */
    void onInventoryChanged();
}
