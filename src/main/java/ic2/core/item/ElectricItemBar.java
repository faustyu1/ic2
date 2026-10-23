package ic2.core.item;

import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;

/**
 * The durability bar an electric item shows instead of the vanilla damage bar: it tracks EU, not
 * wear. 1.12.2 did this through {@code IPseudoDamageItem} and the item's own damage value; here
 * the charge lives in a data component, so the bar is computed from it on the spot.
 *
 * <p>Both the item classes and the armour classes need the same three answers, and they sit in
 * different hierarchies, so the arithmetic lives here.
 */
public final class ElectricItemBar {

    private ElectricItemBar() {}

    /** True for an electric item that can hold a charge at all. */
    public static boolean isElectric(ItemStack stack) {
        return stack.getItem() instanceof IElectricItem item && item.getMaxCharge(stack) > 0.0;
    }

    /** A charged item shows a bar; so does an empty one, otherwise an empty tool looks ordinary. */
    public static boolean visible(ItemStack stack) {
        return isElectric(stack);
    }

    /** The bar length, 0..13 as vanilla expects. */
    public static int step(ItemStack stack) {
        return MathHelper.clamp((int) Math.round(fraction(stack) * 13.0), 0, 13);
    }

    /** Red when nearly empty through green when full, as the vanilla durability bar does. */
    public static int color(ItemStack stack) {
        return MathHelper.hsvToRgb((float) fraction(stack) / 3.0f, 1.0f, 1.0f);
    }

    private static double fraction(ItemStack stack) {
        if (!(stack.getItem() instanceof IElectricItem item)) return 0.0;
        double max = item.getMaxCharge(stack);
        if (max <= 0.0 || ElectricItem.manager == null) return 0.0;
        return MathHelper.clamp(ElectricItem.manager.getCharge(stack) / max, 0.0, 1.0);
    }
}
