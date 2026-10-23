package ic2.core.item;

import ic2.api.item.ICustomDamageItem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Fabric stub of decompiled ic2.core.item.DamageHandler (1.12.2).
 * Original handled ICustomDamageItem / IPseudoDamageItem + vanilla ItemStack damage.
 * Fabric 1.21.1 port: Yarn mappings (ItemStack#getDamage/setDamage/getMaxDamage),
 * IPseudoDamageItem branch removed (interface not ported yet) — delegated to vanilla.
 * Keeps API: getDamage / setDamage / getMaxDamage / damage / handleDamage.
 */
public class DamageHandler {

    public static int getDamage(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        Item item = stack.getItem();
        if (item instanceof ICustomDamageItem c) return c.getCustomDamage(stack);
        return stack.getDamage();
    }

    public static void setDamage(ItemStack stack, int damage, boolean displayOnly) {
        if (stack == null || stack.isEmpty()) return;
        Item item = stack.getItem();
        if (item instanceof ICustomDamageItem c) {
            c.setCustomDamage(stack, damage);
            return;
        }
        // IPseudoDamageItem not ported in Fabric stub — treat as vanilla displayOnly check
        if (displayOnly) {
            // vanilla displayOnly: just set visual damage without breaking
            stack.setDamage(damage);
        } else {
            stack.setDamage(damage);
        }
    }

    public static int getMaxDamage(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        Item item = stack.getItem();
        if (item instanceof ICustomDamageItem c) return c.getMaxCustomDamage(stack);
        return stack.getMaxDamage();
    }

    public static boolean damage(ItemStack stack, int damage, LivingEntity src) {
        if (stack == null || stack.isEmpty()) return false;
        Item item = stack.getItem();
        if (item instanceof ICustomDamageItem c) return c.applyCustomDamage(stack, damage, src);
        // vanilla path: increment damage, break if exceeds max
        if (stack.isDamageable()) {
            int cur = stack.getDamage();
            int max = stack.getMaxDamage();
            int next = cur + damage;
            if (next >= max) {
                stack.setCount(0);
            } else {
                stack.setDamage(next);
            }
            return true;
        }
        // non-damageable stub: no-op
        return false;
    }

    /**
     * Requested Fabric stub API: handleDamage(ItemStack, int, LivingEntity).
     * Delegates to {@link #damage(ItemStack, int, LivingEntity)}.
     */
    public static void handleDamage(ItemStack stack, int amount, LivingEntity entity) {
        damage(stack, amount, entity);
    }
}
