package ic2.core.block.transport.cover;

import java.util.Set;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;

/**
 * An item that sits on the side of a cover holder once placed, ticks with it and decides what may
 * pass through that side.
 *
 * <p>1.12.2 kept the side the cover sat on in the stack's NBT and listed the Forge capabilities it
 * provided; the port hands the side to {@link #onTick} instead, and has no capabilities.
 */
public interface ICoverItem {
    boolean isSuitableFor(ItemStack stack, Set<CoverProperty> properties);

    /** One tick of the cover on {@code side} of the holder; runs on the server. */
    boolean onTick(ItemStack stack, ICoverHolder holder, Direction side);

    boolean allowsInput(ItemStack stack);

    boolean allowsInput(FluidVariant fluid);

    boolean allowsOutput(ItemStack stack);

    boolean allowsOutput(FluidVariant fluid);
}
