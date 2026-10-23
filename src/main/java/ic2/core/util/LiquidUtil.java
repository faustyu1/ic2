package ic2.core.util;

import ic2.core.block.TileEntityBlock;
import ic2.core.ref.FluidName;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * Fabric port of ic2.core.util.LiquidUtil: fluid names, units and moving fluid in and out of
 * blocks.
 *
 * <p>Every block, IC2's own included, is reached through Fabric's {@link FluidStorage#SIDED}.
 * Amounts are droplets, 81 to the millibucket; IC2's numbers are millibuckets, so they go
 * through {@link #mb} on their way in and {@link #toMb} on their way to the player.
 */
public final class LiquidUtil {
    /** Droplets in a millibucket. */
    public static final long MB = FluidConstants.BUCKET / 1000;

    private LiquidUtil() {
    }

    /** @return that many millibuckets in droplets */
    public static long mb(long millibuckets) {
        return millibuckets * MB;
    }

    /** @return the droplets in whole millibuckets, rounded down */
    public static long toMb(long droplets) {
        return droplets / MB;
    }

    /**
     * Resolves a fluid by the name 1.12.2 gave it - {@code water}, {@code ic2coolant} - or by its
     * registry id, {@code ic2:coolant}.
     *
     * @return the still fluid, or null when there is none of that name
     */
    public static Fluid getFluid(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        if (name.startsWith("ic2") && !name.startsWith("ic2:")) {
            for (FluidName fluid : FluidName.values()) {
                if (fluid.getName().equals(name)) {
                    return fluid.getInstance();
                }
            }
            return null;
        }
        Identifier id = Identifier.tryParse(name);
        if (id == null || !Registries.FLUID.containsId(id)) {
            return null;
        }
        Fluid fluid = Registries.FLUID.get(id);
        return fluid == Fluids.EMPTY ? null : still(fluid);
    }

    /** The name 1.12.2 gave the fluid: {@code water}, {@code ic2coolant}, or the registry id for any other. */
    public static String nameOf(Fluid fluid) {
        FluidName ic2 = FluidName.of(fluid);
        if (ic2 != null) {
            return ic2.getName();
        }
        Identifier id = Registries.FLUID.getId(still(fluid));
        return "minecraft".equals(id.getNamespace()) ? id.getPath() : id.toString();
    }

    /**
     * The tint a fluid is drawn with, as 0xRRGGBB: the 1.12.2 colour of an IC2 fluid, lava's
     * orange, and water blue for everything else. The client renders with Fabric's fluid render
     * handlers; this is for what the server side shows too, like an item's fill bar.
     */
    public static int colorOf(Fluid fluid) {
        FluidName ic2 = FluidName.of(fluid);
        if (ic2 != null) {
            return ic2.getColor();
        }
        return still(fluid) == Fluids.LAVA ? 0xD45A12 : 0x3F76E4;
    }

    /**
     * A number that names a fluid across the network: the hash of its registry id, so it does not
     * depend on the order things were registered in, and never 0, which stands for no fluid.
     */
    public static int syncId(Fluid fluid) {
        int id = Registries.FLUID.getId(still(fluid)).toString().hashCode();
        return id == 0 ? 1 : id;
    }

    /** @return the fluid {@link #syncId} produced this number for, or null */
    public static Fluid bySyncId(int id) {
        if (id == 0) {
            return null;
        }
        for (Fluid fluid : Registries.FLUID) {
            if (fluid != Fluids.EMPTY && still(fluid) == fluid && syncId(fluid) == id) {
                return fluid;
            }
        }
        return null;
    }

    /** The still form of a fluid, which is what tanks hold. */
    public static Fluid still(Fluid fluid) {
        return fluid instanceof net.minecraft.fluid.FlowableFluid flowable ? flowable.getStill() : fluid;
    }

    public static Storage<FluidVariant> getHandler(BlockEntity te, Direction side) {
        if (te == null) {
            return null;
        }
        if (te instanceof TileEntityBlock block) {
            return block.getFluidHandler(side);
        }
        if (te.getWorld() == null) {
            return null;
        }
        return FluidStorage.SIDED.find(te.getWorld(), te.getPos(), te.getCachedState(), te, side);
    }

    public static boolean isFluidTile(BlockEntity te, Direction side) {
        return getHandler(te, side) != null;
    }

    /** Drains up to that many droplets of whatever comes first. */
    public static ResourceAmount<FluidVariant> drainTile(BlockEntity te, Direction side, long maxAmount, boolean simulate) {
        return drainTile(te, side, null, maxAmount, simulate);
    }

    /**
     * Drains up to that many droplets of the fluid, or of whatever comes first when it is null.
     *
     * @return what came out, or would have, or null when nothing did
     */
    public static ResourceAmount<FluidVariant> drainTile(BlockEntity te, Direction side, Fluid fluid, long maxAmount, boolean simulate) {
        Storage<FluidVariant> handler = getHandler(te, side);
        if (handler == null || maxAmount <= 0) {
            return null;
        }
        FluidVariant variant = fluid != null ? FluidVariant.of(fluid) : StorageUtil.findExtractableResource(handler, null);
        if (variant == null || variant.isBlank()) {
            return null;
        }
        long drained;
        try (Transaction tx = Transaction.openOuter()) {
            drained = handler.extract(variant, maxAmount, tx);
            if (!simulate) {
                tx.commit();
            }
        }
        return drained <= 0 ? null : new ResourceAmount<>(variant, drained);
    }

    /** @return how many droplets went in, or would have */
    public static long fillTile(BlockEntity te, Direction side, FluidVariant fluid, long amount, boolean simulate) {
        Storage<FluidVariant> handler = getHandler(te, side);
        if (handler == null || fluid.isBlank() || amount <= 0) {
            return 0;
        }
        try (Transaction tx = Transaction.openOuter()) {
            long filled = handler.insert(fluid, amount, tx);
            if (!simulate) {
                tx.commit();
            }
            return filled;
        }
    }

    /** The neighbours of a block that take or give fluid on the side facing it. */
    public static List<AdjacentFluidHandler> getAdjacentHandlers(BlockEntity source) {
        List<AdjacentFluidHandler> ret = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            AdjacentFluidHandler handler = getAdjacentHandler(source, dir);
            if (handler != null) {
                ret.add(handler);
            }
        }
        return ret;
    }

    public static AdjacentFluidHandler getAdjacentHandler(BlockEntity source, Direction dir) {
        BlockEntity te = source.getWorld().getBlockEntity(source.getPos().offset(dir));
        return isFluidTile(te, dir.getOpposite()) ? new AdjacentFluidHandler(te, dir) : null;
    }

    /** Pushes up to that many droplets of the fluid into the neighbours in turn. */
    public static long distribute(BlockEntity source, FluidVariant fluid, long amount, boolean simulate) {
        long transferred = 0;
        for (AdjacentFluidHandler handler : getAdjacentHandlers(source)) {
            transferred += fillTile(handler.handler, handler.dir.getOpposite(), fluid, amount - transferred, simulate);
            if (transferred >= amount) {
                break;
            }
        }
        return transferred;
    }

    /** Moves up to that many droplets out of the source into all its neighbours. */
    public static long distributeAll(BlockEntity source, long amount) {
        long transferred = 0;
        for (Direction dir : Direction.values()) {
            BlockEntity te = source.getWorld().getBlockEntity(source.getPos().offset(dir));
            if (isFluidTile(te, dir.getOpposite())) {
                ResourceAmount<FluidVariant> moved = transfer(source, dir, te, amount - transferred);
                if (moved != null) {
                    transferred += moved.amount();
                    if (transferred >= amount) {
                        break;
                    }
                }
            }
        }
        return transferred;
    }

    /**
     * Moves up to that many droplets from the source's side dir into the target, as much as both
     * agree on.
     *
     * @return what moved, or null when nothing did
     */
    public static ResourceAmount<FluidVariant> transfer(BlockEntity source, Direction dir, BlockEntity target, long amount) {
        Storage<FluidVariant> from = getHandler(source, dir);
        Storage<FluidVariant> to = getHandler(target, dir == null ? null : dir.getOpposite());
        if (from == null || to == null || amount <= 0) {
            return null;
        }
        FluidVariant variant = StorageUtil.findExtractableResource(from, null);
        if (variant == null) {
            return null;
        }
        long moved = StorageUtil.move(from, to, variant::equals, amount, null);
        return moved <= 0 ? null : new ResourceAmount<>(variant, moved);
    }

    /** The fluid a block in the world is, and whether it is a source; null for no fluid. */
    public static LiquidData getLiquid(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof FluidBlock)) {
            return null;
        }
        FluidState fluid = state.getFluidState();
        return fluid.isEmpty() ? null : new LiquidData(still(fluid.getFluid()), state.get(FluidBlock.LEVEL) == 0);
    }

    /** Takes a bucket's worth source block out of the world; null when the block is no source. */
    public static ResourceAmount<FluidVariant> drainBlock(World world, BlockPos pos, boolean simulate) {
        LiquidData data = getLiquid(world, pos);
        if (data == null || !data.isSource) {
            return null;
        }
        if (!simulate) {
            world.setBlockState(pos, Blocks.AIR.getDefaultState());
        }
        return new ResourceAmount<>(FluidVariant.of(data.liquid), FluidConstants.BUCKET);
    }

    public static class LiquidData {
        public final Fluid liquid;
        public final boolean isSource;

        LiquidData(Fluid liquid, boolean isSource) {
            this.liquid = liquid;
            this.isSource = isSource;
        }
    }

    public static String toStringSafe(ResourceAmount<FluidVariant> fluid) {
        return fluid == null ? "(null)" : toMb(fluid.amount()) + " mB x" + nameOf(fluid.resource().getFluid());
    }

    public static class AdjacentFluidHandler {
        public final BlockEntity handler;
        public final Direction dir;

        AdjacentFluidHandler(BlockEntity handler, Direction dir) {
            this.handler = handler;
            this.dir = dir;
        }
    }
}
