package ic2.core.block.reactor.tileentity;

import ic2.api.reactor.IReactorChamber;
import ic2.core.block.TileEntityBlock;
import ic2.core.block.comp.FluidReactorLookup;
import ic2.core.ref.TeBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

/**
 * Fabric port of TileEntityReactorVessel - a plain wall block of a fluid reactor's hull.
 *
 * <p>It does nothing itself; it only has to answer that it is a wall, which is what the reactor
 * counts when it decides whether it is walled in.
 */
public class TileEntityReactorVessel extends TileEntityBlock implements IReactorChamber {
    protected final FluidReactorLookup lookup = new FluidReactorLookup(this);

    public TileEntityReactorVessel(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, TeBlock.reactor_vessel);
    }

    protected TileEntityReactorVessel(BlockEntityType<?> type, BlockPos pos, BlockState state, TeBlock teBlock) {
        super(type, pos, state, teBlock);
    }

    public TileEntityReactorVessel(BlockPos pos, BlockState state) { this(null, pos, state); }
    public TileEntityReactorVessel() { this(null, BlockPos.ORIGIN, null); }

    @Override
    public TileEntityNuclearReactorElectric getReactorInstance() {
        return lookup.getReactor();
    }

    @Override
    public boolean isWall() {
        return true;
    }
}
