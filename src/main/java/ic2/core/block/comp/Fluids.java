package ic2.core.block.comp;

import ic2.api.recipe.ILiquidAcceptManager;
import ic2.core.block.invslot.InvSlot;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Direction;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.IFluidHandler;

/**
 * Fabric port of the Fluids tile entity component: the named tanks a machine owns.
 *
 * <p>A tank is addressed by name, exactly like an {@link InvSlot} group, which is what lets the
 * guidef files place {@code <fluidtank name="input">} without knowing the machine. Each tank says
 * which sides may fill or drain it and which fluids it takes at all.
 */
public class Fluids {
    private final List<InternalFluidTank> tanks = new ArrayList<>();
    private final List<Supplier<Collection<InternalFluidTank>>> unmanagedTankHooks = new ArrayList<>();

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
                tank.setFluid(null);
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
     * The fluid handler of a side: filling goes into each tank that side may fill until the fluid
     * is used up, draining takes from the tanks that side may drain, as the 1.12.2 component did.
     */
    public IFluidHandler getHandler(Direction side) {
        return new FluidHandler(side);
    }

    private class FluidHandler implements IFluidHandler {
        private final Direction side;

        FluidHandler(Direction side) {
            this.side = side;
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            if (resource == null || resource.amount <= 0) {
                return 0;
            }

            int total = 0;
            FluidStack missing = resource.copy();
            for (InternalFluidTank tank : getAllTanks()) {
                if (tank.canFill(side)) {
                    total += tank.fill(missing, doFill);
                    missing.amount = resource.amount - total;
                    if (missing.amount <= 0) {
                        break;
                    }
                }
            }
            return total;
        }

        @Override
        public FluidStack drain(FluidStack resource, boolean doDrain) {
            if (resource == null || resource.amount <= 0) {
                return null;
            }

            FluidStack ret = new FluidStack(resource.getFluid(), 0);
            for (InternalFluidTank tank : getAllTanks()) {
                if (tank.canDrain(side)) {
                    FluidStack inTank = tank.getFluid();
                    if (inTank != null && inTank.getFluid() == resource.getFluid()) {
                        FluidStack add = tank.drain(resource.amount - ret.amount, doDrain);
                        if (add != null) {
                            ret.amount += add.amount;
                            if (ret.amount >= resource.amount) {
                                break;
                            }
                        }
                    }
                }
            }
            return ret.amount == 0 ? null : ret;
        }

        @Override
        public FluidStack drain(int maxDrain, boolean doDrain) {
            for (InternalFluidTank tank : getAllTanks()) {
                if (tank.canDrain(side)) {
                    FluidStack stack = tank.drain(maxDrain, false);
                    if (stack != null && stack.amount > 0) {
                        stack.amount = maxDrain;
                        return drain(stack, doDrain);
                    }
                }
            }
            return null;
        }
    }

    /** One named tank of a machine. */
    public static class InternalFluidTank extends FluidTank {
        public final String identifier;
        private final Predicate<Fluid> acceptedFluids;
        private Collection<Direction> inputSides;
        private Collection<Direction> outputSides;

        public InternalFluidTank(String identifier, int capacity,
                                 Collection<Direction> inputSides, Collection<Direction> outputSides,
                                 Predicate<Fluid> acceptedFluids) {
            super(capacity);
            this.identifier = identifier;
            this.inputSides = inputSides;
            this.outputSides = outputSides;
            this.acceptedFluids = acceptedFluids;
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

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            return resource != null && acceptsFluid(resource.getFluid()) ? super.fill(resource, doFill) : 0;
        }

        /** Fills without asking whether the side may, which is what the machine itself does. */
        public int fillInternal(FluidStack resource, boolean doFill) {
            return super.fill(resource, doFill);
        }

        public FluidStack drainInternal(int maxDrain, boolean doDrain) {
            return super.drain(maxDrain, doDrain);
        }

        public void readFromNbt(NbtCompound nbt) {
            String name = nbt.getString("fluid");
            int amount = nbt.getInt("amount");
            Fluid fluid = name.isEmpty() ? null : FluidRegistry.getFluid(name);

            setFluid(fluid == null || amount <= 0 ? null : new FluidStack(fluid, amount));
        }

        public NbtCompound writeToNbt(NbtCompound nbt) {
            FluidStack fluid = getFluid();
            if (fluid != null && fluid.amount > 0) {
                nbt.putString("fluid", fluid.getFluid().getName());
                nbt.putInt("amount", fluid.amount);
            }
            return nbt;
        }
    }
}
