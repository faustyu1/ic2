package ic2.core.block.wiring;

import ic2.api.energy.tile.IEnergySink;
import ic2.api.energy.tile.IEnergySource;
import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;
import ic2.api.tile.IEnergyStorage;
import ic2.core.block.TileEntityInventory;
import ic2.core.block.invslot.InvSlotCharge;
import ic2.core.block.invslot.InvSlotDischarge;
import ic2.core.ref.TeBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Fabric port of TileEntityElectricBlock - the energy storage blocks (BatBox, CESU, MFE, MFSU).
 *
 * <p>Besides holding energy they charge and discharge items: the top slot fills a battery from the
 * buffer, the bottom one empties one into it, which is what the two slots of the 1.12.2 GUI did.
 */
public abstract class TileEntityElectricBlock extends TileEntityInventory implements IEnergyStorage, IEnergySink, IEnergySource {

    protected double energyStored;
    protected final int tier;
    protected final int maxStorage;
    protected final int output;

    /** What the block does with redstone, cycled by the window's button: see {@link #shouldEmitRedstone}. */
    public byte redstoneMode;
    private int emittedRedstone;

    /** Battery being filled from the buffer. */
    public final InvSlotCharge chargeSlot;
    /** Battery being emptied into the buffer. */
    public final InvSlotDischarge dischargeSlot;

    public TileEntityElectricBlock(BlockEntityType<?> type, BlockPos pos, BlockState state, TeBlock teBlock, int tier, int output, int maxStorage) {
        super(type, pos, state, teBlock);
        this.tier = tier;
        this.output = output;
        this.maxStorage = maxStorage;
        this.chargeSlot = new InvSlotCharge(this, tier);
        this.dischargeSlot = new InvSlotDischarge(this, "discharge", tier);
    }

    // For subclasses with dummy type
    public TileEntityElectricBlock(BlockEntityType<?> type, BlockPos pos, BlockState state, int tier, int output, int maxStorage) {
        super(type, pos, state);
        this.tier = tier;
        this.output = output;
        this.maxStorage = maxStorage;
        this.chargeSlot = new InvSlotCharge(this, tier);
        this.dischargeSlot = new InvSlotDischarge(this, "discharge", tier);
    }

    /** Moves energy between the buffer and the two battery slots. */
    @Override
    public void tick() {
        if (world == null || world.isClient || ElectricItem.manager == null) {
            return;
        }

        if (energyStored > 0 && !chargeSlot.isEmpty()) {
            double charged = chargeSlot.charge(energyStored);
            if (charged > 0) {
                energyStored -= charged;
                markDirty();
            }
        }

        double space = maxStorage - energyStored;
        ItemStack battery = dischargeSlot.get();
        if (space > 0 && !battery.isEmpty() && battery.getItem() instanceof IElectricItem) {
            double drained = ElectricItem.manager.discharge(battery, space, tier, false, true, false);
            if (drained > 0) {
                energyStored += drained;
                markDirty();
            }
        }

        updateRedstone();
    }

    /** Sets the signal the block gives off and tells the neighbours when it changed. */
    protected void updateRedstone() {
        int level = shouldEmitRedstone() ? 15 : 0;
        if (level != emittedRedstone) {
            emittedRedstone = level;
            world.updateNeighbors(pos, getCachedState().getBlock());
        }
    }

    @Override
    public int getRedstoneOutput() {
        return emittedRedstone;
    }

    protected int redstoneModes() {
        return 7;
    }

    /** 1: emit when full, 2: partly filled, 3: not full, 4: empty; 0, 5 and 6 emit nothing. */
    protected boolean shouldEmitRedstone() {
        return switch (redstoneMode) {
            case 1 -> energyStored >= maxStorage - output * 20.0;
            case 2 -> energyStored > output && energyStored < maxStorage - output;
            case 3 -> energyStored < maxStorage - output;
            case 4 -> energyStored < output;
            default -> false;
        };
    }

    /** 5: no output while powered by redstone, 6: none while powered unless nearly full. */
    protected boolean shouldEmitEnergy() {
        boolean redstone = world != null && world.isReceivingRedstonePower(pos);
        if (redstoneMode == 5) {
            return !redstone;
        }
        return redstoneMode != 6 || !redstone || energyStored > maxStorage - output * 20.0;
    }

    public String getRedstoneModeKey() {
        return "EUStorage.gui.mod.redstone" + redstoneMode;
    }

    @Override
    public void onGuiEvent(net.minecraft.entity.player.PlayerEntity player, String event) {
        if ("redstone".equals(event)) {
            redstoneMode = (byte) ((redstoneMode + 1) % redstoneModes());
            markDirty();
            if (player != null) {
                player.sendMessage(net.minecraft.text.Text.translatable(getRedstoneModeKey()), false);
            }
        }
    }

    @Override
    public int getComparatorOutput() {
        return maxStorage <= 0 || energyStored <= 0 ? 0 : (int) Math.round(energyStored / maxStorage * 14.0) + 1;
    }

    @Override
    public double getGuiValue(String name) {
        return switch (name) {
            case "energy" -> maxStorage == 0 ? 0.0 : energyStored / maxStorage;
            default -> super.getGuiValue(name);
        };
    }

