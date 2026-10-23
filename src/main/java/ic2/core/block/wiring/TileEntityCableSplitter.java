package ic2.core.block.wiring;

import ic2.core.init.IC2Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

/**
 * Fabric port of TileEntityCableSplitter - the EU splitter cable.
 *
 * <p>A redstone signal cuts it: it leaves the energy net while powered and joins it again once the
 * signal is gone, which splits a cable line in two. It is lit while it connects.
 */
public class TileEntityCableSplitter extends TileEntityCable {

    public TileEntityCableSplitter(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.CABLE_BE : type, pos, state, CableType.splitter, 0);
    }

    public TileEntityCableSplitter(BlockPos pos, BlockState state) {
        this(IC2Blocks.CABLE_BE, pos, state);
    }

    @Override
    public void tick() {
        if (world == null || world.isClient) {
            return;
        }

        boolean powered = world.isReceivingRedstonePower(pos);
        if (powered && isAddedToEnet()) {
            onUnloaded();
        } else if (!powered && !isAddedToEnet()) {
            onLoad();
        }

        setActive(isAddedToEnet());
    }

    public static Class<? extends TileEntityCable> delegate() {
        return TileEntityCableSplitter.class;
    }
}
