package ic2.core.item;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

/**
 * Fabric port of ItemBooze - a mug of beer or rum from the brewing barrel.
 *
 * <p>What is in the mug is one packed number, the item damage in 1.12.2 and the {@code value} of
 * the item's custom data here: two bits of type (beer or rum), then for beer three bits each of
 * how solid (wheat), how hoppy and how long it matured, and for rum seven bits of progress.
 * The name is built from those ("Strong Dark Ale"), and so is the effect of drinking it: beer
 * feeds and makes the drinker slow at digging, and every further mug adds strength, slowness,
 * resistance, nausea and harm in turn; rum gives fire resistance and resistance. Black stuff and
 * rum that is not ready yet make the drinker sick.
 */
public class ItemBooze extends ItemIC2 {
    public static final String[] SOLID_RATIO = {"Watery ", "Clear ", "Lite ", "", "Strong ", "Thick ", "Stodge ", "X"};
    public static final String[] HOPS_RATIO = {"Soup ", "Alcfree ", "White ", "", "Dark ", "Full ", "Black ", "X"};
    public static final String[] TIME_RATIO_NAMES = {"Brew", "Youngster", "Beer", "Ale", "Dragonblood", "Black Stuff"};
    public static final int[] BASE_DURATION = {300, 600, 900, 1200, 1600, 2000, 2400};
    public static final float[] BASE_INTENSITY = {0.4F, 0.75F, 1.0F, 1.5F, 2.0F};
    public static final float RUM_STACKABILITY = 2.0F;
    public static final int RUM_DURATION = 600;

    public ItemBooze(String name) {
        super(name, new Item.Settings().maxCount(1));
    }

    public ItemBooze() {
        this("booze");
    }

    /** The packed contents of the mug. */
    public static int getValue(ItemStack stack) {
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        return data == null ? 0 : data.copyNbt().getInt("value");
    }

