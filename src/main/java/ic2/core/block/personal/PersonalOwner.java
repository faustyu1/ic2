package ic2.core.block.personal;

import java.util.UUID;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;

/**
 * Who owns a trade-o-mat or an energy-o-mat, and the check TileEntityPersonalChest.checkAccess did
 * for them: the first player to place or use an unclaimed block becomes its owner, the owner and
 * operators may configure it, everybody else only trades.
 *
 * <p>The owner travels to the client with the block's update packet, because the client picks the
 * owner's or the customer's window itself.
 */
final class PersonalOwner {
    private UUID id;
    private String name = "";

    UUID getId() {
        return id;
    }

    String getName() {
        return name;
    }

    /**
     * Whether the player may configure the block. On the server an unclaimed block becomes the
     * player's; on the client a block whose owner is not known yet counts as the player's, as in
     * 1.12.2.
     */
    boolean checkAccess(BlockEntity block, PlayerEntity player) {
        if (player == null) {
            return id == null;
        }
        if (id == null) {
            if (block.getWorld() != null && !block.getWorld().isClient) {
                claim(block, player);
            }
            return true;
        }
        return id.equals(player.getUuid()) || player.hasPermissionLevel(2);
    }

    /** Makes the player the owner and tells the clients. */
    void claim(BlockEntity block, PlayerEntity player) {
        id = player.getUuid();
        name = player.getGameProfile().getName();
        block.markDirty();
        if (block.getWorld() != null && !block.getWorld().isClient) {
            block.getWorld().updateListeners(block.getPos(), block.getCachedState(), block.getCachedState(), Block.NOTIFY_LISTENERS);
        }
    }

    Text deniedMessage() {
        return Text.literal("Owned by " + (name.isEmpty() ? "somebody else" : name));
    }

    void write(NbtCompound nbt) {
        if (id != null) {
            nbt.putUuid("ownerId", id);
            nbt.putString("ownerName", name);
        }
    }

    void read(NbtCompound nbt) {
        if (nbt.containsUuid("ownerId")) {
            id = nbt.getUuid("ownerId");
            name = nbt.getString("ownerName");
        }
    }
}
