package ic2.api.upgrade;

import net.minecraft.item.ItemStack;

public interface IRedstoneSensitiveUpgrade extends IUpgradeItem {
    boolean modifiesRedstoneInput(ItemStack stack, IUpgradableBlock parent);
    int getRedstoneInput(ItemStack stack, IUpgradableBlock parent, int original);
}
