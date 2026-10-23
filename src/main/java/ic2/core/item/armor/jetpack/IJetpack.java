package ic2.core.item.armor.jetpack;

import net.minecraft.item.ItemStack;

/**
 * Port of {@code ic2.core.item.armor.jetpack.IJetpack}: what {@link JetpackLogic} needs to know
 * about whatever is worn in the chest slot to fly with it.
 */
public interface IJetpack {
    /** EU an electric jetpack spends on top of the thrust cost, per tick of flight. */
    int EU_ENERGY_INCREASE = 6;

    boolean drainEnergy(ItemStack stack, int amount);

    float getPower(ItemStack stack);

    /** The charge level below which the thrust starts fading out. */
    float getDropPercentage(ItemStack stack);

    double getChargeLevel(ItemStack stack);

    boolean isJetpackActive(ItemStack stack);

    float getHoverMultiplier(ItemStack stack, boolean upwards);

    /** World height divided by this is as high as the jetpack will carry its wearer. */
    float getWorldHeightDivisor(ItemStack stack);
}
