package ic2.core.block.storage.box;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

public class TileEntityBronzeStorageBox extends TileEntityStorageBox {
    public TileEntityBronzeStorageBox(BlockEntityType<?> t, BlockPos p, BlockState s){ super(t, p, s, 45); }
    public TileEntityBronzeStorageBox(BlockPos p, BlockState s){ this(null, p, s); }
    public TileEntityBronzeStorageBox(){ super(null, BlockPos.ORIGIN, null, 45); }
}
