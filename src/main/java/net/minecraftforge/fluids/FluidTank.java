package net.minecraftforge.fluids;

/**
 * Minimal shim for Forge's FluidTank.
 */
public class FluidTank {
    private FluidStack fluid;
    private final int capacity;

    public FluidTank(int capacity) {
        this.capacity = capacity;
    }

    public FluidTank(FluidStack fluidStack, int capacity) {
        this.fluid = fluidStack;
        this.capacity = capacity;
    }

    public FluidStack getFluid() {
        return fluid;
    }

    public int getFluidAmount() {
        return fluid == null ? 0 : fluid.amount;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setFluid(FluidStack fluid) {
        this.fluid = fluid;
    }

    /**
     * Whether that fluid could go in at all, ignoring how much room is left. The reactor asks
     * before it converts heat, so it does not run the conversion for a fluid the tank would
     * refuse anyway.
     */
    public boolean canFillFluidType(FluidStack resource) {
        return resource != null && (fluid == null || fluid.isFluidEqual(resource));
    }

    public int fill(FluidStack resource, boolean doFill) {
        if (resource == null || resource.amount <= 0) return 0;
        if (fluid == null) {
            int fillAmount = Math.min(capacity, resource.amount);
            if (doFill && fillAmount > 0) fluid = new FluidStack(resource.fluid, fillAmount);
            return fillAmount;
        }
        if (!fluid.isFluidEqual(resource)) return 0;
        int fillAmount = Math.min(capacity - fluid.amount, resource.amount);
        if (doFill) fluid.amount += fillAmount;
        return fillAmount;
    }

    public FluidStack drain(int maxDrain, boolean doDrain) {
        if (fluid == null) return null;
        int drainAmount = Math.min(fluid.amount, maxDrain);
        FluidStack drained = new FluidStack(fluid.fluid, drainAmount);
        if (doDrain) {
            fluid.amount -= drainAmount;
            if (fluid.amount <= 0) fluid = null;
        }
        return drained;
    }
}
