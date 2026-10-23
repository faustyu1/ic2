package net.minecraftforge.fluids.capability;

import net.minecraftforge.fluids.FluidStack;

/**
 * Minimal shim for Forge's IFluidHandler: what one side of a block offers to fill or drain.
 *
 * <p>Forge handed these out as a capability; in the port a block gives its handler for a side
 * through {@code TileEntityBlock.getFluidHandler}, and {@link ic2.core.util.LiquidUtil} is the
 * one place that asks for it.
 */
public interface IFluidHandler {
    /** @return how much of the fluid was (or, with doFill false, would be) taken */
    int fill(FluidStack resource, boolean doFill);

    /** Drains up to the amount of exactly that fluid; null when none came out. */
    FluidStack drain(FluidStack resource, boolean doDrain);

    /** Drains up to maxDrain of whatever comes first; null when none came out. */
    FluidStack drain(int maxDrain, boolean doDrain);
}
