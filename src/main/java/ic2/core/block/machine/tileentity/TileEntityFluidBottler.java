package ic2.core.block.machine.tileentity;

import ic2.api.recipe.IBasicMachineRecipeManager;
import ic2.api.recipe.MachineRecipe;
import ic2.api.recipe.MachineRecipeResult;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.block.comp.Fluids;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.invslot.InvSlotConsumableLiquid;
import ic2.core.block.invslot.InvSlotConsumableLiquidByTank;
import ic2.core.init.IC2Blocks;
import ic2.core.item.IItemFluidTank;
import ic2.core.ref.TeBlock;
import ic2.core.util.FluidContainers;
import java.util.EnumSet;
import java.util.Set;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

/**
 * Fabric port of TileEntityFluidBottler: empties the container in its top slot into its 8000 mB
 * tank, or, when there is nothing to empty, fills the container in its bottom slot from the tank,
 * one container per operation of 100 ticks at 2 EU a tick. Emptying wins when both are possible,
 * as in 1.12.2.
 *
 * <p>1.12.2 went through the {@code emptyFluidContainer}/{@code fillFluidContainer} recipe
 * managers of the API; the port works on the containers directly, as the canner does.
 */
public class TileEntityFluidBottler extends TileEntityStandardMachine<Void, Runnable, Void> {
    public final InvSlotConsumableLiquid drainInputSlot;
    public final InvSlotConsumableLiquid fillInputSlot;
    public final Fluids.InternalFluidTank fluidTank;

    public TileEntityFluidBottler(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.FLUID_BOTTLER_BE : type, pos, state, 2, 100, 1);
        this.fluidTank = fluids().addTank("fluidTank", 8000);
        this.drainInputSlot = new InvSlotConsumableLiquidByTank(this, "drainInput", InvSlot.Access.I, 1, InvSlot.InvSide.TOP,
                InvSlotConsumableLiquid.OpType.Drain, fluidTank);
        this.fillInputSlot = new InvSlotConsumableLiquidByTank(this, "fillInput", InvSlot.Access.I, 1, InvSlot.InvSide.BOTTOM,
                InvSlotConsumableLiquid.OpType.Fill, fluidTank);
    }

    public TileEntityFluidBottler(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    @Override
    public TeBlock getTeBlock() {
        return TeBlock.fluid_bottler;
    }

    @Override
    protected IBasicMachineRecipeManager getRecipeManager() {
        return null;
    }

    @Override
    protected MachineRecipeResult<Void, Runnable, Void> getOutput() {
        Runnable op = emptyContainer();
        if (op == null) {
            op = fillContainer();
        }
        return op == null ? null : new MachineRecipeResult<>(new MachineRecipe<>(null, op), null);
    }

    @Override
    protected void operate(MachineRecipeResult<Void, Runnable, Void> result) {
        if (result != null) {
            result.getOutput().run();
            markDirty();
        }
    }

    private Runnable emptyContainer() {
        ItemStack container = drainInputSlot.get();
        if (container.isEmpty()) {
            return null;
        }

        IItemFluidTank itemTank = FluidContainers.tankOf(container);
        if (itemTank != null) {
            ResourceAmount<FluidVariant> content = itemTank.getFluid(container);
            if (content == null || fluidTank.fill(content.resource(), content.amount(), false) < content.amount()
                    || !outputSlot.canAdd(container.copyWithCount(1))) {
                return null;
            }
            return () -> {
                ItemStack emptied = container.copyWithCount(1);
                itemTank.drain(emptied, content.amount(), true);
                fluidTank.fill(content.resource(), content.amount(), true);
                drainInputSlot.consume(1);
                outputSlot.add(emptied);
            };
        }

        ResourceAmount<FluidVariant> content = FluidContainers.contentOf(container);
        ItemStack emptied = FluidContainers.emptied(container);
        if (content == null || fluidTank.fill(content.resource(), content.amount(), false) < content.amount()
                || !emptied.isEmpty() && !outputSlot.canAdd(emptied)) {
            return null;
        }
        return () -> {
            fluidTank.fill(content.resource(), content.amount(), true);
            drainInputSlot.consume(1);
            if (!emptied.isEmpty()) {
                outputSlot.add(emptied);
            }
        };
    }

    private Runnable fillContainer() {
        ItemStack container = fillInputSlot.get();
        FluidVariant stored = fluidTank.getFluid();
        if (container.isEmpty() || fluidTank.isEmpty()) {
            return null;
        }

        IItemFluidTank itemTank = FluidContainers.tankOf(container);
        if (itemTank != null) {
            ItemStack filled = container.copyWithCount(1);
            long amount = itemTank.fill(filled, stored, fluidTank.getFluidAmount(), true);
            if (amount <= 0 || !outputSlot.canAdd(filled)) {
                return null;
            }
            return () -> {
                fluidTank.drainInternal(amount, true);
                fillInputSlot.consume(1);
                outputSlot.add(filled);
            };
        }

        if (FluidContainers.contentOf(container) != null) {
            return null;
        }
        long capacity = FluidContainers.capacityOf(container);
        ItemStack filled = FluidContainers.filled(container, stored.getFluid());
        if (capacity <= 0 || fluidTank.getFluidAmount() < capacity || filled.isEmpty() || !outputSlot.canAdd(filled)) {
            return null;
        }
        return () -> {
            fluidTank.drainInternal(capacity, true);
            fillInputSlot.consume(1);
            outputSlot.add(filled);
        };
    }

    @Override
    public Set<UpgradableProperty> getUpgradableProperties() {
        return EnumSet.of(UpgradableProperty.Processing, UpgradableProperty.Transformer, UpgradableProperty.EnergyStorage,
                UpgradableProperty.ItemConsuming, UpgradableProperty.ItemProducing,
                UpgradableProperty.FluidConsuming, UpgradableProperty.FluidProducing);
    }
}
