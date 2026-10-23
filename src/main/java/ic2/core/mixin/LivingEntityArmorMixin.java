package ic2.core.mixin;

import ic2.api.item.ISpecialArmor;
import ic2.core.item.armor.ItemArmorElectric;
import ic2.core.item.armor.ItemArmorHazmat;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gives IC2 armour the two hooks Forge gave it in 1.12.2 and Fabric has no equivalent for:
 * {@code ISpecialArmor} absorption and {@code LivingFallEvent}.
 *
 * <p>The absorption runs after vanilla armour has taken its share: the nano and quantum suits
 * reduce damage with their armour points first and then absorb what is left with energy.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityArmorMixin {

    @Inject(method = "applyArmorToDamage", at = @At("RETURN"), cancellable = true)
    private void ic2$applySpecialArmor(DamageSource source, float amount, CallbackInfoReturnable<Float> cir) {
        float remaining = cir.getReturnValue();
        if (remaining <= 0.0f) return;

        LivingEntity self = (LivingEntity) (Object) this;
        float result = ISpecialArmor.applyArmor(self, source, remaining);
        if (result != remaining) {
            cir.setReturnValue(result);
        }
    }

    /**
     * The boots of the hazmat, nano and quantum sets swallow a fall outright, paying with
     * durability or charge, which is what the 1.12.2 fall handlers did.
     */
    @Inject(method = "handleFallDamage", at = @At("HEAD"), cancellable = true)
    private void ic2$absorbFall(float fallDistance, float damageMultiplier, DamageSource source,
                                CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.getWorld().isClient) return;

        ItemStack boots = self.getEquippedStack(EquipmentSlot.FEET);
        if (boots.isEmpty()) return;

        boolean absorbed = switch (boots.getItem()) {
            case ItemArmorHazmat hazmat -> hazmat.absorbFallDamage(self, boots, fallDistance);
            case ItemArmorElectric electric -> electric.absorbFallDamage(self, boots, fallDistance);
            default -> false;
        };

        if (absorbed) {
            cir.setReturnValue(false);
        }
    }
}
