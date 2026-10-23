package ic2.core.block.storage.tank;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

public class TileEntityIronTank extends TileEntityTank {
    public TileEntityIronTank(BlockEntityType<?> t, BlockPos p, BlockState s){ super(t,p,s,32); }
    public TileEntityIronTank(BlockPos p, BlockState s){ this(null,p,s); }
    public TileEntityIronTank(){ super(32); }
}
