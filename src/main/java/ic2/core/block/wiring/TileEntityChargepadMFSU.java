package ic2.core.block.wiring;

import ic2.core.init.IC2Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

/** The MFSU charge pad: tier 4, 2048 EU a tick per item, 40000000 EU. */
public class TileEntityChargepadMFSU extends TileEntityChargepadBlock {
    public TileEntityChargepadMFSU(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.CHARGEPAD_MFSU_BE : type, pos, state, 4, 2048, 40000000);
    }

    public TileEntityChargepadMFSU(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }
}
