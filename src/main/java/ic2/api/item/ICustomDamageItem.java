package ic2.api.item;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

public interface ICustomDamageItem {
    int getCustomDamage(ItemStack stack);
    int getMaxCustomDamage(ItemStack stack);
    void setCustomDamage(ItemStack stack, int damage);
    boolean applyCustomDamage(ItemStack stack, int damage, LivingEntity entity);
}
