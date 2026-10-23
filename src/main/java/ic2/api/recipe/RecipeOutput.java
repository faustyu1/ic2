package ic2.api.recipe;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

@Deprecated
public final class RecipeOutput {
    public final List<ItemStack> items;
    public final NbtCompound metadata;

    public RecipeOutput(NbtCompound metadata, List<ItemStack> items) {
        assert !items.contains(null);
        this.metadata = metadata;
        this.items = items;
    }

    public RecipeOutput(NbtCompound metadata, ItemStack... items) {
        this(metadata, Arrays.asList(items));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RecipeOutput other) {
            if (items.size() == other.items.size()
                    && (metadata == null && other.metadata == null
                            || metadata != null && other.metadata != null && metadata.equals(other.metadata))) {
                Iterator<ItemStack> itA = items.iterator();
                Iterator<ItemStack> itB = other.items.iterator();
                while (itA.hasNext() && itB.hasNext()) {
                    ItemStack stackA = itA.next();
                    ItemStack stackB = itB.next();
                    if (ItemStack.areEqual(stackA, stackB)) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "ROutput<" + items + "," + metadata + ">";
    }
}
