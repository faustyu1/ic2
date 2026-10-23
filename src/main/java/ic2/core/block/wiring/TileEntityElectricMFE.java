package ic2.core.block.wiring;

import ic2.core.IC2;
import ic2.core.ref.TeBlock;
import ic2.core.init.IC2Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

public class TileEntityElectricMFE extends TileEntityElectricBlock {
    public TileEntityElectricMFE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, TeBlock.mfe, 3, 512, 4000000);
    }

    public TileEntityElectricMFE() {
        super(null, BlockPos.ORIGIN, null, 3, 512, 4000000);
    }

    public TileEntityElectricMFE(BlockPos pos, BlockState state) {
        this(IC2Blocks.MFE_BE, pos, state);
    }

    public static Class<? extends TileEntityElectricBlock> delegate() {
        return IC2.version.isClassic() ? TileEntityElectricClassicMFE.class : TileEntityElectricMFE.class;
    }

    public static class TileEntityElectricClassicMFE extends TileEntityElectricBlock {
        public TileEntityElectricClassicMFE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
            super(type, pos, state, TeBlock.mfe, 2, 128, 600000);
        }

        public TileEntityElectricClassicMFE() {
            super(null, BlockPos.ORIGIN, null, 2, 128, 600000);
        }
    }
}
