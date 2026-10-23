package ic2.core.block.storage.tank;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

public class TileEntityIridiumTank extends TileEntityTank {
    public TileEntityIridiumTank(BlockEntityType<?> t, BlockPos p, BlockState s){ super(t,p,s,1024); }
    public TileEntityIridiumTank(BlockPos p, BlockState s){ this(null,p,s); }
    public TileEntityIridiumTank(){ super(1024); }
}
