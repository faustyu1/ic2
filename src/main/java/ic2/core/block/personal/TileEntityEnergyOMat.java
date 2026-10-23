package ic2.core.block.personal;

import ic2.api.energy.tile.IEnergyAcceptor;
import ic2.api.energy.tile.IEnergyEmitter;
import ic2.api.energy.tile.IEnergySink;
import ic2.api.energy.tile.IEnergySource;
import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.block.TileEntityInventory;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.invslot.InvSlotCharge;
import ic2.core.block.invslot.InvSlotConsumableLinked;
import ic2.core.block.invslot.InvSlotUpgrade;
import ic2.core.init.IC2Blocks;
import ic2.core.util.AdjacentInventories;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Fabric port of TileEntityEnergyOMat - sells energy for items.
 *
 * <p>The owner puts the price into the demand slot and sets how much EU one payment buys (1000 by
 * default, in steps of 100 to 100k, never below 100). A payment put into the input slot goes into
 * the inventories next to the block - only when it fits there - and pays for that much energy. The
 * energy-o-mat then takes up to the paid amount from the energy net on any side but its face,
 * buffers up to 10000 EU (10000 more per energy storage upgrade) and gives it out through its
 * face at tier 1 (one more per transformer upgrade), or into an item in the charge slot.
 */
public class TileEntityEnergyOMat extends TileEntityInventory implements IPersonalBlock, IEnergySink, IEnergySource, IUpgradableBlock {
    private final PersonalOwner owner = new PersonalOwner();
    public int euOffer = 1000;
    public int paidFor;
    public double euBuffer;
    private int euBufferMax = 10000;
    private int tier = 1;
    public final InvSlot demandSlot;
    public final InvSlotConsumableLinked inputSlot;
    public final InvSlotCharge chargeSlot;
    public final InvSlotUpgrade upgradeSlot;

