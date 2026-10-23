package ic2.core.item;

import java.util.List;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

/**
 * An item that carries a tank of its own, filled bit by bit rather than all at once - the fuel
 * jetpack, the CF pack and the foam sprayer. 1.12.2 gave such items a Forge fluid handler
 * capability ({@code CapabilityFluidHandlerItem}); here the item answers for itself and keeps the
 * fluid in {@code CUSTOM_DATA} under {@code Fluid}, in the same {@code fluid}/{@code amount} form a
 * tank block writes.
 *
 * <p>The machines' fluid slots and the tank block move fluid in and out of these through
 * {@link ic2.core.util.FluidContainers}, next to the buckets and cells.
 */
public interface IItemFluidTank {
    String TAG = "Fluid";

    /** How many mB the item holds when full. */
    int getCapacity(ItemStack stack);

    /** Whether the item takes that fluid at all. */
    boolean canHold(ItemStack stack, Fluid fluid);

    /** The fluid the creative tab lists a full copy of, or null for none. */
    default Fluid getCreativeFluid() {
        return null;
    }

    /** @return the content, or null when the item is empty */
    default FluidStack getFluid(ItemStack stack) {
        NbtCompound data = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        NbtCompound tag = data.getCompound(TAG);
        String name = tag.getString("fluid");
        int amount = tag.getInt("amount");
        Fluid fluid = name.isEmpty() ? null : FluidRegistry.getFluid(name);
        return fluid == null || amount <= 0 ? null : new FluidStack(fluid, amount);
    }

    default void setFluid(ItemStack stack, FluidStack fluid) {
        NbtCompound data = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        if (fluid == null || fluid.amount <= 0) {
            data.remove(TAG);
        } else {
            NbtCompound tag = new NbtCompound();
            tag.putString("fluid", fluid.getFluid().getName());
            tag.putInt("amount", fluid.amount);
            data.put(TAG, tag);
        }
        if (data.isEmpty()) {
            stack.remove(DataComponentTypes.CUSTOM_DATA);
        } else {
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(data));
        }
    }

    default int getAmount(ItemStack stack) {
        FluidStack fluid = getFluid(stack);
        return fluid == null ? 0 : fluid.amount;
    }

    default boolean isEmpty(ItemStack stack) {
        return getFluid(stack) == null;
    }

    /**
     * Pours as much of {@code resource} in as fits.
     *
     * @return how many mB went in, or would have
     */
    default int fill(ItemStack stack, FluidStack resource, boolean doFill) {
        if (resource == null || resource.amount <= 0 || !canHold(stack, resource.getFluid())) {
            return 0;
        }

        FluidStack content = getFluid(stack);
        if (content != null && !content.getFluid().equals(resource.getFluid())) {
            return 0;
        }

        int stored = content == null ? 0 : content.amount;
        int filled = Math.min(resource.amount, getCapacity(stack) - stored);
        if (filled > 0 && doFill) {
            setFluid(stack, new FluidStack(resource.getFluid(), stored + filled));
        }
        return Math.max(filled, 0);
    }

    /**
     * Takes up to {@code maxDrain} mB out.
     *
     * @return what came out, or would have, or null when nothing did
     */
    default FluidStack drain(ItemStack stack, int maxDrain, boolean doDrain) {
        FluidStack content = getFluid(stack);
        if (content == null || maxDrain <= 0) {
            return null;
        }

        int drained = Math.min(maxDrain, content.amount);
        if (doDrain) {
            setFluid(stack, new FluidStack(content.getFluid(), content.amount - drained));
        }
        return new FluidStack(content.getFluid(), drained);
    }

    /** Fills the item to the brim with the fluid; the creative tab lists such a copy. */
    default ItemStack filledCopy(ItemStack stack, Fluid fluid) {
        ItemStack copy = stack.copy();
        setFluid(copy, new FluidStack(fluid, getCapacity(copy)));
        return copy;
    }

    // ---- what the item shows, shared by the armour and the tool hierarchies ----

    /** The bar length, 0..13 as vanilla expects; it tracks the fluid, as the damage did in 1.12.2. */
    default int fluidBarStep(ItemStack stack) {
        int capacity = getCapacity(stack);
        return capacity <= 0 ? 0 : MathHelper.clamp(Math.round(13.0F * getAmount(stack) / capacity), 0, 13);
    }

    default int fluidBarColor(ItemStack stack) {
        FluidStack fluid = getFluid(stack);
        return fluid == null ? 0x7F7F7F : fluid.getFluid().getColor() & 0xFFFFFF;
    }

    /** {@code < Biogas, 1000 mB >}, or {@code < Empty >}. */
    default void appendFluidTooltip(ItemStack stack, List<Text> tooltip) {
        FluidStack fluid = getFluid(stack);
        if (fluid == null) {
            tooltip.add(Text.translatable("item.FluidContainer.Empty"));
        } else {
            tooltip.add(Text.literal("< ").append(ic2.core.util.FluidContainers.displayName(fluid.getFluid()))
                    .append(Text.literal(", " + fluid.amount + " mB >")));
        }
    }
}
