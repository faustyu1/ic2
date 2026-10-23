package ic2.core.gui.dynamic;

import com.mojang.datafixers.util.Pair;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.invslot.InvSlotCharge;
import ic2.core.block.invslot.InvSlotDischarge;
import ic2.core.block.invslot.InvSlotUpgrade;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

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
    /** The outline drawn in the empty slot, as the armour slots show theirs. */
    private @Nullable Identifier icon;

    public SlotIC2(Inventory inventory, int index, int x, int y) {
        super(inventory, index, x, y);
    }

    /** Makes this slot one nothing can be put in or taken out of ({@code readonly="true"} in the guidef). */
    public SlotIC2 readOnly() {
        this.readOnly = true;
        return this;
    }

    /** Gives the slot the outline of what goes in it, for the slot groups that have one. */
    public SlotIC2 withIconFor(InvSlot invSlot) {
        String name = invSlot instanceof InvSlotCharge ? "charge"
                : invSlot instanceof InvSlotDischarge ? "discharge"
                : invSlot instanceof InvSlotUpgrade ? "upgrade"
                : null;
        icon = name == null ? null : Identifier.of("ic2", "item/slot/" + name);
        return this;
    }

    @Override
    public @Nullable Pair<Identifier, Identifier> getBackgroundSprite() {
        return icon == null ? super.getBackgroundSprite() : Pair.of(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, icon);
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        return !readOnly && inventory.isValid(getIndex(), stack);
    }

    @Override
    public boolean canTakeItems(net.minecraft.entity.player.PlayerEntity player) {
        return !readOnly && super.canTakeItems(player);
    }

    /** The limit of the slot's group, e.g. one item in a reactor slot. */
    @Override
    public int getMaxItemCount() {
        return inventory instanceof ic2.core.block.TileEntityInventory tile
                ? tile.getMaxCountAt(getIndex()) : inventory.getMaxCountPerStack();
    }

    @Override
    public int getMaxItemCount(ItemStack stack) {
        return Math.min(super.getMaxItemCount(stack), getMaxItemCount());
    }
}
