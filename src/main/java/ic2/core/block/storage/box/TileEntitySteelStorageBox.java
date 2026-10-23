package ic2.core.block.storage.box;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

public class TileEntitySteelStorageBox extends TileEntityStorageBox {
    public TileEntitySteelStorageBox(BlockEntityType<?> t, BlockPos p, BlockState s){ super(t, p, s, 63); }
    public TileEntitySteelStorageBox(BlockPos p, BlockState s){ this(null, p, s); }
    public TileEntitySteelStorageBox(){ super(null, BlockPos.ORIGIN, null, 63); }
}
