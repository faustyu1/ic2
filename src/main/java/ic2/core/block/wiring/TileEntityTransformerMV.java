package ic2.core.block.wiring;

import ic2.core.ref.TeBlock;
import ic2.core.init.IC2Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

public class TileEntityTransformerMV extends TileEntityTransformer {
    public TileEntityTransformerMV(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, TeBlock.mv_transformer, 2);
    }
    public TileEntityTransformerMV() {
        super(null, BlockPos.ORIGIN, null, 2);
    }

    public TileEntityTransformerMV(BlockPos pos, BlockState state) {
        this(IC2Blocks.MV_TRANSFORMER_BE, pos, state);
    }
}
