package ic2.core.block.generator.tileentity;

import ic2.api.energy.tile.IEnergySource;
import ic2.core.block.TileEntityInventory;
import ic2.core.block.invslot.InvSlotCharge;
import ic2.core.ref.TeBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Fabric port of TileEntityBaseGenerator: a block that turns some fuel into energy and offers it
 * to the energy net.
 *
 * <p>It is an inventory block because every generator has the {@code charge} slot its guidef
 * shows - the original had it on the base class as well and let the energy component push the
 * buffer into the item. Here the tick does that directly, since the components are not ported.
 */
public abstract class TileEntityBaseGenerator extends TileEntityInventory implements IEnergySource {

    /** Not final: the semifluid generator changes it with every fuel it burns. */
    protected double production;
    protected final int tier;
    protected final int maxStorage;
    protected double energyStored = 0;
    protected int fuel = 0;

    /** Battery slot; every generator guidef places one. */
    public final InvSlotCharge chargeSlot;

    /**
     * Generators with a fluctuating output (sun, wind, water) only change their look every 256
     * ticks, to whatever they mostly did in that time; the counter starts at a random point so a
     * field of solar panels does not flicker in step.
     */
    private int ticksSinceLastActiveUpdate = ic2.core.IC2.random.nextInt(256);
    private int activityMeter = 0;

    public TileEntityBaseGenerator(BlockEntityType<?> type, BlockPos pos, BlockState state, TeBlock teBlock, double production, int tier, int maxStorage) {
        super(type, pos, state, teBlock);
        this.production = production;
        this.tier = tier;
        this.maxStorage = maxStorage;
        // the battery slot is LV in every generator, whatever the generator's own tier
        this.chargeSlot = new InvSlotCharge(this, 1);
    }

    public TileEntityBaseGenerator(BlockEntityType<?> type, BlockPos pos, BlockState state, double production, int tier, int maxStorage) {
        super(type, pos, state);
        this.production = production;
        this.tier = tier;
        this.maxStorage = maxStorage;
        // the battery slot is LV in every generator, whatever the generator's own tier
        this.chargeSlot = new InvSlotCharge(this, 1);
    }

    // IEnergySource
    @Override public boolean emitsEnergyTo(ic2.api.energy.tile.IEnergyAcceptor receiver, Direction side) { return true; }
    @Override public double getOfferedEnergy() { return Math.min(energyStored, getMaxEnergyOutput()); }

    @Override public double getEnergyForDisplay() { return energyStored; }
    @Override public double getEnergyCapacityForDisplay() { return maxStorage; }
    @Override public void drawEnergy(double amount) { energyStored = Math.max(0, energyStored - amount); markDirty(); }
    @Override public int getSourceTier() { return tier; }

    public double getMaxEnergyOutput() {
        // Tier power: use EnergyNet's power table if available, else tier*8?
        try {
            return ic2.api.energy.EnergyNet.instance.getPowerFromTier(tier);
        } catch (Exception e) {
            return Math.pow(4, tier) * 8;
        }
    }

    public boolean gainEnergy() {
        if (isConverting()) {
            energyStored = Math.min(maxStorage, energyStored + production);
            fuel--;
            markDirty();
            return true;
        }
        return false;
    }

    /** EU in the buffer. */
    public double getEnergy() {
        return energyStored;
    }

    /** Fuel left, in ticks of production. */
    public int getFuel() {
        return fuel;
    }

    /** Room left in the buffer; the generator only burns fuel it can store the energy of. */
    public double getFreeEnergy() {
        return Math.max(0.0, maxStorage - energyStored);
    }

    public boolean isConverting() { return !needsFuel() && getFreeEnergy() >= production; }
    public boolean needsFuel() { return fuel <= 0 && getFreeEnergy() >= production; }
    public boolean gainFuel() { return false; }
    protected boolean delayActiveUpdate() { return false; }
    public String getOperationSoundFile() { return null; }

    public void tick() {
        if (world == null || world.isClient) return;
        if (needsFuel() && gainFuel()) markDirty();
        boolean newActive = gainEnergy();
        chargeFromBuffer();

        if (!delayActiveUpdate()) {
            setActive(newActive);
        } else {
            if (ticksSinceLastActiveUpdate % 256 == 0) {
                setActive(activityMeter > 0);
                activityMeter = 0;
            }

            if (newActive) {
                activityMeter++;
            } else {
                activityMeter--;
            }

            ticksSinceLastActiveUpdate++;
        }
    }

    /** Hands the buffer to a battery in the charge slot, which is what the energy component did. */
    private void chargeFromBuffer() {
        if (energyStored <= 0 || chargeSlot.isEmpty()) {
            return;
        }

        double charged = chargeSlot.charge(energyStored);
        if (charged > 0) {
            energyStored -= charged;
            markDirty();
        }
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
        return "energy".equals(name) ? energyStored : super.getGuiAmount(name);
    }

    @Override
    public double getGuiCapacity(String name) {
        return "energy".equals(name) ? maxStorage : super.getGuiCapacity(name);
    }

    @Override public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putInt("fuel", fuel);
        nbt.putDouble("energyStored", energyStored);
    }

    @Override public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        if (nbt.contains("fuel")) fuel = nbt.getInt("fuel");
        if (nbt.contains("energyStored")) energyStored = nbt.getDouble("energyStored");
    }
}
