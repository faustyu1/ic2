package ic2.core.util;

import ic2.core.init.IC2ItemVariants;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/**
 * What an item holds and what is left of it once it is emptied.
 *
 * <p>IC2 knows two kinds of fluid container: the vanilla bucket and its own cell. The machines
 * handle the two kinds directly, because the flattened cells carry their fluid in the item id
 * ({@code ic2:fluid_cell_lava}) rather than in item data; other mods reach the cells through
 * {@link ic2.core.item.ItemFluidStorages}.
 */
public final class FluidContainers {
    /** A bucket holds one bucket, and so does a cell - both in droplets. */
    public static final long BUCKET = FluidConstants.BUCKET;
    public static final long CELL = FluidConstants.BUCKET;

    /** Fluid names of the vanilla buckets. */
    private static final Map<Item, String> BUCKETS = new LinkedHashMap<>();

    static {
        BUCKETS.put(Items.WATER_BUCKET, "water");
        BUCKETS.put(Items.LAVA_BUCKET, "lava");
        BUCKETS.put(Items.MILK_BUCKET, "ic2milk");
    }

    private FluidContainers() {}

    /** @return what the container holds, or null when the item is not a full container */
    public static ResourceAmount<FluidVariant> contentOf(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        String bucket = BUCKETS.get(stack.getItem());
        if (bucket != null) {
            return new ResourceAmount<>(FluidVariant.of(LiquidUtil.getFluid(bucket)), BUCKET);
        }

        Fluid cell = LiquidUtil.getFluid(cellFluidName(stack));
        return cell == null ? null : new ResourceAmount<>(FluidVariant.of(cell), CELL);
    }

    /** @return what is left of the container once it is emptied */
    public static ItemStack emptied(ItemStack stack) {
        if (BUCKETS.containsKey(stack.getItem())) {
            return new ItemStack(Items.BUCKET);
        }

        if (cellFluidName(stack) != null) {
            Item empty = IC2ItemVariants.get("fluid_cell", null);
            return empty == null ? ItemStack.EMPTY : new ItemStack(empty);
        }

        return ItemStack.EMPTY;
    }

    /**
     * Fills an empty container.
     *
     * @return the full container, or an empty stack when that fluid has none of that kind
     */
    public static ItemStack filled(ItemStack empty, Fluid fluid) {
        if (empty.isEmpty() || fluid == null) {
            return ItemStack.EMPTY;
        }

        if (empty.isOf(Items.BUCKET)) {
            for (Map.Entry<Item, String> entry : BUCKETS.entrySet()) {
                if (entry.getValue().equals(LiquidUtil.nameOf(fluid))) {
                    return new ItemStack(entry.getKey());
                }
            }
            return ItemStack.EMPTY;
        }

        Item emptyCell = IC2ItemVariants.get("fluid_cell", null);
        if (emptyCell != null && empty.isOf(emptyCell)) {
            Item cell = IC2ItemVariants.get("fluid_cell", LiquidUtil.nameOf(fluid));
            return cell == null ? ItemStack.EMPTY : new ItemStack(cell);
        }

        return ItemStack.EMPTY;
    }

    /** How many droplets a container of this kind takes at once. */
    public static long capacityOf(ItemStack stack) {
        if (stack.isOf(Items.BUCKET) || BUCKETS.containsKey(stack.getItem())) {
            return BUCKET;
        }

        Item emptyCell = IC2ItemVariants.get("fluid_cell", null);
        return emptyCell != null && (stack.isOf(emptyCell) || cellFluidName(stack) != null) ? CELL : 0;
    }

    /** A bucket, a cell or an item with a tank of its own. */
    public static boolean isFluidContainer(ItemStack stack) {
        return capacityOf(stack) > 0 || tankOf(stack) != null;
    }

    /**
     * The fluid an IC2 cell holds, read off the variant the item was registered as.
     *
     * @return the fluid name, or null when the item is not a full cell
     */
    private static String cellFluidName(ItemStack stack) {
        for (Map.Entry<String, Item> entry : IC2ItemVariants.getAll().entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("fluid_cell#") && stack.isOf(entry.getValue())) {
                return key.substring("fluid_cell#".length());
            }
        }
        return null;
    }

    /** The item's own tank, or null when the item is no partly fillable container. */
    public static ic2.core.item.IItemFluidTank tankOf(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ic2.core.item.IItemFluidTank tank ? tank : null;
    }

    /** The name a player sees for a fluid: the IC2 ones from the language files, the rest as vanilla names its block. */
    public static net.minecraft.text.Text displayName(Fluid fluid) {
        return FluidVariantAttributes.getName(FluidVariant.of(fluid));
    }
}
