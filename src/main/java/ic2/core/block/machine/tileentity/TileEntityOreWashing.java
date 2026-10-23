package ic2.core.block.machine.tileentity;

import ic2.api.recipe.IRecipeInput;
import ic2.api.recipe.MachineRecipeResult;
import ic2.api.recipe.Recipes;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.block.comp.Fluids;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.invslot.InvSlotConsumableLiquid;
import ic2.core.block.invslot.InvSlotConsumableLiquidByTank;
import ic2.core.block.invslot.InvSlotOutput;
import ic2.core.init.IC2Blocks;
import ic2.core.recipe.BasicMachineRecipeManager;
import ic2.core.util.LiquidUtil;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;

/**
 * The ore washing plant: crushed ore and water in, purified ore and by-products out.
 *
 * <p>Each recipe names how much water it takes ({@code @fluid}, stored as {@code amount}); the
 * machine holds 8000 mB, filled through pipes or from water cells and buckets in its fluid slot,
 * whose empty containers go to the cell slot. A recipe waits until the tank has its water.
 */
public class TileEntityOreWashing extends TileEntityStandardMachine<IRecipeInput, Collection<ItemStack>, ItemStack> {
    public static final int TANK_CAPACITY = 8000;

    public final Fluids.InternalFluidTank fluidTank;
    public final InvSlotConsumableLiquidByTank fluidSlot;
    public final InvSlotOutput cellSlot;

    public TileEntityOreWashing(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 16, 500, 3, 1);
        this.fluidTank = fluids().addTankInsert("fluid", TANK_CAPACITY, Fluids.fluidPredicate(LiquidUtil.getFluid("water")));
        this.fluidSlot = new InvSlotConsumableLiquidByTank(this, "fluid", InvSlot.Access.I, 1,
                InvSlot.InvSide.ANY, InvSlotConsumableLiquid.OpType.Drain, fluidTank);
        this.cellSlot = new InvSlotOutput(this, "cell", 1);
    }

    public TileEntityOreWashing(BlockPos pos, BlockState state) {
        this(IC2Blocks.ORE_WASHING_BE, pos, state);
    }

    public static void init() {
        if (Recipes.oreWashing == null) Recipes.oreWashing = new BasicMachineRecipeManager();
    }

    @Override
    protected ic2.api.recipe.IBasicMachineRecipeManager getRecipeManager() {
        if (Recipes.oreWashing == null) init();
        return Recipes.oreWashing;
    }

    /** How much water a recipe takes. */
    static int waterOf(MachineRecipeResult<IRecipeInput, Collection<ItemStack>, ItemStack> result) {
        NbtCompound meta = result.getRecipe().getMetaData();
        return meta == null ? 0 : meta.getInt("amount");
    }

    @Override
    protected MachineRecipeResult<IRecipeInput, Collection<ItemStack>, ItemStack> getOutput() {
        MachineRecipeResult<IRecipeInput, Collection<ItemStack>, ItemStack> ret = super.getOutput();
        // a recipe without metadata names no water, which 1.12.2 refused as well
        if (ret != null && (ret.getRecipe().getMetaData() == null || LiquidUtil.mb(waterOf(ret)) > fluidTank.getFluidAmount())) {
            return null;
        }
        return ret;
    }

    @Override
    protected void operate(MachineRecipeResult<IRecipeInput, Collection<ItemStack>, ItemStack> result) {
        if (result == null) {
            return;
        }
        super.operate(result);
        fluidTank.drainInternal(LiquidUtil.mb(waterOf(result)), true);
    }

    @Override
    public void tick() {
        if (world != null && !world.isClient && fluidTank.getFluidAmount() < fluidTank.getCapacity() && gainFluid()) {
            markDirty();
        }
        super.tick();
    }

    /** Empties a water container from the fluid slot into the tank. */
    public boolean gainFluid() {
        return fluidSlot.processIntoTank(fluidTank, cellSlot);
    }

    @Override
    public Set<UpgradableProperty> getUpgradableProperties() {
        return EnumSet.of(UpgradableProperty.Processing, UpgradableProperty.Transformer, UpgradableProperty.EnergyStorage,
                UpgradableProperty.ItemConsuming, UpgradableProperty.ItemProducing, UpgradableProperty.FluidConsuming);
    }
}
