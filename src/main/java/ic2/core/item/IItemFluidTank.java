package ic2.core.item;

import ic2.core.util.LiquidUtil;
import java.util.List;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

/**
 * An item that carries a tank of its own, filled bit by bit rather than all at once - the fuel
 * jetpack, the CF pack and the foam sprayer. 1.12.2 gave such items a Forge fluid handler
 * capability ({@code CapabilityFluidHandlerItem}); here the item answers for itself and keeps the
 * fluid in {@code CUSTOM_DATA} under {@code Fluid}, in the same {@code fluid}/{@code amount} form a
 * tank block writes. Amounts are droplets.
 *
 * <p>The machines' fluid slots and the tank block move fluid in and out of these through
 * {@link ic2.core.util.FluidContainers}, next to the buckets and cells; other mods reach them as
 * Fabric item fluid storage ({@link ItemFluidStorages}).
 */
public interface IItemFluidTank {
    String TAG = "Fluid";

    /** How many droplets the item holds when full. */
    long getCapacity(ItemStack stack);

    /** Whether the item takes that fluid at all. */
    boolean canHold(ItemStack stack, Fluid fluid);

    /** The fluid the creative tab lists a full copy of, or null for none. */
    default Fluid getCreativeFluid() {
        return null;
    }

    /** @return the content in droplets, or null when the item is empty */
    default ResourceAmount<FluidVariant> getFluid(ItemStack stack) {
        NbtCompound data = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        NbtCompound tag = data.getCompound(TAG);
        Fluid fluid = LiquidUtil.getFluid(tag.getString("fluid"));
        long amount = tag.getLong("amount");
        return fluid == null || amount <= 0 ? null : new ResourceAmount<>(FluidVariant.of(fluid), amount);
    }

    default void setFluid(ItemStack stack, FluidVariant fluid, long amount) {
        NbtCompound data = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        if (fluid.isBlank() || amount <= 0) {
            data.remove(TAG);
        } else {
            NbtCompound tag = new NbtCompound();
            tag.putString("fluid", LiquidUtil.nameOf(fluid.getFluid()));
            tag.putLong("amount", amount);
            data.put(TAG, tag);
        }
        if (data.isEmpty()) {
            stack.remove(DataComponentTypes.CUSTOM_DATA);
        } else {
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(data));
        }
    }

    default long getAmount(ItemStack stack) {
        ResourceAmount<FluidVariant> fluid = getFluid(stack);
        return fluid == null ? 0 : fluid.amount();
    }

    default boolean isEmpty(ItemStack stack) {
        return getFluid(stack) == null;
    }

    /**
     * Pours as many droplets of the fluid in as fit.
     *
     * @return how many droplets went in, or would have
     */
    default long fill(ItemStack stack, FluidVariant fluid, long amount, boolean doFill) {
        if (fluid.isBlank() || amount <= 0 || !canHold(stack, fluid.getFluid())) {
            return 0;
        }

        ResourceAmount<FluidVariant> content = getFluid(stack);
        if (content != null && !content.resource().equals(fluid)) {
            return 0;
        }

        long stored = content == null ? 0 : content.amount();
        long filled = Math.min(amount, getCapacity(stack) - stored);
        if (filled > 0 && doFill) {
            setFluid(stack, fluid, stored + filled);
        }
        return Math.max(filled, 0);
    }

    /**
     * Takes up to {@code maxDrain} droplets out.
     *
     * @return what came out, or would have, or null when nothing did
     */
    default ResourceAmount<FluidVariant> drain(ItemStack stack, long maxDrain, boolean doDrain) {
        ResourceAmount<FluidVariant> content = getFluid(stack);
        if (content == null || maxDrain <= 0) {
            return null;
        }

        long drained = Math.min(maxDrain, content.amount());
        if (doDrain) {
            setFluid(stack, content.resource(), content.amount() - drained);
        }
        return new ResourceAmount<>(content.resource(), drained);
    }

    /** Fills the item to the brim with the fluid; the creative tab lists such a copy. */
    default ItemStack filledCopy(ItemStack stack, Fluid fluid) {
        ItemStack copy = stack.copy();
        setFluid(copy, FluidVariant.of(fluid), getCapacity(copy));
        return copy;
    }

    // ---- what the item shows, shared by the armour and the tool hierarchies ----

    /** The bar length, 0..13 as vanilla expects; it tracks the fluid, as the damage did in 1.12.2. */
    default int fluidBarStep(ItemStack stack) {
        long capacity = getCapacity(stack);
        return capacity <= 0 ? 0 : MathHelper.clamp(Math.round(13.0F * getAmount(stack) / capacity), 0, 13);
    }

    default int fluidBarColor(ItemStack stack) {
        ResourceAmount<FluidVariant> fluid = getFluid(stack);
        return fluid == null ? 0x7F7F7F : LiquidUtil.colorOf(fluid.resource().getFluid()) & 0xFFFFFF;
    }

    /** {@code < Biogas, 1000 mB >}, or {@code < Empty >}. */
    default void appendFluidTooltip(ItemStack stack, List<Text> tooltip) {
        ResourceAmount<FluidVariant> fluid = getFluid(stack);
        if (fluid == null) {
            tooltip.add(Text.translatable("item.FluidContainer.Empty"));
        } else {
            tooltip.add(Text.literal("< ").append(ic2.core.util.FluidContainers.displayName(fluid.resource().getFluid()))
                    .append(Text.literal(", " + LiquidUtil.toMb(fluid.amount()) + " mB >")));
        }
    }
}
