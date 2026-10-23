package ic2.core.block.wiring;

import ic2.core.init.IC2Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

/** The CESU charge pad: tier 2, 128 EU a tick per item, 300000 EU. */
public class TileEntityChargepadCESU extends TileEntityChargepadBlock {
    public TileEntityChargepadCESU(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.CHARGEPAD_CESU_BE : type, pos, state, 2, 128, 300000);
    }

    public TileEntityChargepadCESU(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }
}
