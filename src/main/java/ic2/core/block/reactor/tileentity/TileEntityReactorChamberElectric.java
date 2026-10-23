package ic2.core.block.reactor.tileentity;

import ic2.api.reactor.IReactorChamber;
import ic2.core.block.TileEntityBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

/**
 * A chamber built against a reactor: it widens the reactor's grid and, as one of the reactor's
 * sub tiles on the energy net, gives off its EU to cables connected to it.
 */
public class TileEntityReactorChamberElectric extends TileEntityBlock implements IReactorChamber,
        ic2.api.energy.tile.IEnergyEmitter {
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

    /** Cables connect on every side; the energy itself comes from the reactor. */
    @Override public boolean emitsEnergyTo(ic2.api.energy.tile.IEnergyAcceptor receiver, net.minecraft.util.math.Direction side){ return true; }

    /** The reactor puts the chamber on the energy net with itself. */
    @Override protected boolean joinsEnergyNetItself(){ return false; }

    /** A chamber placed or broken changes the reactor's shape on the energy net. */
    @Override public void onLoad(){
        super.onLoad();
        notifyReactor();
    }

    @Override public void onBlockRemoved(){
        super.onBlockRemoved();
        notifyReactor();
    }

    private void notifyReactor(){
        if(world!=null && !world.isClient && getReactorInstance() instanceof TileEntityBlock reactor){
            reactor.onNeighborUpdate();
        }
    }
}
