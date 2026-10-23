package ic2.core.block.wiring;

import ic2.core.ref.TeBlock;
import ic2.core.init.IC2Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

public class TileEntityElectricBatBox extends TileEntityElectricBlock {
    public TileEntityElectricBatBox(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, TeBlock.batbox, 1, 32, 40000);
    }

    // For Fabric BlockEntityType instantiation via no-arg reflection fallback
    public TileEntityElectricBatBox() {
        super(null, BlockPos.ORIGIN, null, 1, 32, 40000);
    }

    public TileEntityElectricBatBox(BlockPos pos, BlockState state) {
        this(IC2Blocks.BATBOX_BE, pos, state);
    }
}
