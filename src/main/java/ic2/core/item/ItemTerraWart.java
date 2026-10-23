package ic2.core.item;

import ic2.core.IC2Potion;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Rarity;
import net.minecraft.world.World;

/**
 * Fabric port of {@code ic2.core.item.ItemTerraWart}: eating one clears the eight harmful vanilla
 * effects outright and takes 30 seconds off the radiation.
 *
 * <p>Food values are the 1.12.2 ones - no hunger restored, saturation 1, always edible.
 */
public class ItemTerraWart extends ItemIC2 {

    /** What a wart cures outright, in the order 1.12.2 removed them. */
    private static final RegistryEntry<net.minecraft.entity.effect.StatusEffect>[] CURED = cured();

    public ItemTerraWart() {
        super("terra_wart", new Settings()
                .rarity(Rarity.RARE)
                .food(new FoodComponent.Builder().nutrition(0).saturationModifier(1.0f).alwaysEdible().build()));
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        for (RegistryEntry<net.minecraft.entity.effect.StatusEffect> effect : CURED) {
            user.removeStatusEffect(effect);
        }

        if (IC2Potion.radiationEntry != null) {
            StatusEffectInstance radiation = user.getStatusEffect(IC2Potion.radiationEntry);
            if (radiation != null) {
                user.removeStatusEffect(IC2Potion.radiationEntry);
                if (radiation.getDuration() > 600) {
                    IC2Potion.applyTo(user, radiation.getDuration() - 600, radiation.getAmplifier());
                }
            }
        }

        return super.finishUsing(stack, world, user);
    }

    @SuppressWarnings("unchecked")
    private static RegistryEntry<net.minecraft.entity.effect.StatusEffect>[] cured() {
        return new RegistryEntry[] {
                StatusEffects.NAUSEA,
                StatusEffects.MINING_FATIGUE,
                StatusEffects.HUNGER,
                StatusEffects.SLOWNESS,
                StatusEffects.WEAKNESS,
                StatusEffects.BLINDNESS,
                StatusEffects.POISON,
                StatusEffects.WITHER,
        };
    }
}
