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
import ic2.core.crop.TileEntityCrop;
import ic2.core.init.IC2Blocks;
import ic2.core.ref.FluidName;
import ic2.core.ref.TeBlock;
import ic2.core.util.LiquidUtil;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

/**
 * Fabric port of TileEntityCropmatron - tends the crops around it.
 *
 * <p>Every ten ticks it looks at one more block of the 9x3x9 area around itself, front to back
 * and bottom to top: a crop gets fertilizer, water and Weed-EX from its slots and tanks, bare
 * farmland gets watered. Each visit costs 1 EU, each thing it hands out 10 more.
 */
public class TileEntityCropmatron extends TileEntityElectricMachine implements IUpgradableBlock {
    public final InvSlotConsumable fertilizerSlot;
    public final InvSlotConsumableLiquidByTank wasserinputSlot;
    public final InvSlotConsumableLiquidByTank exInputSlot;
    public final InvSlotOutput wasseroutputSlot;
    public final InvSlotOutput exOutputSlot;
    public final InvSlotUpgrade upgradeSlot;
    public final InvSlotDischarge dischargeSlot;

    protected final Fluids.InternalFluidTank waterTank;
    protected final Fluids.InternalFluidTank exTank;

    public int scanX = -4;
    public int scanY = -1;
    public int scanZ = -4;

    public TileEntityCropmatron(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.CROPMATRON_BE : type, pos, state, TeBlock.cropmatron, 10000, 1);

        this.waterTank = fluids().addTankInsert("waterTank", 2000, Fluids.fluidPredicate(LiquidUtil.getFluid("water")));
        this.exTank = fluids().addTankInsert("exTank", 2000, Fluids.fluidPredicate(FluidName.weed_ex.getInstance()));

        // the order is the one the 1.12.2 constructor created them in, which is the inventory order
        this.fertilizerSlot = new InvSlotConsumableItemStack(this, "fertilizer", 7, new ItemStack(TileEntityCrop.fertilizer()));
        this.wasserinputSlot = new InvSlotConsumableLiquidByTank(this, "wasserinputSlot", InvSlot.Access.I, 1,
                InvSlot.InvSide.TOP, InvSlotConsumableLiquid.OpType.Drain, waterTank);
        this.exInputSlot = new InvSlotConsumableLiquidByTank(this, "exInputSlot", InvSlot.Access.I, 1,
                InvSlot.InvSide.TOP, InvSlotConsumableLiquid.OpType.Drain, exTank);
        this.wasseroutputSlot = new InvSlotOutput(this, "wasseroutputSlot", 1);
        this.exOutputSlot = new InvSlotOutput(this, "exOutputSlot", 1);
        this.upgradeSlot = new InvSlotUpgrade(this, "upgrade", 4);
        this.dischargeSlot = new InvSlotDischarge(this, "discharge", 1);
    }

    public TileEntityCropmatron(BlockPos pos, BlockState state) {
        this(IC2Blocks.CROPMATRON_BE, pos, state);
    }

    public TileEntityCropmatron() {
        this(IC2Blocks.CROPMATRON_BE, BlockPos.ORIGIN, null);
    }

    @Override
    public void tick() {
        if (world == null || world.isClient) {
            return;
        }

        drainDischargeSlot(dischargeSlot);
        upgradeSlot.tickNoMark();
        if (wasserinputSlot.processIntoTank(waterTank, wasseroutputSlot) | exInputSlot.processIntoTank(exTank, exOutputSlot)) {
            markDirty();
        }
        fertilizerSlot.organize();

        if (world.getTime() % 10L == 0L && getEnergy() >= 31.0) {
            scan();
        }
    }

    public void scan() {
        scanX++;
        if (scanX > 4) {
            scanX = -4;
            scanZ++;
            if (scanZ > 4) {
                scanZ = -4;
                scanY++;
                if (scanY > 1) {
                    scanY = -1;
                }
            }
        }

        useEnergy(1.0);
        BlockPos scan = pos.add(scanX, scanY, scanZ);

        if (world.getBlockEntity(scan) instanceof TileEntityCrop crop) {
            boolean changed = false;

            if (!fertilizerSlot.isEmpty() && crop.applyFertilizer(false)) {
                useEnergy(10.0);
                fertilizerSlot.consume(1);
                changed = true;
            }

            if (waterTank.getFluidAmount() > 0) {
                int taken = crop.applyHydration(mbIn(waterTank));
                if (taken > 0) {
                    waterTank.drainInternal(LiquidUtil.mb(taken), true);
                    useEnergy(10.0);
                    changed = true;
                }
            }

            if (exTank.getFluidAmount() > 0) {
                int taken = crop.applyWeedEx(mbIn(exTank), false);
                if (taken > 0) {
                    exTank.drainInternal(LiquidUtil.mb(taken), true);
                    useEnergy(10.0);
                    changed = true;
                }
            }

            if (changed) {
                crop.markDirty();
                markDirty();
            }
        } else if (mbIn(waterTank) > 0 && tryHydrateFarmland(scan)) {
            useEnergy(10.0);
            markDirty();
        }
    }

    /** The whole millibuckets in a tank, the unit the crops take water and Weed-EX in. */
    private static int mbIn(Fluids.InternalFluidTank tank) {
        return (int) LiquidUtil.toMb(tank.getFluidAmount());
    }

    /** Wets farmland that has dried out, one millibucket per level of moisture missing. */
    private boolean tryHydrateFarmland(BlockPos target) {
        BlockState state = world.getBlockState(target);
        if (!state.isOf(Blocks.FARMLAND)) {
            return false;
        }

        int hydration = state.get(FarmlandBlock.MOISTURE);
        if (hydration >= FarmlandBlock.MAX_MOISTURE) {
            return false;
        }

        int drainAmount = Math.min(mbIn(waterTank), FarmlandBlock.MAX_MOISTURE - hydration);
        waterTank.drainInternal(LiquidUtil.mb(drainAmount), true);
        world.setBlockState(target, state.with(FarmlandBlock.MOISTURE, hydration + drainAmount), 2);
        return true;
    }

    @Override
    public Set<UpgradableProperty> getUpgradableProperties() {
        return EnumSet.of(UpgradableProperty.Transformer, UpgradableProperty.EnergyStorage,
                UpgradableProperty.ItemConsuming, UpgradableProperty.FluidConsuming);
    }

    public Fluids.InternalFluidTank getWaterTank() {
        return waterTank;
    }

    public Fluids.InternalFluidTank getExTank() {
        return exTank;
    }
}
