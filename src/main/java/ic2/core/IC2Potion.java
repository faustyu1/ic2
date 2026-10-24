package ic2.core;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

/**
 * Fabric port of {@code ic2.core.IC2Potion}: the radiation effect a bare uranium stack gives the
 * player carrying it.
 *
 * <p>1.12.2 subclassed {@code Potion} and overrode {@code performEffect} / {@code isReady};
 * 1.21.1 calls those {@code applyUpdateEffect} and {@code canApplyUpdateEffect}, and the damage
 * goes through the {@code ic2:radiation} damage type instead of a static {@code DamageSource}.
 */
public class IC2Potion {
    public static StatusEffect radiation;

    /** Registry entry of the effect, which {@link StatusEffectInstance} needs in 1.21.1. */
    public static RegistryEntry<StatusEffect> radiationEntry;

    public static void init() {
        radiation = Registry.register(Registries.STATUS_EFFECT, Identifier.of("ic2", "radiation"),
                new RadiationEffect());
        radiationEntry = Registries.STATUS_EFFECT.getEntry(radiation);
        ic2.api.info.Info.POTION_RADIATION = radiation;
    }

    /**
     * Give {@code entity} the radiation effect, {@code IC2Potion.applyTo} in 1.12.2.
     *
     * @param duration in ticks
     * @param amplifier the 1.12.2 amplifier, which for radiation runs up to 100 rather than to 4
     */
    public static void applyTo(LivingEntity entity, int duration, int amplifier) {
        if (radiationEntry == null) return;
        // nothing hurts a creative or spectating player, radiation included
        if (entity instanceof net.minecraft.entity.player.PlayerEntity player && player.getAbilities().invulnerable) return;
        entity.addStatusEffect(new StatusEffectInstance(radiationEntry, duration, amplifier));
    }

    /** The effect itself: damage every few ticks, how often and how hard set by the amplifier. */
    private static final class RadiationEffect extends StatusEffect {
        // 5149489 is the liquid colour the 1.12.2 potion was registered with
        private RadiationEffect() {
            super(StatusEffectCategory.HARMFUL, 5149489);
        }

        @Override
        public boolean applyUpdateEffect(LivingEntity entity, int amplifier) {
            DamageSource source = IC2DamageSource.radiation(entity.getWorld());
            if (source == null) return true;

            // the 1.12.2 formula, integer division included: amplifier 100 hits for 1.5
            entity.damage(source, (float) (amplifier / 100) + 0.5f);
            return true;
        }

        @Override
        public boolean canApplyUpdateEffect(int duration, int amplifier) {
            int rate = 25 >> amplifier;
            return rate > 0 ? duration % rate == 0 : true;
        }
    }
}
