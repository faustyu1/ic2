package ic2.core.block.machine.tileentity;

import ic2.api.energy.tile.IHeatSource;
import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.block.TileEntityInventory;
import ic2.core.block.comp.Fluids;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.invslot.InvSlotConsumableLiquid;
import ic2.core.block.invslot.InvSlotConsumableLiquidByTank;
import ic2.core.block.invslot.InvSlotOutput;
import ic2.core.block.invslot.InvSlotUpgrade;
import ic2.core.crop.TileEntityCrop;
import ic2.core.init.IC2Blocks;
import ic2.core.init.MainConfig;
import ic2.core.ref.FluidName;
import ic2.core.ref.TeBlock;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

/**
 * Fabric port of {@code ic2.core.block.machine.tileentity.TileEntityFermenter}: turns biomass into
 * biogas with heat.
 *
 * <p>It draws up to 100 HU a tick from the heat source it faces. Every 4000 HU (the config's
 * {@code balance/fermenter/hU_per_run}) it turns 20 mB of biomass into 400 mB of biogas, and every
 * 500 mB of biomass it has used up leaves one fertilizer. It needs no EU. Cells and other fluid
 * containers fill its biomass tank from the left and take biogas out on the right.
 *
 * <p>1.12.2 kept the one fermentation recipe in a {@code FermenterRecipeManager}; there was never
 * a second one, so the port keeps it as config numbers here.
 */
public class TileEntityFermenter extends TileEntityInventory implements IUpgradableBlock {
    private final Fluids.InternalFluidTank inputTank;
    private final Fluids.InternalFluidTank outputTank;
    public final InvSlotOutput fluidInputCellOutSlot;
    public final InvSlotOutput fluidOutputCellOutSlot;
    public final InvSlotOutput fertiliserSlot;
    public final InvSlotUpgrade upgradeSlot;
    public final InvSlotConsumableLiquidByTank fluidOutputCellInSlot;
    public final InvSlotConsumableLiquidByTank fluidInputCellInSlot;

    private int heatBuffer = 0;
    public int progress = 0;

    public TileEntityFermenter(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.FERMENTER_BE : type, pos, state, TeBlock.fermenter);
        this.outputTank = fluids().addTankExtract("output", 2000);
        this.inputTank = fluids().addTankInsert("input", 10000, fluid -> fluid.equals(FluidName.biomass.getInstance()));
        // the slot order is the 1.12.2 field order, which is the inventory order
        this.fluidInputCellOutSlot = new InvSlotOutput(this, "biomassOutput", 1);
        this.fluidOutputCellOutSlot = new InvSlotOutput(this, "biogassOutput", 1);
        this.fertiliserSlot = new InvSlotOutput(this, "output", 1);
        this.upgradeSlot = new InvSlotUpgrade(this, "upgrade", 2);
        this.fluidOutputCellInSlot = new InvSlotConsumableLiquidByTank(this, "biogasInput", InvSlot.Access.I, 1,
                InvSlot.InvSide.BOTTOM, InvSlotConsumableLiquid.OpType.Fill, outputTank);
        this.fluidInputCellInSlot = new InvSlotConsumableLiquidByTank(this, "biomassInput", InvSlot.Access.I, 1,
                InvSlot.InvSide.TOP, InvSlotConsumableLiquid.OpType.Drain, inputTank);
    }

    public TileEntityFermenter(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    private static int config(String key, int fallback) {
        return MainConfig.get().getInt("balance/fermenter/" + key, fallback);
    }

    /** Biomass one run takes. */
    public static int biomassPerRun() {
        return config("need_amount_biomass_per_run", 20);
    }

    /** Biogas one run gives. */
    public static int biogasPerRun() {
        return config("output_amount_biogas_per_run", 400);
    }

    /** Heat one run needs. */
    public static int heatPerRun() {
        return config("hU_per_run", 4000);
    }

    /** Biomass used up per fertilizer. */
    public static int biomassPerFertilizer() {
        return config("biomass_per_fertilizier", 500);
    }

    @Override
    public void tick() {
        if (world == null || world.isClient) {
            return;
        }

        boolean changed = fluidInputCellInSlot.processIntoTank(inputTank, fluidInputCellOutSlot);
        changed |= fluidOutputCellInSlot.processFromTank(outputTank, fluidOutputCellOutSlot);
        boolean active = work();
        if (getActive() != active) {
            setActive(active);
        }
        upgradeSlot.tickNoMark();
        if (changed || active) {
            markDirty();
        }
    }

    private boolean work() {
        if (progress >= biomassPerFertilizer()) {
            if (!fertiliserSlot.canAdd(new ItemStack(TileEntityCrop.fertilizer()))) {
                return false;
            }
            fertiliserSlot.add(new ItemStack(TileEntityCrop.fertilizer()));
            progress = 0;
        }

        Direction dir = getFacing();
        FluidStack input = inputTank.getFluid();
        if (!(world.getBlockEntity(pos.offset(dir)) instanceof IHeatSource heatSource) || input == null
                || !input.getFluid().equals(FluidName.biomass.getInstance())) {
            return false;
        }

        int inputAmount = biomassPerRun();
        int outputAmount = biogasPerRun();
        if (inputTank.getFluidAmount() < inputAmount || outputAmount > outputTank.getCapacity() - outputTank.getFluidAmount()) {
            return false;
        }

        heatBuffer += heatSource.drawHeat(dir.getOpposite(), 100, false);
        if (heatBuffer >= heatPerRun()) {
            heatBuffer -= heatPerRun();
            inputTank.drainInternal(inputAmount, true);
            outputTank.fillInternal(new FluidStack(FluidName.biogas.getInstance(), outputAmount), true);
            progress += inputAmount;
        }
        return true;
    }

    public Fluids.InternalFluidTank getInputTank() {
        return inputTank;
    }

    public Fluids.InternalFluidTank getOutputTank() {
        return outputTank;
    }

    public int getHeatBuffer() {
        return heatBuffer;
    }

    @Override
    public double getGuiValue(String name) {
        return switch (name) {
            case "heat" -> heatBuffer == 0 ? 0.0 : (double) heatBuffer / heatPerRun();
            case "progress" -> progress == 0 ? 0.0 : (double) progress / biomassPerFertilizer();
            default -> super.getGuiValue(name);
        };
    }

    /** The fermenter runs on heat, not EU; to the upgrades it always has power. */
    @Override
    public double getEnergy() {
        return 40.0;
    }

    @Override
    public boolean useEnergy(double amount) {
        return true;
    }

    @Override
    public Set<UpgradableProperty> getUpgradableProperties() {
        return EnumSet.of(UpgradableProperty.ItemConsuming, UpgradableProperty.ItemProducing,
                UpgradableProperty.FluidConsuming, UpgradableProperty.FluidProducing);
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        nbt.putInt("progress", progress);
        nbt.putInt("heatBuffer", heatBuffer);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        progress = nbt.getInt("progress");
        heatBuffer = nbt.getInt("heatBuffer");
    }

    /** Biomass is the only fluid it takes. */
    public static boolean acceptsFluid(Fluid fluid) {
        return FluidName.biomass.getInstance().equals(fluid);
    }
}
