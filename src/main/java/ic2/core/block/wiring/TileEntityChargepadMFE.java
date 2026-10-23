package ic2.core.block.wiring;

import ic2.core.init.IC2Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

/** The MFE charge pad: tier 3, 512 EU a tick per item, 4000000 EU. */
public class TileEntityChargepadMFE extends TileEntityChargepadBlock {
    public TileEntityChargepadMFE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.CHARGEPAD_MFE_BE : type, pos, state, 3, 512, 4000000);
    }

    public TileEntityChargepadMFE(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }
}