    public TileEntityEnergyOMat(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.ENERGY_O_MAT_BE : type, pos, state);
        this.demandSlot = new InvSlot(this, "demand", InvSlot.Access.NONE, 1);
        this.inputSlot = new InvSlotConsumableLinked(this, "input", 1, demandSlot);
        this.chargeSlot = new InvSlotCharge(this, 1);
        this.upgradeSlot = new InvSlotUpgrade(this, "upgrade", 1);
    }

    public TileEntityEnergyOMat(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    @Override
    public void tick() {
        if (world == null || world.isClient) {
            return;
        }
        boolean invChanged = false;

        euBufferMax = 10000;
        tier = 1;
        chargeSlot.setTier(1);
        if (!upgradeSlot.isEmpty()) {
            euBufferMax = upgradeSlot.getEnergyStorage(10000, 0, 0);
            tier = 1 + upgradeSlot.extraTier;
            chargeSlot.setTier(tier);
        }

        ItemStack tradedIn = inputSlot.consumeLinked(true);
        if (tradedIn != null && !tradedIn.isEmpty()
                && AdjacentInventories.distribute(this, tradedIn, true) == tradedIn.getCount()) {
            AdjacentInventories.distribute(this, inputSlot.consumeLinked(false), false);
            paidFor += euOffer;
            invChanged = true;
        }

        if (euBuffer >= 1.0) {
            double sent = chargeSlot.charge(euBuffer);
            if (sent > 0.0) {
                euBuffer -= sent;
                invChanged = true;
            }
        }

        if (invChanged) {
            markDirty();
        }
    }

    // ---------------------------------------------------------------- energy

    @Override
    public boolean acceptsEnergyFrom(IEnergyEmitter emitter, Direction direction) {
        return direction != getFacing();
    }

    @Override
    public boolean emitsEnergyTo(IEnergyAcceptor receiver, Direction direction) {
        return direction == getFacing();
    }

    @Override
    public double getOfferedEnergy() {
        return euBuffer;
    }

    @Override
    public void drawEnergy(double amount) {
        euBuffer -= amount;
    }

    @Override
    public double getDemandedEnergy() {
        return Math.min(paidFor, euBufferMax - euBuffer);
    }

    @Override
    public double injectEnergy(Direction directionFrom, double amount, double voltage) {
        double toAdd = Math.min(Math.min(amount, paidFor), euBufferMax - euBuffer);
        paidFor = (int) (paidFor - toAdd);
        euBuffer += toAdd;
        return amount - toAdd;
    }

    @Override
    public int getSourceTier() {
        return tier;
    }

    @Override
    public int getSinkTier() {
        return Integer.MAX_VALUE;
    }

    @Override
    public double getEnergy() {
        return euBuffer;
    }

    /** As in 1.12.2, an upgrade asking for energy is told it got it without anything being taken. */
    @Override
    public boolean useEnergy(double amount) {
        return amount <= euBuffer;
    }

    @Override
    public Set<UpgradableProperty> getUpgradableProperties() {
        return EnumSet.of(UpgradableProperty.EnergyStorage, UpgradableProperty.Transformer);
    }

    // ---------------------------------------------------------------- owner and window

    @Override
    public UUID getOwnerId() {
        return owner.getId();
    }

    public String getOwnerName() {
        return owner.getName();
    }

    /** Whether the player may configure it; an unclaimed energy-o-mat becomes the player's. */
    public boolean isOwnerOrAdmin(PlayerEntity player) {
        return owner.checkAccess(this, player);
    }

    @Override
    public void onPlaced(ItemStack stack, LivingEntity placer) {
        super.onPlaced(stack, placer);
        if (placer instanceof PlayerEntity player && owner.getId() == null) {
            owner.claim(this, player);
        }
    }

    /** Everybody may open it - customers get the paying window, see {@link #getGuiName(PlayerEntity)}. */
    @Override
    public boolean permitsAccess(PlayerEntity player) {
        owner.checkAccess(this, player);
        return true;
    }

    @Override
    public String getGuiName(PlayerEntity player) {
        return player != null && owner.checkAccess(this, player) ? "energy_o_mat" : "energy_o_mat_closed";
    }

    @Override
    public boolean wrenchCanRemove(PlayerEntity player) {
        if (!isOwnerOrAdmin(player)) {
            player.sendMessage(owner.deniedMessage(), false);
            return false;
        }
        return true;
    }

    @Override
    public boolean canSetFacingWrench(Direction facing, PlayerEntity player) {
        return player != null && isOwnerOrAdmin(player) && super.canSetFacingWrench(facing, player);
    }

    @Override
    public Text getAccessDeniedMessage() {
        return owner.deniedMessage();
    }

    /** {@code offer:<amount>} changes the EU a payment buys, never below 100; only the owner may. */
    @Override
    public void onGuiEvent(PlayerEntity player, String event) {
        if (!event.startsWith("offer:") || player == null || !isOwnerOrAdmin(player)) {
            return;
        }
        try {
            attemptSet(Integer.parseInt(event.substring(6)));
            markDirty();
        } catch (NumberFormatException e) {
            // a malformed event changes nothing
        }
    }

    private void attemptSet(int amount) {
        euOffer += amount;
        if (euOffer < 100) {
            euOffer = 100;
        }
    }

    @Override
    public double getGuiAmount(String name) {
        return switch (name) {
            case "offer" -> euOffer;
            case "paid" -> paidFor;
            case "buffer" -> euBuffer;
            default -> super.getGuiAmount(name);
        };
    }

    // ---------------------------------------------------------------- persistence

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        owner.write(nbt);
        nbt.putInt("euOffer", euOffer);
        nbt.putInt("paidFor", paidFor);
        nbt.putDouble("euBuffer", euBuffer);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        owner.read(nbt);
        if (nbt.contains("euOffer")) {
            euOffer = nbt.getInt("euOffer");
        }
        paidFor = nbt.getInt("paidFor");
        euBuffer = nbt.getDouble("euBuffer");
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
        NbtCompound nbt = new NbtCompound();
        owner.write(nbt);
        return nbt;
    }

    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }
}