    @Override
    public double getGuiAmount(String name) {
        return switch (name) {
            case "energy" -> Math.min(energyStored, maxStorage);
            case "output" -> output;
            default -> super.getGuiAmount(name);
        };
    }

    @Override
    public boolean getGuiCondition(String name) {
        if (name.startsWith("redstone")) {
            return name.equals("redstone" + redstoneMode);
        }
        return super.getGuiCondition(name);
    }

    @Override
    public double getGuiCapacity(String name) {
        return "energy".equals(name) ? maxStorage : super.getGuiCapacity(name);
    }

    // IEnergyStorage
    @Override public int getStored() { return (int) energyStored; }

    @Override public double getEnergyForDisplay() { return energyStored; }
    @Override public double getEnergyCapacityForDisplay() { return maxStorage; }
    @Override public void setStored(int amount) { this.energyStored = Math.max(0, Math.min(amount, maxStorage)); markDirty(); }
    @Override public int addEnergy(int amount) {
        int canAdd = Math.min(amount, maxStorage - (int) energyStored);
        energyStored += canAdd;
        markDirty();
        return canAdd;
    }
    @Override public int getCapacity() { return maxStorage; }
    @Override public int getOutput() { return output; }
    @Override public double getOutputEnergyUnitsPerTick() { return output; }
    @Override public boolean isTeleporterCompatible(Direction side) { return true; }

    // IEnergySink
    /** Energy comes in on every side but the face, as the 1.12.2 energy component was set up. */
    @Override public boolean acceptsEnergyFrom(ic2.api.energy.tile.IEnergyEmitter emitter, Direction side) { return side != getFacing(); }
    @Override public double getDemandedEnergy() { return Math.max(0, maxStorage - energyStored); }
    @Override public double injectEnergy(Direction directionFrom, double amount, double voltage) {
        double demanded = getDemandedEnergy();
        double toAdd = Math.min(amount, demanded);
        energyStored += toAdd;
        markDirty();
        return amount - toAdd;
    }
    @Override public int getSinkTier() { return tier; }

    // IEnergySource
    /** ... and leaves through the face only. */
    @Override public boolean emitsEnergyTo(ic2.api.energy.tile.IEnergyAcceptor receiver, Direction side) { return side == getFacing(); }
    @Override public double getOfferedEnergy() { return shouldEmitEnergy() ? Math.min(energyStored, output) : 0.0; }
    @Override public void drawEnergy(double amount) { energyStored = Math.max(0, energyStored - amount); markDirty(); }
    @Override public int getSourceTier() { return tier; }

    // NBT
    @Override public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putDouble("energyStored", energyStored);
        nbt.putByte("redstoneMode", redstoneMode);
    }

    @Override public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        if (nbt.contains("energyStored")) energyStored = nbt.getDouble("energyStored");
        redstoneMode = nbt.getByte("redstoneMode");
    }

    // ---------------------------------------------------------------- the dropped item

    /**
     * Wrenched, or mined from a block that drops itself, the storage keeps a share of its energy
     * in the item - {@code balance/energyRetainedInStorageBlockDrops}, 80% by default.
     */
    @Override
    protected net.minecraft.item.ItemStack adjustDrop(net.minecraft.item.ItemStack drop, boolean wrench) {
        drop = super.adjustDrop(drop, wrench);
        if (drop != null && (wrench || getDropTeBlock().getDefaultDrop() == TeBlock.DefaultDrop.Self)) {
            double retainedRatio = ic2.core.util.ConfigUtil.getDouble(ic2.core.init.MainConfig.get(), "balance/energyRetainedInStorageBlockDrops");
            if (retainedRatio > 0.0 && energyStored > 0.0) {
                double energy = energyStored * retainedRatio;
                editDropData(drop, nbt -> nbt.putDouble("energy", energy));
            }
        }
        return drop;
    }

    @Override
    public void onPlaced(net.minecraft.item.ItemStack stack, net.minecraft.entity.LivingEntity placer) {
        energyStored = Math.min(maxStorage, energyStored + dropData(stack).getDouble("energy"));
        markDirty();
    }

    @Override
    public void addInformation(net.minecraft.item.ItemStack stack, java.util.List<net.minecraft.text.Text> tooltip) {
        tooltip.add(net.minecraft.text.Text.translatable("item.tooltip.Output").append(" " + output + " ")
                .append(net.minecraft.text.Text.translatable("generic.text.EUt")).append(" ")
                .append(net.minecraft.text.Text.translatable("item.tooltip.Capacity")).append(" " + maxStorage + " ")
                .append(net.minecraft.text.Text.translatable("generic.text.EU")));
        tooltip.add(net.minecraft.text.Text.translatable("item.tooltip.Store").append(" " + (long) dropData(stack).getDouble("energy") + " ")
                .append(net.minecraft.text.Text.translatable("generic.text.EU")));
    }

    // For TeBlockRegistry compatibility
    public static Class<? extends TileEntityElectricBlock> getTeClassFor(int tier) {
        return null;
    }
}
