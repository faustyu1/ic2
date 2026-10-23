package ic2.api.info;

import net.minecraft.item.ItemStack;

public interface IInfoProvider {
    double getEnergyValue(ItemStack stack);
    int getFuelValue(ItemStack stack, boolean allowOverload);
}
