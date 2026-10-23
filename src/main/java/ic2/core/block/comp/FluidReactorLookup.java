package ic2.core.block.comp;

import ic2.core.block.TileEntityBlock;
import ic2.core.block.reactor.tileentity.TileEntityNuclearReactorElectric;
import ic2.core.util.WorldSearchUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Fabric port of the FluidReactorLookup component: finds the fluid cooled reactor a casing block
 * belongs to.
 *
 * <p>The answer is cached for the rest of the tick, and the cached reactor is only re-searched
 * once it stops being a fluid reactor or moves out of the 5x5x5 the casing may sit in, so a wall
 * of ports does not search the world once per block per tick.
 */
public class FluidReactorLookup {
    private static final int RANGE = 2;

    private final TileEntityBlock parent;
    private TileEntityNuclearReactorElectric reactor;
    private long lastReactorUpdate = Long.MIN_VALUE;

    public FluidReactorLookup(TileEntityBlock parent) {
        this.parent = parent;
    }

    public TileEntityNuclearReactorElectric getReactor() {
        World world = parent.getWorld();
        if (world == null) {
            return null;
        }

        long time = world.getTime();
        if (time != lastReactorUpdate) {
            updateReactor();
            lastReactorUpdate = time;
        } else if (reactor != null && (reactor.isRemoved() || !reactor.isFluidCooled())) {
            reactor = null;
        }

        return reactor;
    }

    private void updateReactor() {
        World world = parent.getWorld();
        BlockPos pos = parent.getPos();

        if (reactor != null
                && !reactor.isRemoved()
                && reactor.isFluidCooled()
                && reactor.getWorld() == world
                && world.getBlockEntity(reactor.getPos()) == reactor) {
            BlockPos reactorPos = reactor.getPos();
            int dx = Math.abs(pos.getX() - reactorPos.getX());
            int dy = Math.abs(pos.getY() - reactorPos.getY());
            int dz = Math.abs(pos.getZ() - reactorPos.getZ());
            // still a wall block of that reactor: inside the 5x5x5 and on its surface
            if (dx <= RANGE && dy <= RANGE && dz <= RANGE && (dx == RANGE || dy == RANGE || dz == RANGE)) {
                return;
            }
        }

        reactor = null;
        WorldSearchUtil.findTileEntities(world, pos, RANGE, te -> {
            if (te instanceof TileEntityNuclearReactorElectric candidate && candidate.isFluidCooled()) {
                reactor = candidate;
                return true;
            }
            return false;
        });
    }
}
