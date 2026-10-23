package ic2.core.block.comp;

import ic2.core.block.TileEntityBlock;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/**
 * One side of an IC2 block as Fabric fluid storage, so pipes and tanks of other mods reach IC2
 * tanks the way Forge mods reached them through the fluid capability.
 *
 * <p>Only fluids that exist as Minecraft fluids can pass (water and lava; the IC2 fluids are not
 * registered as such), in whole millibuckets of 81 droplets. A transaction works on the tanks
 * directly and puts their old contents back if it is aborted.
 */
public final class FabricFluidStorage extends SnapshotParticipant<List<FluidStack>> implements Storage<FluidVariant> {
    private static final long DROPLETS_PER_MB = FluidConstants.BUCKET / 1000;

    private final TileEntityBlock tile;
    private final IFluidHandler handler;

    private FabricFluidStorage(TileEntityBlock tile, IFluidHandler handler) {
        this.tile = tile;
        this.handler = handler;
    }

    /** Makes the IC2 blocks with tanks visible through {@link FluidStorage#SIDED}. */
    public static void register() {
        FluidStorage.SIDED.registerFallback((world, pos, state, be, side) -> {
            if (be instanceof TileEntityBlock tile) {
                IFluidHandler handler = tile.getFluidHandler(side);
                return handler == null ? null : new FabricFluidStorage(tile, handler);
            }
            return null;
        });
    }

    private List<Fluids.InternalFluidTank> tanks() {
        Fluids fluids = tile.getFluidsComponent();
        return fluids == null ? List.of() : fluids.getAllTanks();
    }

    @Override
    protected List<FluidStack> createSnapshot() {
        List<FluidStack> snapshot = new ArrayList<>();
        for (Fluids.InternalFluidTank tank : tanks()) {
            snapshot.add(tank.getFluid() == null ? null : tank.getFluid().copy());
        }
        return snapshot;
    }

    @Override
    protected void readSnapshot(List<FluidStack> snapshot) {
        List<Fluids.InternalFluidTank> tanks = tanks();
        for (int i = 0; i < tanks.size() && i < snapshot.size(); i++) {
            tanks.get(i).setFluid(snapshot.get(i) == null ? null : snapshot.get(i).copy());
        }
    }

    @Override
    protected void onFinalCommit() {
        tile.markDirty();
    }

    @Override
    public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        Fluid fluid = toIc2(resource);
        int mb = (int) Math.min(Integer.MAX_VALUE, maxAmount / DROPLETS_PER_MB);
        if (fluid == null || mb <= 0) {
            return 0;
        }
        updateSnapshots(transaction);
        return handler.fill(new FluidStack(fluid, mb), true) * DROPLETS_PER_MB;
    }

    @Override
    public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        Fluid fluid = toIc2(resource);
        int mb = (int) Math.min(Integer.MAX_VALUE, maxAmount / DROPLETS_PER_MB);
        if (fluid == null || mb <= 0) {
            return 0;
        }
        updateSnapshots(transaction);
        FluidStack drained = handler.drain(new FluidStack(fluid, mb), true);
        return drained == null ? 0 : drained.amount * DROPLETS_PER_MB;
    }

    @Override
    public Iterator<StorageView<FluidVariant>> iterator() {
        List<StorageView<FluidVariant>> views = new ArrayList<>();
        for (Fluids.InternalFluidTank tank : tanks()) {
            views.add(new TankView(tank));
        }
        return views.iterator();
    }

    private final class TankView implements StorageView<FluidVariant> {
        private final Fluids.InternalFluidTank tank;

        TankView(Fluids.InternalFluidTank tank) {
            this.tank = tank;
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            return resource.equals(getResource()) ? FabricFluidStorage.this.extract(resource, maxAmount, transaction) : 0;
        }

        @Override
        public boolean isResourceBlank() {
            return getResource().isBlank();
        }

        @Override
        public FluidVariant getResource() {
            FluidStack content = tank.getFluid();
            if (content == null || content.amount <= 0) {
                return FluidVariant.blank();
            }
            net.minecraft.fluid.Fluid mc = content.getFluid().getMinecraftFluid();
            return mc == null ? FluidVariant.blank() : FluidVariant.of(mc);
        }

        @Override
        public long getAmount() {
            return isResourceBlank() ? 0 : tank.getFluidAmount() * DROPLETS_PER_MB;
        }

        @Override
        public long getCapacity() {
            return tank.getCapacity() * DROPLETS_PER_MB;
        }
    }

    private static Fluid toIc2(FluidVariant variant) {
        if (variant.isBlank()) {
            return null;
        }
        Identifier id = Registries.FLUID.getId(variant.getFluid());
        Fluid fluid = FluidRegistry.getFluid("minecraft".equals(id.getNamespace()) ? id.getPath() : id.toString());
        return fluid != null && fluid.getMinecraftFluid() == variant.getFluid() ? fluid : null;
    }
}
