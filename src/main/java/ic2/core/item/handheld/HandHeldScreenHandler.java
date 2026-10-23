package ic2.core.item.handheld;

import ic2.core.init.IC2ScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;

/**
 * The screen handler every hand-held IC2 container shares; the layout comes from
 * {@link HandHeldKind}, the way the machine screens take theirs from their guidef file.
 */
public class HandHeldScreenHandler extends ScreenHandler {

    /** What the client needs to rebuild the same handler: which hand holds it, and which kind. */
    public record OpenData(Hand hand, HandHeldKind kind) {
        public static final PacketCodec<RegistryByteBuf, OpenData> CODEC = PacketCodec.tuple(
                PacketCodecs.VAR_INT.xmap(i -> Hand.values()[i], Hand::ordinal), OpenData::hand,
                PacketCodecs.VAR_INT.xmap(i -> HandHeldKind.values()[i], HandHeldKind::ordinal), OpenData::kind,
                OpenData::new);
    }

    private final Inventory inventory;
    private final HandHeldKind kind;

    /** Client side: the contents arrive through the usual slot sync, so a scratch inventory does. */
    public HandHeldScreenHandler(int syncId, PlayerInventory playerInventory, OpenData data) {
        this(IC2ScreenHandlers.HAND_HELD, syncId, playerInventory, new SimpleInventory(data.kind().getSize()),
                data.kind());
    }

    public HandHeldScreenHandler(ScreenHandlerType<?> type, int syncId, PlayerInventory playerInventory,
                                 Inventory inventory, HandHeldKind kind) {
        super(type, syncId);
        this.inventory = inventory;
        this.kind = kind;

        addContainerSlots();
        addPlayerSlots(playerInventory);
    }

    public HandHeldKind getKind() {
        return kind;
    }

    private void addContainerSlots() {
        for (int index = 0; index < kind.getSize(); index++) {
            int column = index % kind.getColumns();
            int row = index / kind.getColumns();
            addSlot(new FilteredSlot(inventory, index,
                    kind.getOriginX() + column * 18, kind.getOriginY() + row * 18));
        }
    }

    private void addPlayerSlots(PlayerInventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 142));
        }
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return inventory.canPlayerUse(player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        Slot slot = slots.get(slotIndex);
        if (!slot.hasStack()) return ItemStack.EMPTY;

        ItemStack inSlot = slot.getStack();
        ItemStack original = inSlot.copy();
        int containerSlots = kind.getSize();

        if (slotIndex < containerSlots) {
            if (!insertItem(inSlot, containerSlots, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!insertItem(inSlot, 0, containerSlots, false)) {
            return ItemStack.EMPTY;
        }

        if (inSlot.isEmpty()) {
            slot.setStack(ItemStack.EMPTY);
        } else {
            slot.markDirty();
        }

        return original;
    }

    /** A slot that only takes what its kind accepts, and never the box it lives in. */
    private final class FilteredSlot extends Slot {
        private FilteredSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return kind.accepts(stack);
        }
    }
}
