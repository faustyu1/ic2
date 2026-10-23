package ic2.core.block.wiring;

import ic2.core.ref.TeBlock;
import ic2.core.init.IC2Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

public class TileEntityTransformerHV extends TileEntityTransformer {
    public TileEntityTransformerHV(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, TeBlock.hv_transformer, 3);
    }
    public TileEntityTransformerHV() {
        super(null, BlockPos.ORIGIN, null, 3);
    }

    public TileEntityTransformerHV(BlockPos pos, BlockState state) {
        this(IC2Blocks.HV_TRANSFORMER_BE, pos, state);
    }
}
