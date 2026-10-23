package ic2.core.block.invslot;

import ic2.core.block.IInventorySlotHolder;
import ic2.core.block.comp.Fluids;
import ic2.core.item.IItemFluidTank;
import ic2.core.util.FluidContainers;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;

/**
 * Fabric port of InvSlotConsumableLiquid - the slot a machine's fluid containers go into.
 *
 * <p>Depending on its {@link OpType} it either pours a full container into the machine's tank or
 * fills an empty one from it; what is left of the container goes to the output slot. A transfer
 * only happens when the whole container fits, which is what kept the original from losing fluid.
 */
public class InvSlotConsumableLiquid extends InvSlotConsumable {
    /** Which way the fluid moves between the container and the tank. */
    public enum OpType {
        /** The container is emptied into the machine. */
        Drain,
        /** The container is filled from the machine. */
        Fill,
        Both
    }

    public final OpType opType;

    public InvSlotConsumableLiquid(IInventorySlotHolder<?> base, String name, Access access, int count,
                                   InvSide preferredSide, OpType opType) {
        super(base, name, access, count, preferredSide);
        this.opType = opType;
    }

    @Override
    public boolean accepts(ItemStack stack) {
        if (!FluidContainers.isFluidContainer(stack)) {
            return false;
        }

        IItemFluidTank itemTank = FluidContainers.tankOf(stack);
        if (itemTank != null) {
            ResourceAmount<FluidVariant> content = itemTank.getFluid(stack);
            return switch (opType) {
                case Fill -> itemTank.getAmount(stack) < itemTank.getCapacity(stack);
                case Drain -> content != null && acceptsLiquid(content.resource().getFluid());
                case Both -> true;
            };
        }

        ResourceAmount<FluidVariant> content = FluidContainers.contentOf(stack);
        if (opType == OpType.Fill) {
            return content == null;
        }
        return content == null || acceptsLiquid(content.resource().getFluid());
    }

    /** Whether the machine's tank takes that fluid; the tank bound slot narrows this down. */
    protected boolean acceptsLiquid(Fluid fluid) {
        return true;
    }

    /**
     * Empties the container in this slot into the tank.
     *
     * @return true when something moved, i.e. the machine has to be saved
     */
    public boolean processIntoTank(Fluids.InternalFluidTank tank, InvSlotOutput outputSlot) {
        if (isEmpty() || opType == OpType.Fill) {
            return false;
        }

        ItemStack stack = get();
        IItemFluidTank itemTank = FluidContainers.tankOf(stack);
        if (itemTank != null) {
            return drainItemTank(itemTank, stack, tank, outputSlot);
        }

        ResourceAmount<FluidVariant> content = FluidContainers.contentOf(stack);
        if (content == null || !acceptsLiquid(content.resource().getFluid())) {
            return false;
        }

        // the whole container has to fit, otherwise the rest would be lost
        if (tank.fillInternal(content.resource(), content.amount(), false) < content.amount()) {
            return false;
        }

        ItemStack emptied = FluidContainers.emptied(stack);
        if (!emptied.isEmpty() && !outputSlot.canAdd(emptied)) {
            return false;
        }

        tank.fillInternal(content.resource(), content.amount(), true);
        consume(1);
        if (!emptied.isEmpty()) {
            outputSlot.add(emptied);
        }
        return true;
    }

    /**
     * Fills the empty container in this slot from the tank.
     *
     * @return true when something moved
     */
    public boolean processFromTank(Fluids.InternalFluidTank tank, InvSlotOutput outputSlot) {
        if (isEmpty() || opType == OpType.Drain) {
            return false;
        }

        ItemStack stack = get();
        IItemFluidTank itemTank = FluidContainers.tankOf(stack);
        if (itemTank != null) {
            return fillItemTank(itemTank, stack, tank, outputSlot);
        }

        if (tank.getFluidAmount() <= 0) {
            return false;
        }

        if (FluidContainers.contentOf(stack) != null) {
            return false;
        }

        FluidVariant stored = tank.getFluid();
        long capacity = FluidContainers.capacityOf(stack);
        if (capacity <= 0 || tank.getFluidAmount() < capacity) {
            return false;
        }

        ItemStack filled = FluidContainers.filled(stack, stored.getFluid());
        if (filled.isEmpty() || !outputSlot.canAdd(filled)) {
            return false;
        }

        tank.drainInternal(capacity, true);
        consume(1);
        outputSlot.add(filled);
        return true;
    }

    /**
     * Empties as much of an item's own tank into the machine as fits; once the item is dry it moves
     * on to the output slot, so the next one can come in.
     */
    private boolean drainItemTank(IItemFluidTank itemTank, ItemStack stack, Fluids.InternalFluidTank tank, InvSlotOutput outputSlot) {
        boolean moved = false;
        ResourceAmount<FluidVariant> content = itemTank.getFluid(stack);
        if (content != null && acceptsLiquid(content.resource().getFluid())) {
            long amount = tank.fillInternal(content.resource(), content.amount(), false);
            if (amount > 0) {
                itemTank.drain(stack, amount, true);
                tank.fillInternal(content.resource(), amount, true);
                onChanged();
                moved = true;
            }
        }

        if (itemTank.isEmpty(stack)) {
            moved |= passOn(stack, outputSlot);
        }
        return moved;
    }

    /** Tops an item's own tank up from the machine; once it is full it moves on to the output slot. */
    private boolean fillItemTank(IItemFluidTank itemTank, ItemStack stack, Fluids.InternalFluidTank tank, InvSlotOutput outputSlot) {
        boolean moved = false;
        FluidVariant stored = tank.getFluid();
        if (!tank.isEmpty()) {
            long amount = itemTank.fill(stack, stored, tank.getFluidAmount(), false);
            if (amount > 0) {
                itemTank.fill(stack, stored, tank.drainInternal(amount, true), true);
                onChanged();
                moved = true;
            }
        }

        if (itemTank.getAmount(stack) >= itemTank.getCapacity(stack)) {
            moved |= passOn(stack, outputSlot);
        }
        return moved;
    }

    private boolean passOn(ItemStack stack, InvSlotOutput outputSlot) {
        if (!outputSlot.canAdd(stack)) {
            return false;
        }
        outputSlot.add(stack.copy());
        clear();
        return true;
    }
}