    public static void setValue(ItemStack stack, int value) {
        NbtCompound nbt = new NbtCompound();
        nbt.putInt("value", value);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    @Override
    public Text getName(ItemStack stack) {
        int value = getValue(stack);
        int type = getTypeOfValue(value);
        if (type == 1) {
            int timeRatio = Math.min(getTimeRatioOfBeerValue(value), TIME_RATIO_NAMES.length - 1);
            return Text.literal(timeRatio == TIME_RATIO_NAMES.length - 1
                    ? TIME_RATIO_NAMES[timeRatio]
                    : SOLID_RATIO[getSolidRatioOfBeerValue(value)] + HOPS_RATIO[getHopsRatioOfBeerValue(value)] + TIME_RATIO_NAMES[timeRatio]);
        }
        return Text.literal(type == 2 ? "Rum" : "Zero");
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        return ItemUsage.consumeHeldItem(world, player, hand);
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return 32;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.DRINK;
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity living) {
        int value = getValue(stack);
        int type = getTypeOfValue(value);
        if (type == 0) {
            return ItemMug.emptyMug();
        }

        if (type == 1) {
            if (getTimeRatioOfBeerValue(value) == 5) {
                return drinkBlackStuff(living);
            }

            int solidRatio = getSolidRatioOfBeerValue(value);
            int alc = getHopsRatioOfBeerValue(value);
            // the top values of the three-bit fields ran past the 1.12.2 tables and crashed
            int duration = BASE_DURATION[Math.min(solidRatio, BASE_DURATION.length - 1)];
            float intensity = BASE_INTENSITY[Math.min(getTimeRatioOfBeerValue(value), BASE_INTENSITY.length - 1)];
            if (living instanceof PlayerEntity player) {
                // 1.12.2's addStats took a saturation modifier, 1.21 takes the saturation itself
                int food = 6 - alc;
                player.getHungerManager().add(food, food * solidRatio * 0.15F * 2.0F);
            }

            int max = (int) (intensity * alc * 0.5F);
            StatusEffectInstance slow = living.getStatusEffect(StatusEffects.MINING_FATIGUE);
            int level = slow == null ? -1 : slow.getAmplifier();
            amplifyEffect(living, StatusEffects.MINING_FATIGUE, max, intensity, duration);
            if (level > -1) {
                amplifyEffect(living, StatusEffects.STRENGTH, max, intensity, duration);
                if (level > 0) {
                    amplifyEffect(living, StatusEffects.SLOWNESS, max / 2, intensity, duration);
                    if (level > 1) {
                        amplifyEffect(living, StatusEffects.RESISTANCE, max - 1, intensity, duration);
                        if (level > 2) {
                            amplifyEffect(living, StatusEffects.NAUSEA, 0, intensity, duration);
                            if (level > 3) {
                                living.addStatusEffect(new StatusEffectInstance(StatusEffects.INSTANT_DAMAGE, 1, living.getWorld().random.nextInt(3)));
                            }
                        }
                    }
                }
            }
        }

        if (type == 2) {
            if (getProgressOfRumValue(value) < 100) {
                drinkBlackStuff(living);
            } else {
                amplifyEffect(living, StatusEffects.FIRE_RESISTANCE, 0, RUM_STACKABILITY, RUM_DURATION);
                StatusEffectInstance def = living.getStatusEffect(StatusEffects.RESISTANCE);
                int level = def == null ? -1 : def.getAmplifier();
                amplifyEffect(living, StatusEffects.RESISTANCE, 2, RUM_STACKABILITY, RUM_DURATION);
                if (level >= 0) {
                    amplifyEffect(living, StatusEffects.BLINDNESS, 0, RUM_STACKABILITY, RUM_DURATION);
                }
                if (level >= 1) {
                    amplifyEffect(living, StatusEffects.NAUSEA, 0, RUM_STACKABILITY, RUM_DURATION);
                }
            }
        }

        return ItemMug.emptyMug();
    }

    /**
     * Starts the effect, or makes a running one one level stronger (up to {@code max}) and longer -
     * the stronger the drink, the more it adds, but never past the duration times 1 + 2 intensity.
     */
    public static void amplifyEffect(LivingEntity living, RegistryEntry<StatusEffect> effect, int max, float intensity, int duration) {
        StatusEffectInstance current = living.getStatusEffect(effect);
        if (current == null) {
            living.addStatusEffect(new StatusEffectInstance(effect, duration, 0));
            return;
        }

        int currentDuration = current.getDuration();
        int maxNewDuration = (int) (duration * (1.0F + intensity * 2.0F) - currentDuration) / 2;
        if (maxNewDuration < 0) {
            maxNewDuration = 0;
        }
        if (maxNewDuration < duration) {
            duration = maxNewDuration;
        }

        currentDuration += duration;
        int amplifier = current.getAmplifier();
        if (amplifier < max) {
            amplifier++;
        }
        living.removeStatusEffect(effect);
        living.addStatusEffect(new StatusEffectInstance(effect, currentDuration, amplifier));
    }

    /** One of five ills at random, or luck and nothing at all. */
    public static ItemStack drinkBlackStuff(LivingEntity living) {
        switch (living.getWorld().random.nextInt(6)) {
            case 1 -> living.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 1200, 0));
            case 2 -> living.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 2400, 0));
            case 3 -> living.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 2400, 0));
            case 4 -> living.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 200, 2));
            case 5 -> living.addStatusEffect(new StatusEffectInstance(StatusEffects.INSTANT_DAMAGE, 1, living.getWorld().random.nextInt(4)));
            default -> { }
        }
        return ItemMug.emptyMug();
    }

    public static int getTypeOfValue(int value) {
        return unpackValue(value, 0, 2);
    }

    public static int getAmountOfValue(int value) {
        return getTypeOfValue(value) == 0 ? 0 : unpackValue(value, 2, 5) + 1;
    }

    public static int getSolidRatioOfBeerValue(int value) {
        return unpackValue(value, 7, 3);
    }

    public static int getHopsRatioOfBeerValue(int value) {
        return unpackValue(value, 10, 3);
    }

    public static int getTimeRatioOfBeerValue(int value) {
        return unpackValue(value, 13, 3);
    }

    public static int getProgressOfRumValue(int value) {
        return unpackValue(value, 7, 7);
    }

    private static int unpackValue(int value, int bitshift, int take) {
        value >>>= bitshift;
        int mask = (1 << take) - 1;
        return value & mask;
    }
}
