package ic2.core.block.reactor.tileentity;

import ic2.api.reactor.IReactorChamber;
import ic2.core.block.TileEntityInventory;
import ic2.core.block.comp.FluidReactorLookup;
import ic2.core.block.comp.Fluids;
import ic2.core.block.invslot.InvSlotUpgrade;
import ic2.core.ref.TeBlock;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

/**
 * Fabric port of TileEntityReactorFluidPort - the casing block a fluid reactor's coolant goes
 * through.
 *
 * <p>The port owns no tanks of its own: it hands out the reactor's two tanks, so a pipe attached
 * to any port of the hull fills and drains the same coolant loop. Once the reactor stops being
 * fluid cooled the lookup gives nothing back and the port goes quiet on its own.
 */
public class TileEntityReactorFluidPort extends TileEntityInventory implements IReactorChamber {
    public final InvSlotUpgrade upgradeSlot;
    private final FluidReactorLookup lookup = new FluidReactorLookup(this);

    public TileEntityReactorFluidPort(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, TeBlock.reactor_fluid_port);
        this.upgradeSlot = new InvSlotUpgrade(this, "upgrade", 1);

        fluids().addUnmanagedTankHook(() -> {
            TileEntityNuclearReactorElectric reactor = getReactorInstance();
            return reactor == null
                    ? Collections.<Fluids.InternalFluidTank>emptySet()
                    : Arrays.asList(reactor.inputTank, reactor.outputTank);
        });
    }

    public TileEntityReactorFluidPort(BlockPos pos, BlockState state) { this(null, pos, state); }
    public TileEntityReactorFluidPort() { this(null, BlockPos.ORIGIN, null); }

    @Override
    public void tick() {
        if (world != null && !world.isClient && upgradeSlot.tickNoMark()) {
            markDirty();
        }
    }

    @Override
    public TileEntityNuclearReactorElectric getReactorInstance() {
        return lookup.getReactor();
    }

    @Override
    public boolean isWall() {
        return true;
    }

    /** The tanks a pipe may reach through this port: the reactor's, or none at all. */
    public Collection<Fluids.InternalFluidTank> getReachableTanks() {
        return fluids().getAllTanks();
    }
}
