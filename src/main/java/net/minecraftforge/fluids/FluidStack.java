package net.minecraftforge.fluids;

/**
 * Minimal shim for Forge's FluidStack.
 * Holds a Fluid reference and amount (in mB).
 */
public class FluidStack {
    public final Fluid fluid;
    public int amount;

    public FluidStack(Fluid fluid, int amount) {
        if (fluid == null) throw new IllegalArgumentException("fluid cannot be null");
        this.fluid = fluid;
        this.amount = amount;
    }

    public Fluid getFluid() {
        return fluid;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public boolean isFluidEqual(FluidStack other) {
        if (other == null) return false;
        if (this.fluid == other.fluid) return true;
        return this.fluid.getName().equals(other.fluid.getName());
    }

    public FluidStack copy() {
        return new FluidStack(fluid, amount);
    }

    @Override
    public String toString() {
        return "FluidStack[" + fluid.getName() + " * " + amount + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof FluidStack other)) return false;
        return amount == other.amount && fluid.equals(other.fluid);
    }

    @Override
    public int hashCode() {
        return 31 * fluid.hashCode() + amount;
    }
}
