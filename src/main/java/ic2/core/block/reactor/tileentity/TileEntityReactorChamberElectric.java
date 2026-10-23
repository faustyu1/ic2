package ic2.core.block.reactor.tileentity;

import ic2.api.reactor.IReactorChamber;
import ic2.core.block.TileEntityBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

/** Stub - links to main reactor core, no logic. */
public class TileEntityReactorChamberElectric extends TileEntityBlock implements IReactorChamber {
    public TileEntityReactorChamberElectric(BlockEntityType<?> t, BlockPos p, BlockState s){ super(t,p,s); }
    public TileEntityReactorChamberElectric(BlockPos p, BlockState s){ this(null,p,s); }
    public TileEntityReactorChamberElectric(){ super(null, BlockPos.ORIGIN, null); }
    @Override public ic2.api.reactor.IReactor getReactorInstance(){
        if(world==null) return null;
        for(var d: net.minecraft.util.math.Direction.values()){
            var be=world.getBlockEntity(pos.offset(d));
            if(be instanceof ic2.api.reactor.IReactor r) return r;
        }
        return null;
    }
    @Override public boolean isWall(){ return false; }
}
