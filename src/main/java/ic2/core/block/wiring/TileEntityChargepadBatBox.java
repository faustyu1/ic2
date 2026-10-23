package ic2.core.block.wiring;

import ic2.core.init.IC2Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

/** The BatBox charge pad: tier 1, 32 EU a tick per item, 40000 EU. */
public class TileEntityChargepadBatBox extends TileEntityChargepadBlock {
    public TileEntityChargepadBatBox(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.CHARGEPAD_BATBOX_BE : type, pos, state, 1, 32, 40000);
    }

    public TileEntityChargepadBatBox(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }
}
