package ic2.api.item;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

public interface IHazmatLike {
    boolean addsProtection(LivingEntity entity, EquipmentSlot slot, ItemStack stack);
    default boolean fullyProtects(LivingEntity entity, EquipmentSlot slot, ItemStack stack) { return false; }

    static boolean hasCompleteHazmat(LivingEntity living) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                ItemStack stack = living.getEquippedStack(slot);
                if (stack.isEmpty() || !(stack.getItem() instanceof IHazmatLike)) return false;
                IHazmatLike hazmat = (IHazmatLike) stack.getItem();
                if (!hazmat.addsProtection(living, slot, stack)) return false;
                if (hazmat.fullyProtects(living, slot, stack)) return true;
            }
        }
        return true;
    }
}
