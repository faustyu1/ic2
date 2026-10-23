package ic2.api.upgrade;

import net.minecraft.item.ItemStack;

public interface IProcessingUpgrade extends IUpgradeItem {
    int getExtraProcessTime(ItemStack stack, IUpgradableBlock parent);
    double getProcessTimeMultiplier(ItemStack stack, IUpgradableBlock parent);
    int getExtraEnergyDemand(ItemStack stack, IUpgradableBlock parent);
    double getEnergyDemandMultiplier(ItemStack stack, IUpgradableBlock parent);
}
