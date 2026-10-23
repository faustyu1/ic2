package ic2.core.util;

import ic2.core.block.personal.IPersonalBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * The inventories around a block, port of StackUtil.getAdjacentInventories and the item moving
 * built on it ({@code putInInventory}, {@code distribute}, {@code fetch}).
 *
 * <p>Forge reached a neighbour through IInventory or the item handler capability; here it is the
 * Fabric item storage of the side facing the block, which covers vanilla chests, hoppers and every
 * IC2 inventory with its sided rules. A {@link IPersonalBlock} next to another one of the same
 * owner is reached as a whole, ignoring those rules, so a trade-o-mat can stock from its owner's
 * safe and nobody else's.
 */
public final class AdjacentInventories {
    private AdjacentInventories() {}

    /** A neighbouring inventory: the direction it lies in and the storage to move items with. */
    public record AdjacentInv(Direction dir, BlockEntity te, Storage<ItemVariant> storage) {}

    /** The inventory in a direction, or null when there is none the source may use. */
    public static AdjacentInv get(BlockEntity source, Direction dir) {
        World world = source.getWorld();
        if (world == null) {
            return null;
        }

        BlockEntity target = world.getBlockEntity(source.getPos().offset(dir));
        if (target instanceof IPersonalBlock personal && source instanceof IPersonalBlock own) {
            UUID owner = own.getOwnerId();
            if (owner != null && owner.equals(personal.getOwnerId()) && target instanceof Inventory inventory) {
                return new AdjacentInv(dir, target, InventoryStorage.of(inventory, null));
            }
        }

        Storage<ItemVariant> storage = ItemStorage.SIDED.find(world, source.getPos().offset(dir), dir.getOpposite());
        return storage == null ? null : new AdjacentInv(dir, target, storage);
    }

    /**
     * All neighbouring inventories, the trusted personal ones first, then in the order of the
     * directions (down, up, north, south, west, east).
     */
    public static List<AdjacentInv> getAll(BlockEntity source) {
        List<AdjacentInv> personal = new ArrayList<>();
        List<AdjacentInv> other = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            AdjacentInv inv = get(source, dir);
            if (inv != null) {
                (inv.te() instanceof IPersonalBlock ? personal : other).add(inv);
            }
        }
        personal.addAll(other);
        return personal;
    }

    /** Puts as much of the stack into the inventory as fits; returns the amount that went (or would go) in. */
    public static int put(AdjacentInv inv, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) {
            return 0;
        }
        try (Transaction transaction = Transaction.openOuter()) {
            long inserted = inv.storage().insert(ItemVariant.of(stack), stack.getCount(), transaction);
            if (!simulate) {
                transaction.commit();
            }
            return (int) inserted;
        }
    }

    /** Takes up to the stack's amount of its item out of the inventory; returns how many. */
    public static int take(AdjacentInv inv, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) {
            return 0;
        }
        try (Transaction transaction = Transaction.openOuter()) {
            long extracted = inv.storage().extract(ItemVariant.of(stack), stack.getCount(), transaction);
            if (!simulate) {
                transaction.commit();
            }
            return (int) extracted;
        }
    }

    /** Spreads the stack over the neighbouring inventories; returns how many items found a place. */
    public static int distribute(BlockEntity source, ItemStack stack, boolean simulate) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        int remaining = stack.getCount();
        for (AdjacentInv inv : getAll(source)) {
            remaining -= put(inv, stack.copyWithCount(remaining), simulate);
            if (remaining <= 0) {
                break;
            }
        }
        return stack.getCount() - remaining;
    }

    /**
     * Gathers the stack's amount of its item from the neighbouring inventories; returns how many
     * were found. The amount may exceed a stack, which is how the trade-o-mat counts its stock.
     */
    public static int fetch(BlockEntity source, ItemStack stack, int amount, boolean simulate) {
        if (stack == null || stack.isEmpty() || amount <= 0) {
            return 0;
        }
        int remaining = amount;
        for (AdjacentInv inv : getAll(source)) {
            try (Transaction transaction = Transaction.openOuter()) {
                remaining -= (int) inv.storage().extract(ItemVariant.of(stack), remaining, transaction);
                if (!simulate) {
                    transaction.commit();
                }
            }
            if (remaining <= 0) {
                break;
            }
        }
        return amount - remaining;
    }
}
