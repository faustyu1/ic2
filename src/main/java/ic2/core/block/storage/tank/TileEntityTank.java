package ic2.core.block.storage.tank;

import ic2.core.block.TileEntityInventory;
import ic2.core.block.comp.Fluids;
import ic2.core.util.FluidContainers;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.FluidStack;

/**
 * Fabric port of TileEntityTank - a block that holds one fluid and nothing else.
 *
 * <p>The tank is a named tank called {@code contents}, which is what {@code guidef/*_tank.xml}
 * shows as a fluid slot. Buckets are handled on right click; the fluid pipes and the item side of
 * the transfer follow with the rest of the fluid port.
 */
public class TileEntityTank extends TileEntityInventory {
    /** One bucket in millibuckets, the unit IC2 counts fluids in. */
    public static final int BUCKET = 1000;

    protected final Fluids.InternalFluidTank contents;

    public TileEntityTank(BlockEntityType<?> type, BlockPos pos, BlockState state, int buckets) {
        super(type, pos, state);
        this.contents = fluids().addTank("contents", buckets * BUCKET);
    }

    public TileEntityTank(int buckets) {
        this(null, BlockPos.ORIGIN, null, buckets);
    }

    /** The tank keeps its fluid in the dropped item, mined or wrenched. */
    @Override
    protected ItemStack adjustDrop(ItemStack drop, boolean wrench) {
        drop = super.adjustDrop(drop, wrench);
        if (drop != null && contents.getFluidAmount() > 0) {
            editDropData(drop, contents::writeToNbt);
        }
        return drop;
    }

    @Override
    public void onPlaced(ItemStack stack, net.minecraft.entity.LivingEntity placer) {
        contents.readFromNbt(dropData(stack));
    }

    @Override
    public void addInformation(ItemStack stack, java.util.List<net.minecraft.text.Text> tooltip) {
        tooltip.add(net.minecraft.text.Text.literal("Capacity: " + contents.getCapacity() + " mB"));
        net.minecraft.nbt.NbtCompound nbt = dropData(stack);
        String fluid = nbt.getString("fluid");
        if (fluid.isEmpty()) {
            tooltip.add(net.minecraft.text.Text.literal("Empty"));
        } else {
            tooltip.add(net.minecraft.text.Text.literal(fluid));
            tooltip.add(net.minecraft.text.Text.literal("Amount: " + nbt.getInt("amount") + " mB"));
        }
    }

    public Fluids.InternalFluidTank getTank() {
        return contents;
    }

    /**
     * Pours the container in the player's hand into the tank, or fills an empty one from it.
     * Buckets and IC2 cells both work.
     *
     * @return true when the container was used, which keeps the GUI from opening
     */
    @Override
    public boolean onActivated(PlayerEntity player, Hand hand) {
        if (world == null || world.isClient) {
            return false;
        }

        ItemStack held = player.getStackInHand(hand);
        if (!FluidContainers.isFluidContainer(held)) {
            return false;
        }

        ic2.core.item.IItemFluidTank itemTank = FluidContainers.tankOf(held);
        if (itemTank != null) {
            return exchangeWithItemTank(itemTank, held);
        }

        FluidStack content = FluidContainers.contentOf(held);
        if (content != null) {
            if (contents.fillInternal(content, false) < content.amount) {
                return false;
            }

            contents.fillInternal(content, true);
            give(player, hand, held, FluidContainers.emptied(held));
            markDirty();
            return true;
        }

        FluidStack stored = contents.getFluid();
        int capacity = FluidContainers.capacityOf(held);
        if (stored == null || stored.amount < capacity) {
            return false;
        }

        ItemStack filled = FluidContainers.filled(held, stored.getFluid());
        if (filled.isEmpty()) {
            return false;
        }

        contents.drainInternal(capacity, true);
        give(player, hand, held, filled);
        markDirty();
        return true;
    }

    /**
     * An item with a tank of its own pours into the tank what fits; an item the tank cannot take
     * from - empty, or of another fluid than the tank's - is topped up from it instead.
     */
    private boolean exchangeWithItemTank(ic2.core.item.IItemFluidTank itemTank, ItemStack held) {
        FluidStack content = itemTank.getFluid(held);
        if (content != null) {
            int amount = contents.fillInternal(content, false);
            if (amount > 0) {
                contents.fillInternal(itemTank.drain(held, amount, true), true);
                markDirty();
                return true;
            }
        }

        FluidStack stored = contents.getFluid();
        int amount = stored == null ? 0 : itemTank.fill(held, stored, false);
        if (amount <= 0) {
            return false;
        }
        itemTank.fill(held, contents.drainInternal(amount, true), true);
        markDirty();
        return true;
    }

    /** Swaps the used container for what it turned into, dropping it when the hand is full. */
    private static void give(PlayerEntity player, Hand hand, ItemStack used, ItemStack result) {
        if (player.isCreative()) {
            return;
        }

        if (used.getCount() == 1) {
            player.setStackInHand(hand, result);
            return;
        }

        used.decrement(1);
        if (!result.isEmpty() && !player.getInventory().insertStack(result)) {
            player.dropItem(result, false);
        }
    }
}
