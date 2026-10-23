package ic2.core.block.machine.tileentity;

import ic2.api.recipe.ILiquidHeatExchangerManager;
import ic2.api.recipe.Recipes;
import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.block.TileEntityHeatSourceInventory;
import ic2.core.block.comp.Fluids;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.invslot.InvSlotConsumable;
import ic2.core.block.invslot.InvSlotConsumableItemStack;
import ic2.core.block.invslot.InvSlotConsumableLiquid;
import ic2.core.block.invslot.InvSlotConsumableLiquidByManager;
import ic2.core.block.invslot.InvSlotConsumableLiquidByTank;
import ic2.core.block.invslot.InvSlotOutput;
import ic2.core.block.invslot.InvSlotUpgrade;
import ic2.core.init.IC2Blocks;
import ic2.core.init.IC2ItemVariants;
import ic2.core.ref.TeBlock;
import ic2.core.util.LiquidUtil;
import java.util.EnumSet;
import java.util.Set;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

/**
 * Fabric port of TileEntityLiquidHeatExchanger: cools hot fluid (hot coolant, lava) into its cold
 * form at the rates of {@link Recipes#liquidCooldownManager} and gives the heat out of its face,
 * 10 HU a tick for every heat conductor in its ten slots. 2000 mB tanks on both sides.
 */
public class TileEntityLiquidHeatExchanger extends TileEntityHeatSourceInventory implements IUpgradableBlock {
    public final Fluids.InternalFluidTank inputTank;
    public final Fluids.InternalFluidTank outputTank;
    public final InvSlotConsumable heatexchangerslots;
    public final InvSlotOutput hotoutputSlot;
    public final InvSlotOutput cooloutputSlot;
    public final InvSlotConsumableLiquid hotfluidinputSlot;
    public final InvSlotConsumableLiquid coolfluidinputSlot;
    public final InvSlotUpgrade upgradeSlot;

    public TileEntityLiquidHeatExchanger(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.LIQUID_HEAT_EXCHANGER_BE : type, pos, state);
        this.inputTank = fluids().addTankInsert("inputTank", 2000, Fluids.fluidPredicate(Recipes.liquidCooldownManager));
        this.outputTank = fluids().addTankExtract("outputTank", 2000);
        Item conductor = IC2ItemVariants.get("crafting", "heat_conductor");
        this.heatexchangerslots = new InvSlotConsumableItemStack(this, "heatExchanger", 10, new ItemStack(conductor == null ? Items.AIR : conductor));
        this.heatexchangerslots.setStackSizeLimit(1);
        this.hotoutputSlot = new InvSlotOutput(this, "hotOutputSlot", 1);
        this.cooloutputSlot = new InvSlotOutput(this, "outputSlot", 1);
        this.hotfluidinputSlot = new InvSlotConsumableLiquidByManager(this, "hotFluidInput", InvSlot.Access.I, 1, InvSlot.InvSide.TOP,
                InvSlotConsumableLiquid.OpType.Drain, Recipes.liquidCooldownManager);
        this.coolfluidinputSlot = new InvSlotConsumableLiquidByTank(this, "coolFluidOutput", InvSlot.Access.I, 1, InvSlot.InvSide.BOTTOM,
                InvSlotConsumableLiquid.OpType.Fill, outputTank);
        this.upgradeSlot = new InvSlotUpgrade(this, "upgrade", 3);
    }

    public TileEntityLiquidHeatExchanger(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    @Override
    public TeBlock getTeBlock() {
        return TeBlock.liquid_heat_exchanger;
    }

    @Override
    public void updateEntityServer() {
        super.updateEntityServer();
        hotfluidinputSlot.processIntoTank(inputTank, hotoutputSlot);
        coolfluidinputSlot.processFromTank(outputTank, cooloutputSlot);
        setActive(heatBuffer > 0);
        upgradeSlot.tick();
    }

    @Override
    public int getMaxHeatEmittedPerTick() {
        int count = 0;
        for (int i = 0; i < heatexchangerslots.size(); i++) {
            if (!heatexchangerslots.isEmpty(i)) {
                count += 10;
            }
        }
        return count;
    }

    @Override
    protected int fillHeatBuffer(int bufferspace) {
        if (bufferspace <= 0) {
            return 0;
        }
        int amountHot = (int) LiquidUtil.toMb(inputTank.getFluidAmount());
        int outputFree = (int) LiquidUtil.toMb(outputTank.getSpace());
        if (outputFree == 0 || amountHot == 0 || Recipes.liquidCooldownManager == null) {
            return 0;
        }

        Fluid input = inputTank.getFluid().getFluid();
        if (!Recipes.liquidCooldownManager.acceptsFluid(input)) {
            return 0;
        }
        ILiquidHeatExchangerManager.HeatExchangeProperty hep = Recipes.liquidCooldownManager.getHeatExchangeProperty(input);
        Fluid fluidOutput = hep.outputFluid;
        int huPerMb = hep.huPerMB;
        if (fluidOutput == null || !outputTank.isEmpty() && outputTank.getFluid().getFluid() != fluidOutput) {
            return 0;
        }

        // 1.12.2 drained twenty times the free space when the output was the limit, losing the rest
        int mb = Math.min(bufferspace / huPerMb, Math.min(amountHot, outputFree));
        if (mb <= 0) {
            return 0;
        }
        inputTank.drainInternal(LiquidUtil.mb(mb), true);
        outputTank.fillInternal(FluidVariant.of(fluidOutput), LiquidUtil.mb(mb), true);
        return mb * huPerMb;
    }

    public Fluids.InternalFluidTank getInputTank() {
        return inputTank;
    }

    public Fluids.InternalFluidTank getOutputTank() {
        return outputTank;
    }

    @Override
    public Set<UpgradableProperty> getUpgradableProperties() {
        return EnumSet.of(UpgradableProperty.ItemConsuming, UpgradableProperty.ItemProducing,
                UpgradableProperty.FluidConsuming, UpgradableProperty.FluidProducing);
    }

    @Override
    public double getEnergy() {
        return 40.0;
    }

    @Override
    public boolean useEnergy(double amount) {
        return true;
    }
}
