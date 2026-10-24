package ic2.core.block.machine.tileentity;

import ic2.api.energy.tile.IEnergySink;
import ic2.core.block.TileEntityInventory;
import ic2.core.ref.TeBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Fabric port of TileEntityElectricMachine - a machine with an internal energy buffer.
 *
 * <p>Like the original it builds on the inventory base, so subclasses declare their slot groups
 * and get the guidef-driven GUI; the Forge energy component is replaced by implementing
 * {@link IEnergySink} directly against the ported energy net.
 */
public abstract class TileEntityElectricMachine extends TileEntityInventory implements IEnergySink {

    /** The buffer; upgrades change it, {@link #defaultEnergyStorage} is what the machine has without them. */
    protected int maxEnergy;
    protected final int defaultEnergyStorage;

    @Override
    public double getGuiValue(String name) {
        return "energy".equals(name) ? (maxEnergy <= 0 ? 0.0 : energyStored / maxEnergy) : super.getGuiValue(name);
    }

    @Override
    public double getGuiAmount(String name) {
        return "energy".equals(name) ? energyStored : super.getGuiAmount(name);
    }

    @Override
    public double getGuiCapacity(String name) {
        return "energy".equals(name) ? maxEnergy : super.getGuiCapacity(name);
    }

    /** The voltage tier the machine takes; transformer upgrades raise it. */
    protected int tier;
    protected final int defaultTier;
    protected double energyStored = 0;

    public TileEntityElectricMachine(BlockEntityType<?> type, BlockPos pos, BlockState state, TeBlock teBlock, int maxEnergy, int tier) {
        super(type, pos, state, teBlock);
        this.maxEnergy = this.defaultEnergyStorage = maxEnergy;
        this.tier = this.defaultTier = tier;
    }

    public TileEntityElectricMachine(BlockEntityType<?> type, BlockPos pos, BlockState state, int maxEnergy, int tier) {
        super(type, pos, state);
        this.maxEnergy = this.defaultEnergyStorage = maxEnergy;
        this.tier = this.defaultTier = tier;
    }

    // IEnergySink
    @Override public boolean acceptsEnergyFrom(ic2.api.energy.tile.IEnergyEmitter emitter, Direction side) { return true; }
    @Override public double getDemandedEnergy() { return Math.max(0, maxEnergy - energyStored); }

    @Override public double getEnergyForDisplay() { return energyStored; }
    @Override public double getEnergyCapacityForDisplay() { return maxEnergy; }
    @Override public double injectEnergy(Direction directionFrom, double amount, double voltage) {
        double toAdd = Math.min(amount, getDemandedEnergy());
        energyStored += toAdd;
        markDirty();
        return amount - toAdd;
    }
    @Override public int getSinkTier() { return tier; }

    public boolean useEnergy(double amount) {
        if (energyStored >= amount) {
            energyStored -= amount;
            markDirty();
            return true;
        }
        return false;
    }

    public double getEnergy() { return energyStored; }

    /** Moves energy out of a battery in the slot into the buffer, as the energy component did. */
    protected void drainDischargeSlot(ic2.core.block.invslot.InvSlotDischarge slot) {
        double space = maxEnergy - energyStored;
        if (space <= 0 || slot.isEmpty()) {
            return;
        }

        net.minecraft.item.ItemStack stack = slot.get();
        if (ic2.api.item.ElectricItem.manager == null || !(stack.getItem() instanceof ic2.api.item.IElectricItem)) {
            return;
        }

        double drained = ic2.api.item.ElectricItem.manager.discharge(stack, space, tier, false, true, false);
        if (drained > 0) {
            energyStored += drained;
            markDirty();
        }
    }

    @Override public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putDouble("energyStored", energyStored);
    }

    @Override public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        if (nbt.contains("energyStored")) energyStored = nbt.getDouble("energyStored");
    }

    /** Sets the voltage tier the machine takes, as the transformer upgrades decide it. */
    public void setTier(int tier) {
        this.tier = tier;
    }

    /** Sets the buffer size; energy above it is kept until used, as Energy.setCapacity did. */
    public void setCapacity(int capacity) {
        this.maxEnergy = capacity;
    }
}
