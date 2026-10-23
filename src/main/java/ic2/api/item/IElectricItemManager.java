package ic2.api.item;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

public interface IElectricItemManager {
    double charge(ItemStack stack, double amount, int tier, boolean ignoreTransferLimit, boolean simulate);
    double discharge(ItemStack stack, double amount, int tier, boolean ignoreTransferLimit, boolean externally, boolean simulate);
    double getCharge(ItemStack stack);
    double getMaxCharge(ItemStack stack);
    boolean canUse(ItemStack stack, double amount);
    boolean use(ItemStack stack, double amount, LivingEntity entity);
    void chargeFromArmor(ItemStack stack, LivingEntity entity);
    String getToolTip(ItemStack stack);
    int getTier(ItemStack stack);
}
