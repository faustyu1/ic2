package ic2.core.block.heatgenerator.tileentity;

import ic2.api.recipe.IFluidHeatManager;
import ic2.api.recipe.Recipes;
import ic2.core.block.TileEntityHeatSourceInventory;
import ic2.core.block.comp.Fluids;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.invslot.InvSlotConsumableLiquid;
import ic2.core.block.invslot.InvSlotConsumableLiquidByManager;
import ic2.core.block.invslot.InvSlotOutput;
import ic2.core.init.IC2Blocks;
import ic2.core.init.MainConfig;
import ic2.core.recipe.FluidHeatManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

/**
 * Fabric port of {@code TileEntityFluidHeatGenerator}: the liquid fuel firebox. It burns what the
 * semi-fluid generator burns, at the rates of {@link Recipes#fluidHeatGenerator} - biogas gives
 * 32 HU a tick for 10 mB a second. Containers go into its slot and are emptied into the 10 000 mB
 * tank.
 */
public class TileEntityFluidHeatGenerator extends TileEntityHeatSourceInventory {
    public final InvSlotConsumableLiquid fluidSlot;
    public final InvSlotOutput outputSlot;
    protected final Fluids.InternalFluidTank fluidTank;
    private short ticker = 0;
    protected int burnAmount = 0;
    protected int production = 0;
    private boolean newActive;

    public TileEntityFluidHeatGenerator(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.FLUID_HEAT_BE : type, pos, state);
        this.fluidSlot = new InvSlotConsumableLiquidByManager(this, "fluidSlot", InvSlot.Access.I, 1, InvSlot.InvSide.ANY,
                InvSlotConsumableLiquid.OpType.Drain, Recipes.fluidHeatGenerator);
        this.outputSlot = new InvSlotOutput(this, "output", 1);
        this.fluidTank = fluids().addTankInsert("fluidTank", 10000,
                fluid -> Recipes.semiFluidGenerator != null && Recipes.semiFluidGenerator.acceptsFluid(fluid));
    }

    public TileEntityFluidHeatGenerator(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    public static void init() {
        Recipes.fluidHeatGenerator = new FluidHeatManager();
        addFuelIfEnabled("semiFluidOil", "oil", 10, 32.0F);
        addFuelIfEnabled("semiFluidFuel", "fuel", 5, 768.0F);
        addFuelIfEnabled("semiFluidBiomass", "biomass", 20, 16.0F);
        addFuelIfEnabled("semiFluidBioethanol", "bio.ethanol", 10, 32.0F);
        addFuelIfEnabled("semiFluidBiogas", "ic2biogas", 10, 32.0F);
    }

    private static void addFuelIfEnabled(String key, String fluidName, int amount, float heat) {
        if (MainConfig.get().getDouble("balance/energy/generator/" + key, 1.0) > 0.0) {
            addFuel(fluidName, amount, Math.round(heat * (float) MainConfig.get().getDouble("balance/energy/heatgenerator/" + key, 1.0)));
        }
    }

    public static void addFuel(String fluidName, int amount, int heat) {
        Recipes.fluidHeatGenerator.addFluid(fluidName, amount, heat);
    }

    @Override
    public void updateEntityServer() {
        super.updateEntityServer();
        if (gainFuel()) {
            markDirty();
        }
        setActive(newActive);
    }

    public boolean isConverting() {
        return getTankAmount() > 0 && heatBuffer < getMaxHeatEmittedPerTick();
    }

    @Override
    protected int fillHeatBuffer(int maxAmount) {
        if (!isConverting()) {
            newActive = false;
            return 0;
        }

        if (ticker >= 19) {
            fluidTank.drainInternal(burnAmount, true);
            ticker = 0;
        } else {
            ticker++;
        }
        newActive = true;
        return production;
    }

    @Override
    public int getMaxHeatEmittedPerTick() {
        return calcHeatProduction();
    }

    protected int calcHeatProduction() {
        IFluidHeatManager.BurnProperty property = burnProperty();
        return production = property == null ? 0 : property.heat;
    }

    protected void calcBurnAmount() {
        IFluidHeatManager.BurnProperty property = burnProperty();
        burnAmount = property == null ? 0 : property.amount;
    }

    private IFluidHeatManager.BurnProperty burnProperty() {
        Fluid fluid = getFluidFromTank();
        return fluid == null || Recipes.fluidHeatGenerator == null ? null : Recipes.fluidHeatGenerator.getBurnProperty(fluid);
    }

    public Fluids.InternalFluidTank getFluidTank() {
        return fluidTank;
    }

    public Fluid getFluidFromTank() {
        FluidStack content = fluidTank.getFluid();
        return content == null ? null : content.getFluid();
    }

    public int getTankAmount() {
        return fluidTank.getFluidAmount();
    }

    protected boolean gainFuel() {
        if (fluidTank.getFluid() != null) {
            calcHeatProduction();
            calcBurnAmount();
        }
        return fluidSlot.processIntoTank(fluidTank, outputSlot);
    }
}
