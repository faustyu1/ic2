package ic2.core.block.wiring;

import ic2.core.IC2;
import ic2.core.ref.TeBlock;
import ic2.core.init.IC2Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

public class TileEntityElectricMFSU extends TileEntityElectricBlock {
    public TileEntityElectricMFSU(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, TeBlock.mfsu, 4, 2048, 40000000);
    }

    public TileEntityElectricMFSU() {
        super(null, BlockPos.ORIGIN, null, 4, 2048, 40000000);
    }

    public TileEntityElectricMFSU(BlockPos pos, BlockState state) {
        this(IC2Blocks.MFSU_BE, pos, state);
    }

    public static Class<? extends TileEntityElectricBlock> delegate() {
        return IC2.version.isClassic() ? TileEntityElectricClassicMFSU.class : TileEntityElectricMFSU.class;
    }

    public static class TileEntityElectricClassicMFSU extends TileEntityElectricBlock {
        public TileEntityElectricClassicMFSU(BlockEntityType<?> type, BlockPos pos, BlockState state) {
            super(type, pos, state, TeBlock.mfsu, 3, 512, 10000000);
        }

        public TileEntityElectricClassicMFSU() {
            super(null, BlockPos.ORIGIN, null, 3, 512, 10000000);
        }
    }
}
