package ic2.core.item.handheld;

import ic2.core.init.IC2ScreenHandlers;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import java.util.function.Predicate;
import net.minecraft.util.collection.DefaultedList;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;

/**
 * Fabric port of {@code ic2.core.item.tool.HandHeldInventory}: an inventory that lives inside the
 * stack the player is holding.
 *
 * <p>1.12.2 wrote the contents into the stack's NBT and kept a random {@code uid} in it so that
 * saving could find the stack again after the inventory had been copied around. 1.21.1 has a
 * component for exactly this ({@link DataComponentTypes#CONTAINER}) and the held stack can be
 * looked up by hand, so the uid is gone: every change writes straight back into the stack the
 * player has in that hand.
 */
public class HandHeldInventory implements Inventory, ExtendedScreenHandlerFactory<HandHeldScreenHandler.OpenData> {

    private final PlayerEntity player;
    private final Hand hand;
    /** Null for a container that is not one of the box kinds, such as the cropnalyzer. */
    private final HandHeldKind kind;
    private final Predicate<ItemStack> filter;
    private final DefaultedList<ItemStack> stacks;

    public HandHeldInventory(PlayerEntity player, Hand hand, HandHeldKind kind) {
        this(player, hand, kind, kind.getSize(), kind::accepts);
    }

    /**
     * A stack-backed inventory of {@code size} slots that is not one of the box kinds: the
     * cropnalyzer's three slots, where what fits depends on the slot and so is decided by the
     * screen handler instead.
     */
    public HandHeldInventory(PlayerEntity player, Hand hand, int size, Predicate<ItemStack> filter) {
        this(player, hand, null, size, filter);
    }

    private HandHeldInventory(PlayerEntity player, Hand hand, HandHeldKind kind, int size,
                              Predicate<ItemStack> filter) {
        this.player = player;
        this.hand = hand;
        this.kind = kind;
        this.filter = filter;
        this.stacks = DefaultedList.ofSize(size, ItemStack.EMPTY);

        container().copyTo(stacks);
    }

    public HandHeldKind getKind() {
        return kind;
    }

    /** The stack this inventory lives in, looked up fresh so it is never a stale copy. */
    public ItemStack containerStack() {
        return player.getStackInHand(hand);
    }

    private ContainerComponent container() {
        return containerStack().getOrDefault(DataComponentTypes.CONTAINER, ContainerComponent.DEFAULT);
    }

    // ---- Inventory ----

    @Override
    public int size() {
        return stacks.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        return stacks.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack removed = net.minecraft.inventory.Inventories.splitStack(stacks, slot, amount);
        if (!removed.isEmpty()) markDirty();
        return removed;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack removed = net.minecraft.inventory.Inventories.removeStack(stacks, slot);
        if (!removed.isEmpty()) markDirty();
        return removed;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        stacks.set(slot, stack);
        if (stack.getCount() > getMaxCountPerStack()) {
            stack.setCount(getMaxCountPerStack());
        }
        markDirty();
    }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        return filter.test(stack);
    }

    /** Writes the contents back into the held stack. */
    @Override
    public void markDirty() {
        ItemStack container = containerStack();
        if (container.isEmpty()) return;

        // a box must never end up inside itself
        for (int i = 0; i < stacks.size(); i++) {
            if (stacks.get(i) == container) {
                stacks.set(i, ItemStack.EMPTY);
            }
        }

        container.set(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(stacks));
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return player == this.player && !containerStack().isEmpty();
    }

    @Override
    public void clear() {
        stacks.clear();
        markDirty();
    }

    // ---- opening the screen ----

    @Override
    public Text getDisplayName() {
        return containerStack().getName();
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new HandHeldScreenHandler(IC2ScreenHandlers.HAND_HELD, syncId, playerInventory, this, kind);
    }

    /** Writes the contents back, for a caller that changed a stack in place. */
    public void save() {
        markDirty();
    }

    @Override
    public HandHeldScreenHandler.OpenData getScreenOpeningData(net.minecraft.server.network.ServerPlayerEntity player) {
        return new HandHeldScreenHandler.OpenData(hand, kind);
    }
}
