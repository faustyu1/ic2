package ic2.core.item.armor.jetpack;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

/**
 * Port of {@code ic2.core.item.armor.jetpack.IBoostingJetpack}: a jetpack with a separate boost
 * reserve on top of its normal thrust. Nothing in IC2 itself implements it; it is there for the
 * add-ons, and {@link JetpackLogic} honours it.
 */
public interface IBoostingJetpack extends IJetpack {
    float getBaseThrust(ItemStack stack, boolean hoverMode);

    float getBoostThrust(PlayerEntity player, ItemStack stack, boolean hoverMode);

    boolean useBoostPower(ItemStack stack, float amount);

    float getHoverBoost(PlayerEntity player, ItemStack stack, boolean upwards);
}
