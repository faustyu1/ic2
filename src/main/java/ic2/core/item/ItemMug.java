package ic2.core.item;

import ic2.core.init.IC2ItemVariants;
import ic2.core.item.type.MugType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

/**
 * Fabric port of ItemMug - the stone mug and the coffee in it.
 *
 * <p>Drinking coffee gives Speed and Haste for 15 seconds, and every further mug while they last
 * raises both by a level (cold coffee up to II, dark coffee up to VI, coffee with milk and sugar
 * up to VII) and adds 30 or 60 seconds. Too much of it makes the drinker sick: once the level
 * reaches III (V for the sweet coffee, which is milder) nausea follows, and beyond that it hurts.
 * The mug is left empty.
 */
public class ItemMug extends ItemIC2 {
    private final MugType type;

    public ItemMug(String name, MugType type) {
        super(name, new Item.Settings().maxCount(1));
        this.type = type;
    }

    public ItemMug(String name) {
        this(name, MugType.empty);
    }

    public ItemMug() {
        this("mug", MugType.empty);
    }

    public MugType getType() {
        return type;
    }

    /** An empty mug, to hand back once one is drunk. */
    public static ItemStack emptyMug() {
        Item mug = IC2ItemVariants.get("mug", "empty");
        return mug == null ? ItemStack.EMPTY : new ItemStack(mug);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        if (type != MugType.empty) {
            return ItemUsage.consumeHeldItem(world, player, hand);
        }
        return super.use(world, player, hand);
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return type != MugType.empty ? 32 : 0;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return type != MugType.empty ? UseAction.DRINK : UseAction.NONE;
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (!(user instanceof PlayerEntity player) || type == MugType.empty) {
            return stack;
        }

        int maxAmplifier;
        int extraDuration;
        switch (type) {
            case cold_coffee -> {
                maxAmplifier = 1;
                extraDuration = 600;
            }
            case dark_coffee -> {
                maxAmplifier = 5;
                extraDuration = 1200;
            }
            default -> {
                maxAmplifier = 6;
                extraDuration = 1200;
            }
        }

        int highest = Math.max(0, amplifyEffect(player, StatusEffects.SPEED, maxAmplifier, extraDuration));
        highest = Math.max(highest, amplifyEffect(player, StatusEffects.HASTE, maxAmplifier, extraDuration));
        if (type == MugType.coffee) {
            highest -= 2;
        }

        if (highest >= 3) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, (highest - 2) * 200, 0));
            if (highest >= 4) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.INSTANT_DAMAGE, 1, highest - 3));
            }
        }

        return emptyMug();
    }

    /**
     * Starts the effect at level I for 15 seconds, or raises a running one by a level (up to the
     * cap) and lengthens it.
     *
     * @return the new amplifier; 1 when the effect was new, as 1.12.2 counted it
     */
    private static int amplifyEffect(PlayerEntity player, RegistryEntry<StatusEffect> effect, int maxAmplifier, int extraDuration) {
        StatusEffectInstance current = player.getStatusEffect(effect);
        if (current == null) {
            player.addStatusEffect(new StatusEffectInstance(effect, 300, 0));
            return 1;
        }

        int amplifier = current.getAmplifier();
        if (amplifier < maxAmplifier) {
            amplifier++;
        }
        int duration = current.getDuration() + extraDuration;
        player.removeStatusEffect(effect);
        player.addStatusEffect(new StatusEffectInstance(effect, duration, amplifier));
        return amplifier;
    }
}
