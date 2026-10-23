package ic2.core.util;

import ic2.core.block.TileEntityBlock;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/**
 * Fabric port of the block side of ic2.core.util.LiquidUtil: moving fluid in and out of blocks.
 *
 * <p>IC2 blocks offer their tanks through {@link TileEntityBlock#getFluidHandler}. Blocks of other
 * mods are reached through Fabric's fluid transfer API, for the fluids that exist as Minecraft
 * fluids (water and lava; the IC2 fluids are not registered as such): a millibucket is 81
 * droplets there.
 */
public final class LiquidUtil {
    private static final long DROPLETS_PER_MB = FluidConstants.BUCKET / 1000;

    private LiquidUtil() {
    }

    public static IFluidHandler getHandler(BlockEntity te, Direction side) {
        if (te == null) {
            return null;
        }
        if (te instanceof TileEntityBlock block) {
            return block.getFluidHandler(side);
        }
        if (te.getWorld() == null) {
            return null;
        }
        Storage<FluidVariant> storage = FluidStorage.SIDED.find(te.getWorld(), te.getPos(), te.getCachedState(), te, side);
        return storage == null ? null : new FabricHandler(storage);
    }

    public static boolean isFluidTile(BlockEntity te, Direction side) {
        return getHandler(te, side) != null;
    }

    public static FluidStack drainTile(BlockEntity te, Direction side, int maxAmount, boolean simulate) {
        return drainTile(te, side, null, maxAmount, simulate);
    }

    public static FluidStack drainTile(BlockEntity te, Direction side, Fluid fluid, int maxAmount, boolean simulate) {
        IFluidHandler handler = getHandler(te, side);
        if (handler == null) {
            return null;
        }
        return fluid == null ? handler.drain(maxAmount, !simulate) : handler.drain(new FluidStack(fluid, maxAmount), !simulate);
    }

