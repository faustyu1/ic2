package ic2.core.block.invslot;

import ic2.core.block.IInventorySlotHolder;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.Direction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric port of ic2.core.block.invslot.InvSlot - a named group of item slots inside a machine.
 *
 * <p>IC2 does not address machine inventories by raw index: every logical group (input, output,
 * upgrades, ...) is an InvSlot that knows its own size, access direction and what it accepts. The
 * guidef files reference these groups by name, and the vanilla {@code Inventory} view is built by
 * concatenating them, which is what {@link ic2.core.block.TileEntityInventory} does.
 *
 * <p>Only the storage semantics changed for 1.21: stacks serialise through
 * {@code ItemStack.encode} instead of writing themselves into a compound.
 */
public class InvSlot implements Iterable<ItemStack> {
    private static final Logger LOG = LoggerFactory.getLogger("IC2-Inventory");

    public final IInventorySlotHolder<?> base;
    public final String name;
    private final ItemStack[] contents;
    protected final Access access;
    public final InvSide preferredSide;
    private int stackSizeLimit;

    public InvSlot(IInventorySlotHolder<?> base, String name, Access access, int count) {
        this(base, name, access, count, InvSide.ANY);
    }

    public InvSlot(IInventorySlotHolder<?> base, String name, Access access, int count, InvSide preferredSide) {
        if (count <= 0) {
            throw new IllegalArgumentException("invalid slot count: " + count);
        }
        this.contents = new ItemStack[count];
        clear();
        this.base = base;
        this.name = name;
        this.access = access;
        this.preferredSide = preferredSide;
        this.stackSizeLimit = 64;
        base.addInventorySlot(this);
    }

    /** Detached slot, used for scratch inventories that are not owned by a block. */
    public InvSlot(int count) {
        this.contents = new ItemStack[count];
        clear();
        this.base = null;
        this.name = null;
        this.access = Access.NONE;
        this.preferredSide = InvSide.ANY;
        this.stackSizeLimit = 64;
    }

    public void readFromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        clear();
        NbtList contentsTag = nbt.getList("Contents", NbtElement.COMPOUND_TYPE);

        for (int i = 0; i < contentsTag.size(); i++) {
            NbtCompound contentTag = contentsTag.getCompound(i);
            int index = contentTag.getByte("Index") & 255;
            if (index >= size()) {
                LOG.error("[IC2] Can't load item stack for slot {}, index {} is out of bounds", name, index);
                continue;
            }

            ItemStack stack = ItemStack.fromNbt(registries, contentTag).orElse(ItemStack.EMPTY);
            if (stack.isEmpty()) {
                LOG.warn("[IC2] Can't load item stack for slot {}, index {}, no matching item", name, index);
                continue;
            }

            if (!isEmpty(index)) {
                LOG.error("[IC2] Loading content into non-empty slot {}, index {}, replacing {}", name, index, get(index));
            }
            putFromNbt(index, stack);
        }

