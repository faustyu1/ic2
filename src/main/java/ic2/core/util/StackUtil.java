package ic2.core.util;

import java.util.List;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.Direction;

/** Fabric port of the parts of ic2.core.util.StackUtil the machines use. */
public final class StackUtil {
    private StackUtil() {
    }

    /**
     * Hands items to the inventories around a block, in the order of the sides, and drops what
     * none of them takes on top of the block - distributeDrops in 1.12.2.
     */
    public static void distributeDrops(BlockEntity source, List<ItemStack> drops) {
        for (ItemStack drop : drops) {
            ItemStack rest = drop.copy();
            for (Direction dir : Direction.values()) {
                if (rest.isEmpty()) {
                    break;
                }
                Storage<ItemVariant> target = ItemStorage.SIDED.find(source.getWorld(), source.getPos().offset(dir), dir.getOpposite());
                if (target == null) {
                    continue;
                }
                try (Transaction tx = Transaction.openOuter()) {
                    long inserted = target.insert(ItemVariant.of(rest), rest.getCount(), tx);
                    tx.commit();
                    rest.decrement((int) inserted);
                }
            }
            if (!rest.isEmpty()) {
                ItemScatterer.spawn(source.getWorld(), source.getPos().getX() + 0.5, source.getPos().getY() + 1.0,
                        source.getPos().getZ() + 0.5, rest);
            }
        }
    }
}
