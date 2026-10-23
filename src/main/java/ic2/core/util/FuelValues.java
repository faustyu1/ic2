package ic2.core.util;

import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/**
 * Burn value of an item, replacing the {@code Info.itemInfo.getFuelValue} hook of 1.12.2.
 *
 * <p>The value is the vanilla burn time in ticks, so the generator keeps the balance the original
 * had. Lava is opt-in: the solid fuel generator refuses a lava bucket, the fluid burning ones take
 * it.
 */
public final class FuelValues {
    private FuelValues() {}

    public static int get(ItemStack stack, boolean allowLava) {
        if (stack.isEmpty()) {
            return 0;
        }
        if (!allowLava && stack.isOf(Items.LAVA_BUCKET)) {
            return 0;
        }

        Integer value = FuelRegistry.INSTANCE.get(stack.getItem());
        return value == null ? 0 : value;
    }
}
