package ic2.core.block.machine.tileentity;

import ic2.core.block.TileEntityInventory;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.machine.WeightedPriority;
import ic2.core.init.IC2Blocks;
import ic2.core.util.AdjacentInventories;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Fabric port of TileEntityWeightedItemDistributor: items put into its nine slot buffer go to the
 * sides of its priority list in order. Every tick it offers each buffer stack to the first side
 * with an inventory, then the next, and stops at the first side that emptied the buffer. Sides
 * not in the list get nothing, and the side it faces can never be in it.
 */
public class TileEntityWeightedItemDistributor extends TileEntityInventory {
    protected final WeightedPriority priority = new WeightedPriority();
    public final InvSlot buffer;

    public TileEntityWeightedItemDistributor(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.WEIGHTED_ITEM_DISTRIBUTOR_BE : type, pos, state);
        this.buffer = new InvSlot(this, "buffer", InvSlot.Access.I, 9);
    }

    public TileEntityWeightedItemDistributor(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    public List<Direction> getPriority() {
        return priority.get();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        updateConnectivity();
    }

    @Override
    public void setFacing(Direction facing) {
        super.setFacing(facing);
        updateConnectivity();
    }

    /** The side it faces drops out of the list. */
    protected void updateConnectivity() {
        if (world != null && !world.isClient && priority != null) {
            if (priority.remove(getFacing())) {
                markDirty();
            }
        }
    }

    @Override
    public void tick() {
        if (world == null || world.isClient || priority.get().isEmpty() || buffer.isEmpty()) {
            return;
        }

        boolean hasChanged = false;
        for (Direction facing : priority.get()) {
            AdjacentInventories.AdjacentInv inv = AdjacentInventories.get(this, facing);
            if (inv == null) {
                continue;
            }

            boolean empty = true;
            for (int index = 0; index < buffer.size(); index++) {
                if (buffer.isEmpty(index)) {
                    continue;
                }
                ItemStack stack = buffer.get(index);
                int amount = AdjacentInventories.put(inv, stack, true);
                if (amount > 0) {
                    amount = AdjacentInventories.put(inv, stack, false);
                    stack = stack.copyWithCount(stack.getCount() - amount);
                    buffer.put(index, stack);
                    hasChanged = true;
                    empty &= stack.isEmpty();
                }
            }

            if (hasChanged && empty) {
                break;
            }
        }

        if (hasChanged) {
            markDirty();
        }
    }

    @Override
    public void onGuiEvent(PlayerEntity player, String event) {
        if (priority.onGuiEvent(event, getFacing())) {
            markDirty();
        }
    }

    @Override
    public boolean getGuiCondition(String name) {
        Boolean value = priority.getGuiCondition(name, getFacing());
        return value != null ? value : super.getGuiCondition(name);
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        priority.write(nbt);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        priority.read(nbt);
    }
}
