package ic2.api.item;

import net.minecraft.item.ItemStack;

public interface IElectricItem {
    boolean canProvideEnergy(ItemStack stack);

    double getMaxCharge(ItemStack stack);

    int getTier(ItemStack stack);

    double getTransferLimit(ItemStack stack);
}
