package ic2.core.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

public class TileEntityBarrel extends TileEntityBlock {
    private int resin;
    private boolean radioactive;

    public TileEntityBarrel(BlockEntityType<?> t, BlockPos p, BlockState s){ super(t,p,s); }
    public TileEntityBarrel(BlockPos p, BlockState s){ this(null,p,s); }
    public TileEntityBarrel(){ super(null, BlockPos.ORIGIN, null); }

    public void addResin(int amount) { this.resin += amount; }
    public void addRadioactive(boolean value) { this.radioactive = value; }
    // overload for no-arg compatibility
    public void addRadioactive() { this.radioactive = true; }

    public int getResin() { return resin; }
    public boolean isRadioactive() { return radioactive; }
}
