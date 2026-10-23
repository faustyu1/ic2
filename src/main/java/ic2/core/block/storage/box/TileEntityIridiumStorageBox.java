package ic2.core.block.storage.box;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

public class TileEntityIridiumStorageBox extends TileEntityStorageBox {
    public TileEntityIridiumStorageBox(BlockEntityType<?> t, BlockPos p, BlockState s){ super(t, p, s, 126); }
    public TileEntityIridiumStorageBox(BlockPos p, BlockState s){ this(null, p, s); }
    public TileEntityIridiumStorageBox(){ super(null, BlockPos.ORIGIN, null, 126); }
}
