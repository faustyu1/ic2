package ic2.core.item;

import net.minecraft.item.ItemStack;

/**
 * Fabric port stub of ItemCoke — fuel item (coal coke). Burn time 3200 (1.6x coal).
 * In 1.21.1 Fabric burn time is via FuelRegistry; this stub keeps the legacy
 * getItemBurnTime / getBurnTime for compat and registers via FuelRegistry in IC2Items.
 */
public class ItemCoke extends ItemIC2 {
    public ItemCoke() {
        super("coke");
    }

    public int getBurnTime() {
        return 3200;
    }

    public int getItemBurnTime(ItemStack stack) {
        return 3200;
    }
}
