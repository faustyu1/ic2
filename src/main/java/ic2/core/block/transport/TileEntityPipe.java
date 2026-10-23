package ic2.core.block.transport;

import ic2.api.transport.IPipe;
import ic2.core.block.TileEntityBlock;
import ic2.core.block.transport.cover.CoverProperty;
import ic2.core.block.transport.cover.Covers;
import ic2.core.block.transport.cover.ICoverHolder;
import ic2.core.block.transport.cover.ICoverItem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * A pipe: a thin tube that is joined to its neighbours only on the sides a wrench connected, and
 * that takes attachments (covers) on its sides, which the crowbar takes off again.
 *
 * <p>The connections and covers are a bit per side each, kept here and mirrored into the block
 * state of {@link BlockPipe}, which is what the model and the collision box are drawn from - 1.12.2
 * synced the two bytes to the client and built the model from them.
 */
public abstract class TileEntityPipe extends TileEntityBlock implements IPipe, ICoverHolder {
    protected byte connectivity;
    protected byte covers;
    protected final Covers coversComponent = new Covers(this);

    protected TileEntityPipe(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        // a freshly placed pipe already carries the connection its block chose on placement
        if (state != null && state.getBlock() instanceof BlockPipe) {
            this.connectivity = (byte) BlockPipe.getConnectivity(state);
        }
    }

    @Override
    public BlockEntity getTile() {
        return this;
    }

    @Override
    public boolean isConnected(Direction facing) {
        return (connectivity & 1 << facing.ordinal()) != 0;
    }

    public int getConnectivity() {
        return connectivity & 0xFF;
    }

    public int getCoverMask() {
        return covers & 0xFF;
    }

    public boolean hasCover(Direction side) {
        return coversComponent.hasCover(side);
    }

    @Override
    public Set<CoverProperty> getCoverProperties() {
        return Collections.emptySet();
    }

    @Override
    public boolean canPlaceCover(World world, BlockPos pos, Direction side, ItemStack stack) {
        return stack.getItem() instanceof ICoverItem item
                && item.isSuitableFor(stack, getCoverProperties())
                && (covers & 1 << side.ordinal()) == 0;
    }

    @Override
    public void placeCover(World world, BlockPos pos, Direction side, ItemStack stack) {
        coversComponent.addCover(side, stack);
        covers = (byte) (covers ^ 1 << side.ordinal());
        markDirty();
        updatePipeState();
    }

    @Override
    public boolean canRemoveCover(World world, BlockPos pos, Direction side) {
        return (covers & 1 << side.ordinal()) != 0;
    }

    @Override
    public void removeCover(World world, BlockPos pos, Direction side) {
        ItemStack ret = coversComponent.removeCover(side);
        covers = (byte) (covers ^ 1 << side.ordinal());
        markDirty();
        updatePipeState();
        if (!ret.isEmpty()) {
            Block.dropStack(world, pos, ret.copyWithCount(1));
        }
    }

    /** Covers tick first, then the pipe, as the 1.12.2 component and tile entity did. */
    @Override
    public void tick() {
        if (world == null || world.isClient) {
            return;
        }
        coversComponent.onWorldTick();
        updateEntityServer();
    }

    /** Writes the connection and cover bits into the block state, which redraws the pipe. */
    protected void updatePipeState() {
        if (world == null || world.isClient) {
            return;
        }
        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof BlockPipe)) {
            return;
        }
        BlockState updated = BlockPipe.withPipeState(state, getConnectivity(), getCoverMask());
        if (updated != state) {
            world.setBlockState(pos, updated, Block.NOTIFY_ALL);
        }
    }

    /**
     * The pipe itself and its covers, as getAuxDrops added them in 1.12.2. The 1.12.2 pipes were
     * a separate item, so TeBlock.fluid_pipe has none; each pipe block has its own item now.
     */
    @Override
    public List<ItemStack> getSelfDrops(boolean wrench) {
        List<ItemStack> ret = new ArrayList<>();
        ret.add(getPickStack());
        for (Direction side : Direction.values()) {
            if (coversComponent.hasCover(side)) {
                ret.add(coversComponent.removeCover(side));
            }
        }
        return ret;
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        connectivity = nbt.getByte("connectivity");
        covers = nbt.getByte("covers");
        coversComponent.readFromNbt(nbt, registryLookup);
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putByte("connectivity", connectivity);
        nbt.putByte("covers", covers);
        coversComponent.writeToNbt(nbt, registryLookup);
    }
}
