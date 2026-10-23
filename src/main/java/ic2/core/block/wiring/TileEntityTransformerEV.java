package ic2.core.block.wiring;

import ic2.core.ref.TeBlock;
import ic2.core.init.IC2Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

public class TileEntityTransformerEV extends TileEntityTransformer {
    public TileEntityTransformerEV(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, TeBlock.ev_transformer, 4);
    }
    public TileEntityTransformerEV() {
        super(null, BlockPos.ORIGIN, null, 4);
    }

    public TileEntityTransformerEV(BlockPos pos, BlockState state) {
        this(IC2Blocks.EV_TRANSFORMER_BE, pos, state);
    }
}
