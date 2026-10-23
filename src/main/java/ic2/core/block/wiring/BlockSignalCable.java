package ic2.core.block.wiring;

import ic2.core.block.BlockTileEntity;
import ic2.core.ref.TeBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateManager;

/**
 * The detector and the splitter cable: the two cables with a redstone side to them.
 *
 * <p>Both light up - the detector while energy flows through it, the splitter while it is
 * connected - which 1.12.2 drew from the tile entity's active flag; here that flag is the
 * {@link BlockTileEntity#ACTIVE} property of the block state, so the model can show it. The
 * detector also gives off a redstone signal.
 */
public class BlockSignalCable extends BlockCable {

    public BlockSignalCable(Settings settings, TeBlock teBlock, CableType cableType, int insulation) {
        super(settings, teBlock, cableType, insulation);
        setDefaultState(getDefaultState().with(ACTIVE, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(ACTIVE);
    }

    @Override
    protected boolean emitsRedstonePower(BlockState state) {
        return getCableType() == CableType.detector;
    }
}
