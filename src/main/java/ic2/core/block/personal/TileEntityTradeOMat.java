package ic2.core.block.personal;

import ic2.core.block.TileEntityInventory;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.invslot.InvSlotConsumableLinked;
import ic2.core.block.invslot.InvSlotOutput;
import ic2.core.init.IC2Blocks;
import ic2.core.init.IC2ItemVariants;
import ic2.core.init.IC2Sounds;
import ic2.core.util.AdjacentInventories;
import java.util.ArrayList;
import java.util.List;
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
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric port of TileEntityTradeOMat - a vending machine that trades items for items.
 *
 * <p>The owner sets what it wants in the demand slot and what it gives in the offer slot. A
 * customer puts the payment into the input slot; when the offer can be taken from the inventories
 * next to the trade-o-mat (the owner's safe counts) and the payment fits into them, the offer comes
 * out of the output slot. The stock shown is how many trades the neighbours can still serve,
 * recounted every 64 ticks. An operator can make it infinite: then it trades from nothing and the
 * payment is used up. A remote interface upgrade used on it puts it on the trading market for
 * trading terminals. Only the owner opens its configuration or wrenches it.
 */
public class TileEntityTradeOMat extends TileEntityInventory implements IPersonalBlock {
    private static final Logger LOG = LoggerFactory.getLogger("IC2-Block");
    private static final int STOCK_UPDATE_RATE = 64;

    private final PersonalOwner owner = new PersonalOwner();
    private int ticker;
    public int totalTradeCount = 0;
    public int stock = 0;
    public boolean infinite = false;
    public final InvSlot demandSlot;
    public final InvSlot offerSlot;
    public final InvSlotConsumableLinked inputSlot;
    public final InvSlotOutput outputSlot;

    public TileEntityTradeOMat(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.TRADE_O_MAT_BE : type, pos, state);
        this.ticker = new java.util.Random().nextInt(STOCK_UPDATE_RATE);
        this.demandSlot = new InvSlot(this, "demand", InvSlot.Access.NONE, 1);
        this.offerSlot = new InvSlot(this, "offer", InvSlot.Access.NONE, 1);
        this.inputSlot = new InvSlotConsumableLinked(this, "input", 1, demandSlot);
        this.outputSlot = new InvSlotOutput(this, "output", 1);
    }

    public TileEntityTradeOMat(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    // ---------------------------------------------------------------- trading

    @Override
    public void tick() {
        if (world == null || world.isClient) {
            return;
        }
        trade();
        if (infinite) {
            stock = -1;
        } else if (++ticker % STOCK_UPDATE_RATE == 0) {
            updateStock();
        }
    }

    private void trade() {
        ItemStack tradedIn = inputSlot.consumeLinked(true);
        if (tradedIn == null || tradedIn.isEmpty()) {
            return;
        }
        ItemStack offer = offerSlot.get();
        if (offer.isEmpty() || !outputSlot.canAdd(offer)) {
            return;
        }

        if (infinite) {
            inputSlot.consumeLinked(false);
            outputSlot.add(offer.copy());
        } else {
            if (AdjacentInventories.fetch(this, offer, offer.getCount(), true) != offer.getCount()) {
                return;
            }
            if (AdjacentInventories.distribute(this, tradedIn, true) != tradedIn.getCount()) {
                return;
            }
            int amount = AdjacentInventories.fetch(this, offer, offer.getCount(), false);
            if (amount == 0) {
                return;
            }
            if (amount != offer.getCount()) {
                LOG.warn("The Trade-O-Mat at {} received an inconsistent result from an adjacent trade supply inventory, the {} items will be lost.",
                        pos, amount);
                return;
            }
            AdjacentInventories.distribute(this, inputSlot.consumeLinked(false), false);
            outputSlot.add(offer.copy());
            stock--;
        }

        totalTradeCount++;
        world.playSound(null, pos, IC2Sounds.MACHINES_O_MAT, SoundCategory.BLOCKS, 1.0f, 1.0f);
        markDirty();
    }

    /** How many times the offer can still be taken from the neighbours. */
    public void updateStock() {
        ItemStack offer = offerSlot.get();
        stock = offer.isEmpty() ? 0 : AdjacentInventories.fetch(this, offer, Integer.MAX_VALUE, true) / offer.getCount();
    }

    // ---------------------------------------------------------------- the trading market

    public final boolean isWireless() {
        return getActive();
    }

    public final boolean setWireless(boolean wireless) {
        if (isWireless() == wireless) {
            return false;
        }
        setActive(wireless);
        if (wireless) {
            TradingMarket.get(world).registerTradeOMat(this);
        } else {
            TradingMarket.get(world).unregisterTradeOMat(this);
        }
        return true;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (world != null && !world.isClient) {
            updateStock();
            if (isWireless()) {
                TradingMarket.get(world).registerTradeOMat(this);
            }
        }
    }

    @Override
    public void onUnloaded() {
        super.onUnloaded();
        if (world != null && !world.isClient && isWireless()) {
            TradingMarket.get(world).unregisterTradeOMat(this);
        }
    }

    /** A remote interface upgrade used on it makes it wireless; it is kept and dropped with the block. */
    @Override
    public boolean onActivated(PlayerEntity player, Hand hand) {
        ItemStack held = player.getStackInHand(hand);
        if (!isWireless() && held.isOf(IC2ItemVariants.get("upgrade", "remote_interface"))) {
            if (!player.getAbilities().creativeMode) {
                held.decrement(1);
            }
            if (!world.isClient) {
                setWireless(true);
            }
            return true;
        }
        return super.onActivated(player, hand);
    }

    @Override
    public List<ItemStack> getSelfDrops(boolean wrench) {
        List<ItemStack> drops = new ArrayList<>(super.getSelfDrops(wrench));
        if (isWireless()) {
            drops.add(new ItemStack(IC2ItemVariants.get("upgrade", "remote_interface")));
        }
        return drops;
    }

    // ---------------------------------------------------------------- owner

    @Override
    public UUID getOwnerId() {
        return owner.getId();
    }

    public String getOwnerName() {
        return owner.getName();
    }

    /** Whether the player may configure it; an unclaimed trade-o-mat becomes the player's. */
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

    /** Everybody may open it - customers get the trading window, see {@link #getGuiName(PlayerEntity)}. */
    @Override
    public boolean permitsAccess(PlayerEntity player) {
        owner.checkAccess(this, player);
        return true;
    }

    @Override
    public String getGuiName(PlayerEntity player) {
        if (player == null || !owner.checkAccess(this, player)) {
            return "trade_o_mat_closed";
        }
        return player.hasPermissionLevel(2) ? "trade_o_mat_admin" : "trade_o_mat";
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
    public Text getAccessDeniedMessage() {
        return owner.deniedMessage();
    }

    /** {@code infinite}: an operator switches unlimited stock on or off. */
    @Override
    public void onGuiEvent(PlayerEntity player, String event) {
        if ("infinite".equals(event) && player != null && player.hasPermissionLevel(2)) {
            infinite = !infinite;
            if (!infinite) {
                updateStock();
            }
            markDirty();
        }
    }

    @Override
    public double getGuiAmount(String name) {
        return switch (name) {
            case "stock" -> stock;
            case "trades" -> totalTradeCount;
            default -> super.getGuiAmount(name);
        };
    }

    @Override
    public boolean getGuiCondition(String name) {
        return switch (name) {
            case "infinite" -> stock < 0;
            case "nostock" -> stock == 0;
            default -> super.getGuiCondition(name);
        };
    }

    // ---------------------------------------------------------------- persistence

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        owner.write(nbt);
        nbt.putInt("totalTradeCount", totalTradeCount);
        if (infinite) {
            nbt.putBoolean("infinite", true);
        }
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        owner.read(nbt);
        totalTradeCount = nbt.getInt("totalTradeCount");
        infinite = nbt.getBoolean("infinite");
    }

    /** The client learns the owner, which decides the window it shows. */
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
