package ic2.core.mixin;

import ic2.core.item.armor.IArmorTickItem;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces Forge's {@code Item.onArmorTick}: every tick, each worn IC2 piece that wants a tick
 * gets one. This is what keeps the solar helmet charging, the static boots paying out and the
 * quantum helmet refilling air.
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityArmorTickMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void ic2$armorTick(CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) continue;

            ItemStack stack = self.getEquippedStack(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof IArmorTickItem armor)) continue;

            armor.onArmorTick(self.getWorld(), self, stack);
        }

        // a jetpack crafted onto a chestplate flies whatever the chestplate is
        ic2.core.item.armor.jetpack.JetpackHandler.tick(self);
    }
}
