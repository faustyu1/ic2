package ic2.core.block.machine.tileentity;

import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.block.comp.Fluids;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.invslot.InvSlotConsumableLiquid;
import ic2.core.block.invslot.InvSlotDischarge;
import ic2.core.block.invslot.InvSlotOutput;
import ic2.core.block.invslot.InvSlotUpgrade;
import ic2.core.init.IC2Blocks;
import ic2.core.ref.TeBlock;
import ic2.core.util.LiquidUtil;
import ic2.core.util.PumpUtil;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraftforge.fluids.FluidStack;

/**
 * Fabric port of TileEntityPump.
 *
 * <p>Every 20 ticks (1 EU each) it pumps a source block from in front of it - following the flow
 * back to its source with {@link PumpUtil} - into its 8000 mB tank, or drains up to 1000 mB from a
 * tank block there. Next to a miner whose pipe ended in a fluid, it pumps from the bottom of the
 * pipe instead. Containers in its slot are filled from the tank. Upgrades as for any machine.
 */
public class TileEntityPump extends TileEntityElectricMachine implements IUpgradableBlock, InvSlotUpgrade.Listener {
    public int energyConsume;
    public int operationsPerTick;
    public final int defaultEnergyConsume = 1;
    public final int defaultOperationLength = 20;
    private TileEntityMiner miner;
    public final InvSlotConsumableLiquid containerSlot;
    public final InvSlotOutput outputSlot;
    public final InvSlotUpgrade upgradeSlot;
    public final InvSlotDischarge dischargeSlot;
    protected final Fluids.InternalFluidTank fluidTank;
    public short progress;
    public int operationLength;
    public float guiProgress;

    public TileEntityPump(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.PUMP_BE : type, pos, state, TeBlock.pump, 20, 1);
        this.containerSlot = new InvSlotConsumableLiquid(this, "input", InvSlot.Access.I, 1, InvSlot.InvSide.TOP,
                InvSlotConsumableLiquid.OpType.Fill);
        this.outputSlot = new InvSlotOutput(this, "output", 1, InvSlot.InvSide.SIDE);
        this.upgradeSlot = new InvSlotUpgrade(this, "upgrade", 4);
        this.dischargeSlot = new InvSlotDischarge(this, "discharge", 1);
        this.energyConsume = defaultEnergyConsume;
        this.operationLength = defaultOperationLength;
        this.fluidTank = fluids().addTankExtract("fluid", 8000);
        onUpgradesChanged();
    }

    public TileEntityPump(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    @Override
    public void tick() {
        if (world == null || world.isClient) {
            return;
        }

        drainDischargeSlot(dischargeSlot);
        boolean needsInvUpdate = false;
        if (canoperate() && getEnergy() >= energyConsume * operationLength) {
            if (progress < operationLength) {
                progress++;
                useEnergy(energyConsume);
            } else {
                progress = 0;
                operate(false);
            }
            setActive(true);
        } else {
            setActive(false);
        }

        needsInvUpdate |= containerSlot.processFromTank(fluidTank, outputSlot);
        needsInvUpdate |= upgradeSlot.tickNoMark();
        guiProgress = (float) progress / operationLength;
        if (needsInvUpdate) {
            markDirty();
        }
    }

    public boolean canoperate() {
        return operate(true);
    }

    public boolean operate(boolean sim) {
        if (miner == null || miner.isRemoved()) {
            miner = null;
            for (Direction dir : new Direction[] {Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST}) {
                if (world.getBlockEntity(pos.offset(dir)) instanceof TileEntityMiner found) {
                    miner = found;
                    break;
                }
            }
        }

        FluidStack liquid = null;
        if (miner != null) {
            if (miner.canProvideLiquid) {
                liquid = pump(miner.liquidPos, sim, miner);
            }
        } else {
            liquid = pump(pos.offset(getFacing()), sim, null);
        }

        if (liquid != null && fluidTank.fillInternal(liquid, false) > 0) {
            if (!sim) {
                fluidTank.fillInternal(liquid, true);
                markDirty();
            }
            return true;
        }
        return false;
    }

    public FluidStack pump(BlockPos startPos, boolean sim, TileEntityMiner miner) {
        int freeSpace = fluidTank.getCapacity() - fluidTank.getFluidAmount();
        if (miner == null && freeSpace > 0) {
            BlockEntity te = world.getBlockEntity(startPos);
            Direction side = getFacing().getOpposite();
            if (te != null && LiquidUtil.isFluidTile(te, side)) {
                return LiquidUtil.drainTile(te, side, Math.min(freeSpace, 1000), sim);
            }
        }

        if (freeSpace >= 1000) {
            BlockPos cPos = miner != null && miner.canProvideLiquid ? miner.liquidPos : PumpUtil.searchFluidSource(world, startPos);
            if (cPos != null) {
                return LiquidUtil.drainBlock(world, cPos, sim);
            }
        }
        return null;
    }

    @Override
    public void onUpgradesChanged() {
        double previousProgress = operationLength == 0 ? 0 : (double) progress / operationLength;
        operationsPerTick = upgradeSlot.getOperationsPerTick(defaultOperationLength);
        operationLength = upgradeSlot.getOperationLength(defaultOperationLength);
        energyConsume = upgradeSlot.getEnergyDemand(defaultEnergyConsume);
        setTier(upgradeSlot.getTier(defaultTier));
        setCapacity(upgradeSlot.getEnergyStorage(defaultEnergyStorage, defaultOperationLength, defaultEnergyConsume));
        progress = (short) Math.floor(previousProgress * operationLength + 0.1);
    }

    public Fluids.InternalFluidTank getFluidTank() {
        return fluidTank;
    }

    @Override
    public double getGuiValue(String name) {
        return "progress".equals(name) ? guiProgress : super.getGuiValue(name);
    }

    @Override
    public Set<UpgradableProperty> getUpgradableProperties() {
        return EnumSet.of(UpgradableProperty.Processing, UpgradableProperty.Transformer, UpgradableProperty.EnergyStorage,
                UpgradableProperty.ItemConsuming, UpgradableProperty.ItemProducing, UpgradableProperty.FluidProducing);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        progress = nbt.getShort("progress");
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        nbt.putShort("progress", progress);
    }
}
