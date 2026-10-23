package ic2.core.util;

import ic2.core.init.IC2ItemVariants;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

/**
 * What an item holds and what is left of it once it is emptied.
 *
 * <p>IC2 knows two kinds of fluid container: the vanilla bucket and its own cell. Forge asked the
 * item for a fluid handler capability; here the two kinds are handled directly, because the
 * flattened cells carry their fluid in the item id ({@code ic2:fluid_cell_lava}) rather than in
 * item data.
 */
public final class FluidContainers {
    /** A bucket holds one bucket, a cell holds a thousandth of nothing else - both in mB. */
    public static final int BUCKET = 1000;
    public static final int CELL = 1000;

    /** Fluid names of the vanilla buckets. */
    private static final Map<Item, String> BUCKETS = new LinkedHashMap<>();

    static {
        BUCKETS.put(Items.WATER_BUCKET, "water");
        BUCKETS.put(Items.LAVA_BUCKET, "lava");
        BUCKETS.put(Items.MILK_BUCKET, "ic2milk");
    }

    private FluidContainers() {}

    /** @return what the container holds, or null when the item is not a full container */
    public static FluidStack contentOf(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        String bucket = BUCKETS.get(stack.getItem());
        if (bucket != null) {
            return new FluidStack(fluid(bucket), BUCKET);
        }

        String cell = cellFluidName(stack);
        return cell == null ? null : new FluidStack(fluid(cell), CELL);
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
                if (entry.getValue().equals(fluid.getName())) {
                    return new ItemStack(entry.getKey());
                }
            }
            return ItemStack.EMPTY;
        }

        Item emptyCell = IC2ItemVariants.get("fluid_cell", null);
        if (emptyCell != null && empty.isOf(emptyCell)) {
            Item cell = IC2ItemVariants.get("fluid_cell", fluid.getName());
            return cell == null ? ItemStack.EMPTY : new ItemStack(cell);
        }

        return ItemStack.EMPTY;
    }

    /** How much a container of this kind takes at once. */
    public static int capacityOf(ItemStack stack) {
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
        String name = fluid.getName();
        return net.minecraft.text.Text.translatable(name.startsWith("ic2") ? name.substring(3) : "block.minecraft." + name);
    }

    private static Fluid fluid(String name) {
        Fluid known = FluidRegistry.getRegisteredFluids().get(name);
        if (known != null) {
            return known;
        }

        Fluid fluid = new Fluid(name);
        FluidRegistry.registerFluid(fluid);
        return fluid;
    }
}
