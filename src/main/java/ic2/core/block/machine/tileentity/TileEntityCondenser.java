package ic2.core.block.machine.tileentity;

import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.block.comp.Fluids;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.invslot.InvSlotConsumable;
import ic2.core.block.invslot.InvSlotConsumableItemStack;
import ic2.core.block.invslot.InvSlotConsumableLiquid;
import ic2.core.block.invslot.InvSlotConsumableLiquidByTank;
import ic2.core.block.invslot.InvSlotDischarge;
import ic2.core.block.invslot.InvSlotOutput;
import ic2.core.block.invslot.InvSlotUpgrade;
import ic2.core.init.IC2Blocks;
import ic2.core.ref.FluidName;
import ic2.core.ref.TeBlock;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.FluidStack;

/**
 * Fabric port of TileEntityCondenser - turns steam back into distilled water.
 *
 * <p>Steam goes into a 100 000 mB tank; each tick 100 mB of it condenses on its own, plus 100 mB
 * for every heat vent in its four vent slots, each vent costing 2 EU a tick. Every 10 000 mB of
 * steam make 100 mB of distilled water in its 1000 mB output tank, which containers or pipes take
 * out. 100 000 EU buffer, tier 3, raised by transformer upgrades.
 */
public class TileEntityCondenser extends TileEntityElectricMachine implements IUpgradableBlock, InvSlotUpgrade.Listener {
    private static final short PASSIVE_COOLING = 100;
    private static final short COOLING_PER_VENT = 100;
    public static final short VENT_EU_COST = 2;
    public static final int MAX_PROGRESS = 10000;

    public int progress;
    private final Fluids.InternalFluidTank inputTank;
    private final Fluids.InternalFluidTank outputTank;
    public final InvSlotConsumableLiquidByTank waterInputSlot;
    public final InvSlotOutput waterOutputSlot;
    public final InvSlotConsumable ventSlots;
    public final InvSlotUpgrade upgradeSlot;
    public final InvSlotDischarge dischargeSlot;

    public TileEntityCondenser(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.CONDENSER_BE : type, pos, state, TeBlock.condenser, 100000, 3);
        this.inputTank = fluids().addTankInsert("inputTank", 100000,
                Fluids.fluidPredicate(FluidName.steam.getInstance(), FluidName.superheated_steam.getInstance()));
        this.outputTank = fluids().addTankExtract("outputTank", 1000);
        this.waterInputSlot = new InvSlotConsumableLiquidByTank(this, "waterInputSlot", InvSlot.Access.I, 1,
                InvSlot.InvSide.BOTTOM, InvSlotConsumableLiquid.OpType.Fill, outputTank);
        this.waterOutputSlot = new InvSlotOutput(this, "waterOutputSlot", 1);
        this.ventSlots = new InvSlotConsumableItemStack(this, "ventSlots", 4,
                new ItemStack(Registries.ITEM.get(Identifier.of("ic2", "heat_vent"))));
        this.ventSlots.setStackSizeLimit(1);
        this.upgradeSlot = new InvSlotUpgrade(this, "upgradeSlot", 1);
        this.dischargeSlot = new InvSlotDischarge(this, "discharge", 3);
    }

    public TileEntityCondenser(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    public int getVents() {
        int vents = 0;
        for (int slot = 0; slot < ventSlots.size(); slot++) {
            if (!ventSlots.isEmpty(slot)) {
                vents++;
            }
        }
        return vents;
    }

    @Override
    public void onUpgradesChanged() {
        setTier(upgradeSlot.getTier(defaultTier));
    }

    @Override
    public void tick() {
        if (world == null || world.isClient) {
            return;
        }

        drainDischargeSlot(dischargeSlot);
        waterInputSlot.processFromTank(outputTank, waterOutputSlot);
        setActive(inputTank.getFluidAmount() > 0);
        work();
        if (upgradeSlot.tickNoMark()) {
            markDirty();
        }
    }

    private void work() {
        if (outputTank.getCapacity() - outputTank.getFluidAmount() < 100) {
            return;
        }

        if (progress >= MAX_PROGRESS) {
            outputTank.fillInternal(new FluidStack(FluidName.distilled_water.getInstance(), 100), true);
            progress -= MAX_PROGRESS;
        }

        if (inputTank.getFluidAmount() > 0) {
            int vents = getVents();
            int drain = PASSIVE_COOLING + vents * COOLING_PER_VENT;
            if (useEnergy(vents * VENT_EU_COST)) {
                FluidStack drained = inputTank.drainInternal(drain, true);
                progress += drained == null ? 0 : drained.amount;
                markDirty();
            }
        }
    }

    public Fluids.InternalFluidTank getInputTank() {
        return inputTank;
    }

    public Fluids.InternalFluidTank getOutputTank() {
        return outputTank;
    }

    @Override
    public double getGuiValue(String name) {
        return "progress".equals(name) ? (double) progress / MAX_PROGRESS : super.getGuiValue(name);
    }

    @Override
    public Set<UpgradableProperty> getUpgradableProperties() {
        return EnumSet.of(UpgradableProperty.ItemConsuming, UpgradableProperty.ItemProducing,
                UpgradableProperty.FluidConsuming, UpgradableProperty.FluidProducing, UpgradableProperty.Transformer);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        progress = nbt.getInt("progress");
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        nbt.putInt("progress", progress);
    }
}
