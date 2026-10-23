package ic2.core.block.machine.tileentity;

import ic2.core.block.machine.WeightedPriority;
import ic2.core.init.IC2Blocks;
import ic2.core.util.LiquidUtil;
import java.util.Collections;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Fabric port of TileEntityWeightedFluidDistributor: takes fluid in through its face and fills
 * the sides of its priority list in order - the first one as far as it takes, then the next.
 * Sides not in the list get nothing.
 */
public class TileEntityWeightedFluidDistributor extends TileEntityFluidDistributor {
    protected final WeightedPriority priority = new WeightedPriority();

    public TileEntityWeightedFluidDistributor(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.WEIGHTED_FLUID_DISTRIBUTOR_BE : type, pos, state);
    }

    public TileEntityWeightedFluidDistributor(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    public List<Direction> getPriority() {
        return priority.get();
    }

    @Override
    protected void updateConnectivity() {
        if (fluidTank == null) {
            return;
        }
        if (priority != null) {
            priority.remove(getFacing());
        }
        fluidTank.setSides(Collections.singleton(getFacing()), Collections.emptySet());
    }

    @Override
    protected void moveFluid() {
        long tankAmount = fluidTank.getFluidAmount();
        for (Direction dir : priority.get()) {
            BlockEntity target = world.getBlockEntity(pos.offset(dir));
            Direction side = dir.getOpposite();
            if (LiquidUtil.isFluidTile(target, side)) {
                long amount = LiquidUtil.fillTile(target, side, fluidTank.getFluid(), tankAmount, false);
                if (amount > 0) {
                    tankAmount -= amount;
                    fluidTank.drainInternal(amount, true);
                    if (tankAmount <= 0) {
                        break;
                    }
                }
            }
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
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        priority.write(nbt);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        priority.read(nbt);
        super.readNbt(nbt, lookup);
    }
}
