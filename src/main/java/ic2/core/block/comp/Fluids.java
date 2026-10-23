package ic2.core.block.comp;

import ic2.api.recipe.ILiquidAcceptManager;
import ic2.core.block.TileEntityBlock;
import ic2.core.block.invslot.InvSlot;
import ic2.core.util.LiquidUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.FilteringStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.minecraft.fluid.Fluid;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Direction;

/**
 * Fabric port of the Fluids tile entity component: the named tanks a machine owns.
 *
 * <p>A tank is addressed by name, exactly like an {@link InvSlot} group, which is what lets the
 * guidef files place {@code <fluidtank name="input">} without knowing the machine. Each tank says
 * which sides may fill or drain it and which fluids it takes at all. Tanks are Fabric
 * {@link SingleVariantStorage}s and count in droplets.
 */
public class Fluids {
    private final List<InternalFluidTank> tanks = new ArrayList<>();
    private final List<Supplier<Collection<InternalFluidTank>>> unmanagedTankHooks = new ArrayList<>();
    private final Runnable onChange;

    /** @param onChange called once a transaction changed a tank, to save the block */
    public Fluids(Runnable onChange) {
        this.onChange = onChange;
    }

    /** @param capacity in millibuckets */
    public InternalFluidTank addTank(String name, int capacity) {
        return addTank(name, capacity, InvSlot.Access.IO, InvSlot.InvSide.ANY, fluid -> true);
    }

    public InternalFluidTank addTank(String name, int capacity, Predicate<Fluid> acceptedFluids) {
        return addTank(name, capacity, InvSlot.Access.IO, InvSlot.InvSide.ANY, acceptedFluids);
    }

    public InternalFluidTank addTankInsert(String name, int capacity, Predicate<Fluid> acceptedFluids) {
        return addTank(name, capacity, InvSlot.Access.I, InvSlot.InvSide.ANY, acceptedFluids);
    }

    public InternalFluidTank addTankExtract(String name, int capacity) {
        return addTank(name, capacity, InvSlot.Access.O, InvSlot.InvSide.ANY, fluid -> true);
    }

    public InternalFluidTank addTank(String name, int capacity, InvSlot.Access access,
                                     InvSlot.InvSide side, Predicate<Fluid> acceptedFluids) {
        Set<Direction> sides = side.getAcceptedSides();
        return addTank(new InternalFluidTank(name, capacity,
                access.isInput() ? sides : Collections.emptySet(),
                access.isOutput() ? sides : Collections.emptySet(),
                acceptedFluids));
    }

    public InternalFluidTank addTank(InternalFluidTank tank) {
        tank.onChange = onChange;
        tanks.add(tank);
        return tank;
    }

    /**
     * A source of tanks this component does not own but still exposes, which is how the reactor
     * fluid port hands out the reactor's own two tanks without copying anything.
     */
    public void addUnmanagedTankHook(Supplier<Collection<InternalFluidTank>> hook) {
        unmanagedTankHooks.add(hook);
    }

    public List<InternalFluidTank> getAllTanks() {
        if (unmanagedTankHooks.isEmpty()) {
            return Collections.unmodifiableList(tanks);
        }

        List<InternalFluidTank> ret = new ArrayList<>(tanks);
        for (Supplier<Collection<InternalFluidTank>> hook : unmanagedTankHooks) {
            ret.addAll(hook.get());
        }
        return Collections.unmodifiableList(ret);
    }

    /**
     * Opens or closes a tank to the outside world. The reactor uses it to make its coolant tanks
     * reachable only while it is in fluid mode.
     */
    public void changeConnectivity(InternalFluidTank tank, InvSlot.Access access, InvSlot.InvSide side) {
        Set<Direction> sides = side.getAcceptedSides();
        tank.setSides(access.isInput() ? sides : Collections.emptySet(),
                access.isOutput() ? sides : Collections.emptySet());
    }

    /** @return the tank with that name, or null when the machine has none */
    public InternalFluidTank getFluidTank(String name) {
        for (InternalFluidTank tank : getAllTanks()) {
            if (tank.identifier.equals(name)) {
                return tank;
            }
        }
        return null;
    }

    public boolean isEmpty() {
        return tanks.isEmpty();
    }

    /** Makes the IC2 blocks with tanks visible to Fabric's fluid transfer API. */
    public static void register() {
        FluidStorage.SIDED.registerFallback((world, pos, state, be, side) ->
                be instanceof TileEntityBlock tile ? tile.getFluidHandler(side) : null);
    }

    /** Accepts exactly the fluids listed. */
    public static Predicate<Fluid> fluidPredicate(Fluid... fluids) {
        Collection<Fluid> accepted = List.of(fluids);
        return accepted::contains;
    }

    /**
     * Accepts whatever a recipe manager has a recipe for. The manager has to exist by the time
     * the tank is built, which is why the managers are created during mod init and not with the
     * rest of the recipes.
     */
    public static Predicate<Fluid> fluidPredicate(ILiquidAcceptManager manager) {
        return fluid -> manager != null && manager.acceptsFluid(fluid);
    }

    public void readFromNbt(NbtCompound nbt) {
        for (InternalFluidTank tank : tanks) {
            if (nbt.contains(tank.identifier)) {
                tank.readFromNbt(nbt.getCompound(tank.identifier));
            } else {
                tank.setFluid(FluidVariant.blank(), 0);
            }
        }
    }

