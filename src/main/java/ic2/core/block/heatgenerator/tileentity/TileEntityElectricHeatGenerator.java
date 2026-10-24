package ic2.core.block.heatgenerator.tileentity;

import ic2.api.energy.tile.IEnergyEmitter;
import ic2.api.energy.tile.IEnergySink;
import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;
import ic2.core.block.TileEntityHeatSourceInventory;
import ic2.core.block.invslot.InvSlotConsumable;
import ic2.core.block.invslot.InvSlotConsumableItemStack;
import ic2.core.block.invslot.InvSlotDischarge;
import ic2.core.init.IC2Blocks;
import ic2.core.init.IC2ItemVariants;
import ic2.core.init.MainConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Fabric port of {@code TileEntityElectricHeatGenerator}: the electric heater. It turns EU into
 * HU one to one (times {@code balance/energy/heatgenerator/electric}), 10 HU a tick for every coil
 * in its ten slots. It holds 10 000 EU and takes tier 4.
 */
public class TileEntityElectricHeatGenerator extends TileEntityHeatSourceInventory implements IEnergySink {
    public static final double CAPACITY = 10000.0;
    public static final int TIER = 4;

    public final InvSlotConsumable coilSlot;
    public final InvSlotDischarge dischargeSlot;
    private double energy;
    private boolean newActive;

    public TileEntityElectricHeatGenerator(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.ELECTRIC_HEAT_BE : type, pos, state);
        Item coil = IC2ItemVariants.get("crafting", "coil");
        this.coilSlot = new InvSlotConsumableItemStack(this, "CoilSlot", 10, new ItemStack(coil == null ? Items.AIR : coil));
        this.coilSlot.setStackSizeLimit(1);
        this.dischargeSlot = new InvSlotDischarge(this, "discharge", TIER);
    }

    public TileEntityElectricHeatGenerator(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    public static double outputMultiplier() {
        return MainConfig.get().getDouble("balance/energy/heatgenerator/electric", 1.0);
    }

    @Override
    public void updateEntityServer() {
        dischargeBattery();
        super.updateEntityServer();
        setActive(newActive);
    }

    private void dischargeBattery() {
        double space = CAPACITY - energy;
        ItemStack stack = dischargeSlot.get();
        if (space <= 0 || stack.isEmpty() || ElectricItem.manager == null || !(stack.getItem() instanceof IElectricItem)) {
            return;
        }
        double drained = ElectricItem.manager.discharge(stack, space, TIER, false, true, false);
        if (drained > 0) {
            energy += drained;
            markDirty();
        }
    }

    @Override
    protected int fillHeatBuffer(int maxAmount) {
        int amount = Math.min(maxAmount, (int) (energy / outputMultiplier()));
        if (amount > 0) {
            energy -= amount / outputMultiplier();
            newActive = true;
            markDirty();
        } else {
            newActive = false;
        }
        return amount;
    }

    @Override
    public int getMaxHeatEmittedPerTick() {
        int counter = 0;
        for (int i = 0; i < coilSlot.size(); i++) {
            if (!coilSlot.isEmpty(i)) {
                counter++;
            }
        }
        return counter * 10;
    }

    public double getEnergy() {
        return energy;
    }

    @Override public double getEnergyForDisplay() { return energy; }
    @Override public double getEnergyCapacityForDisplay() { return CAPACITY; }

    public float getChargeLevel() {
        return (float) Math.min(1.0, energy / CAPACITY);
    }

    @Override
    public double getGuiValue(String name) {
        return "energy".equals(name) ? getChargeLevel() : super.getGuiValue(name);
    }

    @Override
    public double getGuiAmount(String name) {
        return "energy".equals(name) ? energy : super.getGuiAmount(name);
    }

    @Override
    public double getGuiCapacity(String name) {
        return "energy".equals(name) ? CAPACITY : super.getGuiCapacity(name);
    }

    // ---- IEnergySink ----

    @Override
    public boolean acceptsEnergyFrom(IEnergyEmitter emitter, Direction side) {
        return true;
    }

    @Override
    public double getDemandedEnergy() {
        return Math.max(0.0, CAPACITY - energy);
    }

    @Override
    public double injectEnergy(Direction directionFrom, double amount, double voltage) {
        double accepted = Math.min(amount, getDemandedEnergy());
        energy += accepted;
        markDirty();
        return amount - accepted;
    }

    @Override
    public int getSinkTier() {
        return TIER;
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        nbt.putDouble("energy", energy);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        energy = nbt.getDouble("energy");
    }
}