        onChanged();
    }

    public void writeToNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        NbtList contentsTag = new NbtList();

        for (int i = 0; i < contents.length; i++) {
            ItemStack content = contents[i];
            if (content.isEmpty()) {
                continue;
            }
            NbtCompound contentTag = (NbtCompound) content.encode(registries);
            contentTag.putByte("Index", (byte) i);
            contentsTag.add(contentTag);
        }

        nbt.put("Contents", contentsTag);
    }

    public int size() {
        return contents.length;
    }

    public boolean isEmpty() {
        for (ItemStack stack : contents) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public boolean isEmpty(int index) {
        return contents[index].isEmpty();
    }

    public ItemStack get() {
        return get(0);
    }

    public ItemStack get(int index) {
        return contents[index];
    }

    public void put(ItemStack content) {
        put(0, content);
    }

    protected void putFromNbt(int index, ItemStack content) {
        contents[index] = content;
    }

    public void put(int index, ItemStack content) {
        contents[index] = content == null ? ItemStack.EMPTY : content;
        onChanged();
    }

    public void clear() {
        Arrays.fill(contents, ItemStack.EMPTY);
    }

    public void clear(int index) {
        put(index, ItemStack.EMPTY);
    }

    /** Hook for subclasses; the owner is notified separately through {@link #markDirty()}. */
    public void onChanged() {
        markDirty();
    }

    protected void markDirty() {
        if (base != null) {
            base.onInventoryChanged();
        }
    }

    /** Whether this slot group accepts the stack at all, regardless of space. */
    public boolean accepts(ItemStack stack) {
        return true;
    }

    public boolean canInput() {
        return access == Access.I || access == Access.IO;
    }

    public boolean canOutput() {
        return access == Access.O || access == Access.IO;
    }

    public Access getAccess() {
        return access;
    }

    /** Merges partial stacks towards the front, as the original did after every operation. */
    public void organize() {
        for (int dstIndex = 0; dstIndex < contents.length - 1; dstIndex++) {
            ItemStack dst = contents[dstIndex];
            if (!dst.isEmpty() && dst.getCount() >= dst.getMaxCount()) {
                continue;
            }

            for (int srcIndex = dstIndex + 1; srcIndex < contents.length; srcIndex++) {
                ItemStack src = contents[srcIndex];
                if (src.isEmpty()) {
                    continue;
                }

                if (dst.isEmpty()) {
                    contents[srcIndex] = ItemStack.EMPTY;
                    contents[dstIndex] = dst = src;
                } else if (ItemStack.areItemsAndComponentsEqual(dst, src)) {
                    int space = Math.min(getStackSizeLimit(), dst.getMaxCount()) - dst.getCount();
                    if (space <= 0) {
                        break;
                    }
                    int srcSize = src.getCount();
                    if (srcSize > space) {
                        src.decrement(space);
                        dst.increment(space);
                        break;
                    }
                    contents[srcIndex] = ItemStack.EMPTY;
                    dst.increment(srcSize);
                    if (srcSize == space) {
                        break;
                    }
                }
            }
        }
    }

    public int getStackSizeLimit() {
        return stackSizeLimit;
    }

    public void setStackSizeLimit(int stackSizeLimit) {
        this.stackSizeLimit = stackSizeLimit;
    }

    @Override
    public Iterator<ItemStack> iterator() {
        return new Iterator<>() {
            private int idx = 0;

            @Override
            public boolean hasNext() {
                return idx < contents.length;
            }

            @Override
            public ItemStack next() {
                if (idx >= contents.length) {
                    throw new NoSuchElementException();
                }
                return contents[idx++];
            }
        };
    }

    @Override
    public String toString() {
        return name + Arrays.toString(contents);
    }

    protected ItemStack[] backup() {
        ItemStack[] ret = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {
            ret[i] = contents[i].copy();
        }
        return ret;
    }

    protected void restore(ItemStack[] backup) {
        if (backup.length != contents.length) {
            throw new IllegalArgumentException("invalid array size");
        }
        System.arraycopy(backup, 0, contents, 0, contents.length);
    }

    public void onPickupFromSlot(PlayerEntity player, ItemStack stack) {
    }

    /** Which way items may move through this slot group. */
    public enum Access {
        NONE,
        I,
        O,
        IO;

        public boolean isInput() {
            return (ordinal() & 1) != 0;
        }

        public boolean isOutput() {
            return (ordinal() & 2) != 0;
        }
    }

    /** Which block sides automation may use for this slot group. */
    public enum InvSide {
        ANY(Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST),
        TOP(Direction.UP),
        BOTTOM(Direction.DOWN),
        SIDE(Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST),
        NOTSIDE();

        private final Set<Direction> acceptedSides;

        InvSide(Direction... sides) {
            if (sides.length == 0) {
                this.acceptedSides = Collections.emptySet();
            } else {
                Set<Direction> accepted = EnumSet.noneOf(Direction.class);
                accepted.addAll(Arrays.asList(sides));
                this.acceptedSides = Collections.unmodifiableSet(accepted);
            }
        }

        public boolean matches(Direction side) {
            return acceptedSides.contains(side);
        }

        public Set<Direction> getAcceptedSides() {
            return acceptedSides;
        }
    }
}
