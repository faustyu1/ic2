package ic2.core.block.storage.box;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

public class TileEntityIronStorageBox extends TileEntityStorageBox {
    public TileEntityIronStorageBox(BlockEntityType<?> t, BlockPos p, BlockState s){ super(t, p, s, 45); }
    public TileEntityIronStorageBox(BlockPos p, BlockState s){ this(null, p, s); }
    public TileEntityIronStorageBox(){ super(null, BlockPos.ORIGIN, null, 45); }
}
