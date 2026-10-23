package ic2.core.block.storage.tank;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

public class TileEntitySteelTank extends TileEntityTank {
    public TileEntitySteelTank(BlockEntityType<?> t, BlockPos p, BlockState s){ super(t,p,s,128); }
    public TileEntitySteelTank(BlockPos p, BlockState s){ this(null,p,s); }
    public TileEntitySteelTank(){ super(128); }
}
