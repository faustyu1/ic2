package ic2.core.block.transport.cover;

import ic2.core.IC2;
import ic2.core.util.LogCategory;
import java.util.Arrays;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.Direction;

/**
 * The covers on the six sides of a block, one item each. They tick with the block on the server,
 * before the block itself as the 1.12.2 component did, and are saved with it.
 */
public class Covers {
    private final ICoverHolder parent;
    private final ItemStack[] covers = new ItemStack[6];

    public Covers(ICoverHolder parent) {
        this.parent = parent;
    }

    public void addCover(Direction side, ItemStack cover) {
        if (!hasCover(side)) {
            covers[side.ordinal()] = cover.copyWithCount(1);
        }
    }

    /** Takes the cover off that side; empty when there was none. */
    public ItemStack removeCover(Direction side) {
        ItemStack ret = covers[side.ordinal()];
        covers[side.ordinal()] = null;
        return ret == null ? ItemStack.EMPTY : ret;
    }

    public boolean hasCover(Direction side) {
        ItemStack stack = covers[side.ordinal()];
        return stack != null && !stack.isEmpty();
    }

    public ICoverItem getCoverItem(Direction side) {
        return hasCover(side) ? (ICoverItem) covers[side.ordinal()].getItem() : null;
    }

    public void onWorldTick() {
        for (Direction side : Direction.values()) {
            if (hasCover(side)) {
                ItemStack stack = covers[side.ordinal()];
                ((ICoverItem) stack.getItem()).onTick(stack, parent, side);
            }
        }
    }

    public void readFromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        Arrays.fill(covers, null);
        NbtList list = nbt.getList("covers", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound tag = list.getCompound(i);
            int index = tag.getByte("facing") & 255;
            if (index >= covers.length) {
                IC2.log.warn(LogCategory.Block, "Can't load cover, index %d is out of bounds.", index);
                continue;
            }
            ItemStack cover = ItemStack.fromNbtOrEmpty(lookup, tag.getCompound("item"));
            if (cover.isEmpty() || !(cover.getItem() instanceof ICoverItem)) {
                IC2.log.warn(LogCategory.Block, "Can't load cover %s, index %d.", tag, index);
                continue;
            }
            covers[index] = cover;
        }
    }

    public void writeToNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        NbtList list = new NbtList();
        for (Direction side : Direction.values()) {
            if (hasCover(side)) {
                NbtCompound tag = new NbtCompound();
                tag.putByte("facing", (byte) side.ordinal());
                tag.put("item", covers[side.ordinal()].encode(lookup));
                list.add(tag);
            }
        }
        nbt.put("covers", list);
    }
}
