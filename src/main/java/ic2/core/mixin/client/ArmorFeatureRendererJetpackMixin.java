package ic2.core.mixin.client;

import ic2.core.item.armor.jetpack.JetpackHandler;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * A chestplate with a jetpack attached is drawn as the electric jetpack, as the 1.12.2
 * {@code LayerJetpackOverride} did.
 */
@Mixin(ArmorFeatureRenderer.class)
public abstract class ArmorFeatureRendererJetpackMixin {

    @Redirect(method = "renderArmor", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/LivingEntity;getEquippedStack(Lnet/minecraft/entity/EquipmentSlot;)Lnet/minecraft/item/ItemStack;"))
    private ItemStack ic2$jetpackOverride(LivingEntity entity, EquipmentSlot slot) {
        ItemStack stack = entity.getEquippedStack(slot);
        return slot == EquipmentSlot.CHEST && JetpackHandler.hasJetpackAttached(stack) ? JetpackHandler.jetpack() : stack;
    }
}
