package ic2.api.upgrade;

import net.minecraft.item.ItemStack;

public interface IEnergyStorageUpgrade extends IUpgradeItem {
    int getExtraEnergyStorage(ItemStack stack, IUpgradableBlock parent);
    double getEnergyStorageMultiplier(ItemStack stack, IUpgradableBlock parent);
}
