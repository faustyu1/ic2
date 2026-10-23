package ic2.core.block.wiring;

import ic2.core.ref.TeBlock;
import ic2.core.init.IC2Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

public class TileEntityTransformerLV extends TileEntityTransformer {
    public TileEntityTransformerLV(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, TeBlock.lv_transformer, 1);
    }
    public TileEntityTransformerLV() {
        super(null, BlockPos.ORIGIN, null, 1);
    }

    public TileEntityTransformerLV(BlockPos pos, BlockState state) {
        this(IC2Blocks.LV_TRANSFORMER_BE, pos, state);
    }
}
