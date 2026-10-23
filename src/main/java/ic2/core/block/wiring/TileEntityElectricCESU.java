package ic2.core.block.wiring;

import ic2.core.profile.NotClassic;
import ic2.core.ref.TeBlock;
import ic2.core.init.IC2Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

@NotClassic
public class TileEntityElectricCESU extends TileEntityElectricBlock {
    public TileEntityElectricCESU(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, TeBlock.cesu, 2, 128, 300000);
    }

    public TileEntityElectricCESU() {
        super(null, BlockPos.ORIGIN, null, 2, 128, 300000);
    }

    public TileEntityElectricCESU(BlockPos pos, BlockState state) {
        this(IC2Blocks.CESU_BE, pos, state);
    }
}
