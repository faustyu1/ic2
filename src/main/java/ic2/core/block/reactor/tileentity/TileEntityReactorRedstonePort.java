package ic2.core.block.reactor.tileentity;

import ic2.core.init.IC2Blocks;
import ic2.core.ref.TeBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

/**
 * Fabric port of TileEntityReactorRedstonePort - a wall block of a fluid reactor's hull that
 * passes a redstone signal on to the reactor.
 *
 * <p>The reactor of a fluid reactor sits in the middle of its 5x5x5 hull, out of reach of a
 * lever; powering a redstone port of the hull switches it on as if the reactor itself were
 * powered. 1.12.2 linked the port's redstone component to the reactor's, the port's reactor asks
 * its ports instead ({@link TileEntityNuclearReactorElectric#hasRedstoneInput()}).
 */
public class TileEntityReactorRedstonePort extends TileEntityReactorVessel {

    public TileEntityReactorRedstonePort(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.REACTOR_REDSTONE_PORT_BE : type, pos, state, TeBlock.reactor_redstone_port);
    }

    public TileEntityReactorRedstonePort(BlockPos pos, BlockState state) {
        this(IC2Blocks.REACTOR_REDSTONE_PORT_BE, pos, state);
    }

    /** Whether the port is powered, and so the reactor of its hull. */
    public boolean hasRedstone() {
        return world != null && world.isReceivingRedstonePower(pos);
    }
}