    public static int fillTile(BlockEntity te, Direction side, FluidStack fs, boolean simulate) {
        IFluidHandler handler = getHandler(te, side);
        return handler == null ? 0 : handler.fill(fs, !simulate);
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

    /** Pushes the fluid into the neighbours in turn; the stack is left as it was. */
    public static int distribute(BlockEntity source, FluidStack stack, boolean simulate) {
        int transferred = 0;
        for (AdjacentFluidHandler handler : getAdjacentHandlers(source)) {
            int amount = fillTile(handler.handler, handler.dir.getOpposite(), stack, simulate);
            transferred += amount;
            stack.amount -= amount;
            if (stack.amount <= 0) {
                break;
            }
        }
        stack.amount += transferred;
        return transferred;
    }

    /** Moves up to that much out of the source into all its neighbours. */
    public static int distributeAll(BlockEntity source, int amount) {
        int transferred = 0;
        for (Direction dir : Direction.values()) {
            BlockEntity te = source.getWorld().getBlockEntity(source.getPos().offset(dir));
            if (isFluidTile(te, dir.getOpposite())) {
                FluidStack stack = transfer(source, dir, te, amount);
                if (stack != null) {
                    amount -= stack.amount;
                    transferred += stack.amount;
                    if (amount <= 0) {
                        break;
                    }
                }
            }
        }
        return transferred;
    }

    /**
     * Moves up to the amount from the source's side dir into the target, as much as both agree on.
     *
     * @return what moved, or null when nothing did
     */
    public static FluidStack transfer(BlockEntity source, Direction dir, BlockEntity target, int amount) {
        while (true) {
            FluidStack ret = drainTile(source, dir, amount, true);
            if (ret == null || ret.amount <= 0) {
                return null;
            }

            int cAmount = fillTile(target, dir == null ? null : dir.getOpposite(), ret.copy(), true);
            if (cAmount <= 0) {
                return null;
            }
            amount = Math.min(amount, cAmount);
            if (cAmount != ret.amount) {
                continue;
            }

            ret = drainTile(source, dir, cAmount, false);
            if (ret == null) {
                return null;
            }
            fillTile(target, dir == null ? null : dir.getOpposite(), ret.copy(), false);
            return ret;
        }
    }

    /** The fluid a block in the world is, and whether it is a source; null for no fluid. */
    public static LiquidData getLiquid(net.minecraft.world.World world, net.minecraft.util.math.BlockPos pos) {
        net.minecraft.block.BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof net.minecraft.block.FluidBlock)) {
            return null;
        }
        Fluid liquid = fluidOf(state);
        return liquid == null ? null : new LiquidData(liquid, state.get(net.minecraft.block.FluidBlock.LEVEL) == 0);
    }

    private static Fluid fluidOf(net.minecraft.block.BlockState state) {
        if (state.getBlock() instanceof ic2.core.block.BlockIC2Fluid ic2Fluid) {
            return ic2Fluid.getIc2Fluid();
        }
        Identifier id = Registries.FLUID.getId(state.getFluidState().getFluid());
        String name = "minecraft".equals(id.getNamespace()) ? id.getPath() : id.toString();
        name = name.startsWith("flowing_") ? name.substring("flowing_".length()) : name;
        return FluidRegistry.getFluid(name);
    }

    /** Takes a 1000 mB source block out of the world; null when the block is no source. */
    public static FluidStack drainBlock(net.minecraft.world.World world, net.minecraft.util.math.BlockPos pos, boolean simulate) {
        LiquidData data = getLiquid(world, pos);
        if (data == null || !data.isSource) {
            return null;
        }
        if (!simulate) {
            world.setBlockState(pos, net.minecraft.block.Blocks.AIR.getDefaultState());
        }
        return new FluidStack(data.liquid, 1000);
    }

    public static class LiquidData {
        public final Fluid liquid;
        public final boolean isSource;

        LiquidData(Fluid liquid, boolean isSource) {
            this.liquid = liquid;
            this.isSource = isSource;
        }
    }

    public static boolean check(FluidStack fs) {
        return fs.getFluid() != null;
    }

    public static String toStringSafe(FluidStack fluidStack) {
        return fluidStack == null ? "(null)" : fluidStack.amount + "x" + fluidStack.getFluid().getName();
    }

    public static class AdjacentFluidHandler {
        public final BlockEntity handler;
        public final Direction dir;

        AdjacentFluidHandler(BlockEntity handler, Direction dir) {
            this.handler = handler;
            this.dir = dir;
        }
    }

    /** A block of another mod, through Fabric's transfer API. */
    private record FabricHandler(Storage<FluidVariant> storage) implements IFluidHandler {
        @Override
        public int fill(FluidStack resource, boolean doFill) {
            FluidVariant variant = toVariant(resource == null ? null : resource.getFluid());
            if (variant == null || resource.amount <= 0 || !storage.supportsInsertion()) {
                return 0;
            }
            // only whole millibuckets count: find out how many, then move exactly those
            int mb = (int) (simulate(tx -> storage.insert(variant, resource.amount * DROPLETS_PER_MB, tx)) / DROPLETS_PER_MB);
            if (doFill && mb > 0) {
                try (Transaction tx = Transaction.openOuter()) {
                    storage.insert(variant, mb * DROPLETS_PER_MB, tx);
                    tx.commit();
                }
            }
            return mb;
        }

        @Override
        public FluidStack drain(FluidStack resource, boolean doDrain) {
            FluidVariant variant = toVariant(resource == null ? null : resource.getFluid());
            if (variant == null || resource.amount <= 0 || !storage.supportsExtraction()) {
                return null;
            }
            int mb = (int) (simulate(tx -> storage.extract(variant, resource.amount * DROPLETS_PER_MB, tx)) / DROPLETS_PER_MB);
            if (mb <= 0) {
                return null;
            }
            if (doDrain) {
                try (Transaction tx = Transaction.openOuter()) {
                    storage.extract(variant, mb * DROPLETS_PER_MB, tx);
                    tx.commit();
                }
            }
            return new FluidStack(resource.getFluid(), mb);
        }

        private static long simulate(java.util.function.ToLongFunction<Transaction> op) {
            try (Transaction tx = Transaction.openOuter()) {
                return op.applyAsLong(tx);
            }
        }

        @Override
        public FluidStack drain(int maxDrain, boolean doDrain) {
            for (StorageView<FluidVariant> view : storage.nonEmptyViews()) {
                Fluid fluid = fromVariant(view.getResource());
                if (fluid != null) {
                    return drain(new FluidStack(fluid, maxDrain), doDrain);
                }
            }
            return null;
        }
    }

    private static FluidVariant toVariant(Fluid fluid) {
        if (fluid == null) {
            return null;
        }
        net.minecraft.fluid.Fluid mc = fluid.getMinecraftFluid();
        return mc == null ? null : FluidVariant.of(mc);
    }

    private static Fluid fromVariant(FluidVariant variant) {
        if (variant.isBlank()) {
            return null;
        }
        Identifier id = Registries.FLUID.getId(variant.getFluid());
        return FluidRegistry.getFluid("minecraft".equals(id.getNamespace()) ? id.getPath() : id.toString());
    }
}
