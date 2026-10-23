package ic2.core.uu;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

/**
 * Port of {@code ic2.core.uu.LeanItemStack}: an item and an amount, the unit the UU graph works
 * in.
 *
 * <p>In 1.12.2 a node was told apart by item, metadata and NBT. The flattening gave every former
 * metadata variant its own item, and the components a stack carries (charge, damage, a hover flag)
 * describe its state rather than what it is, so a node is just the item now - which is also what
 * the scanner records and the replicator prints.
 */
final class LeanItemStack {
    private final Item item;
    private final int size;

    LeanItemStack(ItemStack stack) {
        this(stack.getItem(), stack.getCount());
    }

    LeanItemStack(ItemStack stack, int size) {
        this(stack.getItem(), size);
    }

    LeanItemStack(Item item, int size) {
        if (item == null) {
            throw new NullPointerException("null item");
        }
        this.item = item;
        this.size = size;
    }

    Item getItem() {
        return item;
    }

    int getSize() {
        return size;
    }

    boolean hasSameItem(LeanItemStack o) {
        return item == o.item;
    }

    LeanItemStack copyWithSize(int newSize) {
        return new LeanItemStack(item, newSize);
    }

    ItemStack toMcStack() {
        return size <= 0 ? ItemStack.EMPTY : new ItemStack(item, size);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof LeanItemStack o && item == o.item;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(item);
    }

    @Override
    public String toString() {
        Object name;
        try {
            name = Registries.ITEM.getId(item);
        } catch (RuntimeException | LinkageError e) {
            name = item;
        }
        return size + "x" + name;
    }
}
