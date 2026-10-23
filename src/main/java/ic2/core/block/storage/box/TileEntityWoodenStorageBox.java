package ic2.core.block.storage.box;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

public class TileEntityWoodenStorageBox extends TileEntityStorageBox {
    public TileEntityWoodenStorageBox(BlockEntityType<?> t, BlockPos p, BlockState s){ super(t, p, s, 27); }
    public TileEntityWoodenStorageBox(BlockPos p, BlockState s){ this(null, p, s); }
    public TileEntityWoodenStorageBox(){ super(null, BlockPos.ORIGIN, null, 27); }
}
