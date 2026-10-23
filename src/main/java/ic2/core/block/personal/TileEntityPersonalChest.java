package ic2.core.block.personal;

import ic2.core.block.TileEntityInventory;
import ic2.core.block.invslot.InvSlot;
import java.util.UUID;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

/**
 * Fabric port of TileEntityPersonalChest - a 54 slot chest only its owner may open.
 *
 * <p>The first player to open an unclaimed chest becomes its owner, exactly as checkAccess did in
 * 1.12.2; operators keep access. The slot group has no automation access, so hoppers cannot reach
 * around the check.
 */
public class TileEntityPersonalChest extends TileEntityInventory implements IPersonalBlock {
    public final InvSlot contentSlot;

    private UUID ownerId;
    private String ownerName;

    public TileEntityPersonalChest(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.contentSlot = new InvSlot(this, "content", InvSlot.Access.NONE, 54);
    }

    public TileEntityPersonalChest(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    public TileEntityPersonalChest() {
        this(null, BlockPos.ORIGIN, null);
    }

    /** Only the owner takes the safe out with a wrench, and only once it is empty. */
    @Override
    public boolean wrenchCanRemove(PlayerEntity player) {
        if (!permitsAccess(player)) {
            player.sendMessage(net.minecraft.text.Text.literal("This safe is owned by " + ownerName), false);
            return false;
        }
        if (!contentSlot.isEmpty()) {
            player.sendMessage(net.minecraft.text.Text.literal("Can't wrench non-empty safe"), false);
            return false;
        }
        return true;
    }

    @Override
    public boolean permitsAccess(PlayerEntity player) {
        if (world == null || world.isClient) {
            return true;
        }

        if (ownerId == null) {
            ownerId = player.getUuid();
            ownerName = player.getGameProfile().getName();
            markDirty();
            return true;
        }

        if (ownerId.equals(player.getUuid())) {
            return true;
        }

        // operators keep access, which is what the player list check did
        return player instanceof ServerPlayerEntity serverPlayer
                && serverPlayer.server.getPlayerManager().isOperator(serverPlayer.getGameProfile());
    }

    @Override
    public net.minecraft.text.Text getAccessDeniedMessage() {
        return net.minecraft.text.Text.literal("This safe is owned by "
                + (ownerName == null || ownerName.isEmpty() ? "somebody else" : ownerName));
    }

    @Override
    public UUID getOwnerId() {
        return ownerId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        if (ownerId != null) {
            nbt.putUuid("ownerId", ownerId);
            nbt.putString("ownerName", ownerName == null ? "" : ownerName);
        }
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        if (nbt.containsUuid("ownerId")) {
            ownerId = nbt.getUuid("ownerId");
            ownerName = nbt.getString("ownerName");
        }
    }
}