    public NbtCompound writeToNbt() {
        NbtCompound nbt = new NbtCompound();
        for (InternalFluidTank tank : tanks) {
            nbt.put(tank.identifier, tank.writeToNbt(new NbtCompound()));
        }
        return nbt;
    }

    /**
     * The fluid storage of a side: filling goes into each tank that side may fill until the fluid
     * is used up, draining takes from the tanks that side may drain, as the 1.12.2 component did.
     */
    public Storage<FluidVariant> getHandler(Direction side) {
        List<Storage<FluidVariant>> parts = new ArrayList<>();
        for (InternalFluidTank tank : getAllTanks()) {
            parts.add(new FilteringStorage<>(tank) {
                @Override
                protected boolean canInsert(FluidVariant resource) {
                    return tank.canFill(side);
                }

                @Override
                protected boolean canExtract(FluidVariant resource) {
                    return tank.canDrain(side);
                }
            });
        }
        return new CombinedStorage<>(parts);
    }

    /** One named tank of a machine. Capacity and contents are droplets. */
    public static class InternalFluidTank extends SingleVariantStorage<FluidVariant> {
        public final String identifier;
        private final long capacity;
        private final Predicate<Fluid> acceptedFluids;
        private Collection<Direction> inputSides;
        private Collection<Direction> outputSides;
        private Runnable onChange;

        /** @param capacity in millibuckets */
        public InternalFluidTank(String identifier, int capacity,
                                 Collection<Direction> inputSides, Collection<Direction> outputSides,
                                 Predicate<Fluid> acceptedFluids) {
            this.identifier = identifier;
            this.capacity = LiquidUtil.mb(capacity);
            this.inputSides = inputSides;
            this.outputSides = outputSides;
            this.acceptedFluids = acceptedFluids;
        }

        @Override
        protected FluidVariant getBlankVariant() {
            return FluidVariant.blank();
        }

        @Override
        protected long getCapacity(FluidVariant variant) {
            return capacity;
        }

        public long getCapacity() {
            return capacity;
        }

        @Override
        protected boolean canInsert(FluidVariant variant) {
            return acceptsFluid(variant.getFluid());
        }

        @Override
        protected void onFinalCommit() {
            if (onChange != null) {
                onChange.run();
            }
        }

        public boolean acceptsFluid(Fluid fluid) {
            return fluid != null && acceptedFluids.test(fluid);
        }

        public boolean canFill(Direction side) {
            return side == null || inputSides.contains(side);
        }

        public boolean canDrain(Direction side) {
            return side == null || outputSides.contains(side);
        }

        public void setSides(Collection<Direction> inputSides, Collection<Direction> outputSides) {
            this.inputSides = inputSides;
            this.outputSides = outputSides;
        }

        /** The fluid in the tank; blank when it is empty. */
        public FluidVariant getFluid() {
            return amount <= 0 ? FluidVariant.blank() : variant;
        }

        public long getFluidAmount() {
            return amount;
        }

        public boolean isEmpty() {
            return amount <= 0;
        }

        public long getSpace() {
            return capacity - amount;
        }

        /** Replaces the contents, outside of any transaction: the machine's own bookkeeping. */
        public void setFluid(FluidVariant fluid, long amount) {
            if (fluid.isBlank() || amount <= 0) {
                this.variant = FluidVariant.blank();
                this.amount = 0;
            } else {
                this.variant = fluid;
                this.amount = Math.min(amount, capacity);
            }
        }

        /** Whether that fluid could go in at all, ignoring how much room is left. */
        public boolean canFillFluidType(FluidVariant fluid) {
            return !fluid.isBlank() && acceptsFluid(fluid.getFluid()) && (isEmpty() || variant.equals(fluid));
        }

        /**
         * Fills like a neighbour would, honouring the accepted fluids, but outside of a
         * transaction: the machine's own bookkeeping.
         *
         * @return the droplets that went in, or would have
         */
        public long fill(FluidVariant fluid, long amount, boolean doFill) {
            return fluid.isBlank() || !acceptsFluid(fluid.getFluid()) ? 0 : fillInternal(fluid, amount, doFill);
        }

        /** Fills without asking whether the tank takes that fluid, which is what the machine itself does. */
        public long fillInternal(FluidVariant fluid, long amount, boolean doFill) {
            if (fluid.isBlank() || amount <= 0 || !(isEmpty() || variant.equals(fluid))) {
                return 0;
            }
            long filled = Math.min(amount, capacity - this.amount);
            if (doFill && filled > 0) {
                this.variant = fluid;
                this.amount += filled;
            }
            return Math.max(filled, 0);
        }

        /** @return the droplets that came out, or would have */
        public long drainInternal(long maxAmount, boolean doDrain) {
            long drained = Math.min(maxAmount, amount);
            if (drained <= 0) {
                return 0;
            }
            if (doDrain) {
                amount -= drained;
                if (amount <= 0) {
                    variant = FluidVariant.blank();
                    amount = 0;
                }
            }
            return drained;
        }

        public void readFromNbt(NbtCompound nbt) {
            Fluid fluid = LiquidUtil.getFluid(nbt.getString("fluid"));
            setFluid(fluid == null ? FluidVariant.blank() : FluidVariant.of(fluid), nbt.getLong("amount"));
        }

        public NbtCompound writeToNbt(NbtCompound nbt) {
            if (!isEmpty()) {
                nbt.putString("fluid", LiquidUtil.nameOf(variant.getFluid()));
                nbt.putLong("amount", amount);
            }
            return nbt;
        }
    }
}
