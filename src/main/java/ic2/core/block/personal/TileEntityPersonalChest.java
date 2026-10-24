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

    /** The players with the safe's window open, on the server. */
    private final java.util.Set<PlayerEntity> viewers = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());
    /** How open the door is, 0 to 1, and when it started moving: the client animates from these. */
    private boolean open;
    private float doorFrom;
    private long doorChanged = Long.MIN_VALUE;
    /** Block event: the number of players looking into the safe, as a chest sends it. */
    private static final int VIEWERS_EVENT = 1;
    /** Ticks the door takes to swing fully open or shut, the ten steps of the 1.12.2 lid. */
    private static final float DOOR_TICKS = 10.0F;

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
    public void onGuiOpened(PlayerEntity player) {
        viewers.add(player);
        sendViewers();
    }

    @Override
    public void onGuiClosed(PlayerEntity player) {
        viewers.remove(player);
        sendViewers();
    }

    private void sendViewers() {
        if (world != null && !world.isClient) {
            world.addSyncedBlockEvent(pos, getCachedState().getBlock(), VIEWERS_EVENT, viewers.size());
        }
    }

    /** The client swings the door and plays the chest sounds as the first player opens it and the last closes it. */
    @Override
    public boolean onSyncedBlockEvent(int type, int data) {
        if (type != VIEWERS_EVENT) {
            return super.onSyncedBlockEvent(type, data);
        }
        if (world != null && world.isClient && open != data > 0) {
            doorFrom = getDoorOpening(0.0F);
            doorChanged = world.getTime();
            open = data > 0;
            world.playSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    open ? net.minecraft.sound.SoundEvents.BLOCK_CHEST_OPEN : net.minecraft.sound.SoundEvents.BLOCK_CHEST_CLOSE,
                    net.minecraft.sound.SoundCategory.BLOCKS, 0.5F, world.random.nextFloat() * 0.1F + 0.9F, false);
        }
        return true;
    }

    /** How far the door has swung open, 0 shut to 1 open, on the client. */
    public float getDoorOpening(float tickDelta) {
        if (world == null || doorChanged == Long.MIN_VALUE) {
            return open ? 1.0F : 0.0F;
        }
        float step = (world.getTime() - doorChanged + tickDelta) / DOOR_TICKS;
        float ret = open ? doorFrom + step : doorFrom - step;
        return net.minecraft.util.math.MathHelper.clamp(ret, 0.0F, 1.0F);
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
