package ic2.api.item;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.DamageTypeTags;

/**
 * Port of {@code net.minecraftforge.common.ISpecialArmor} - the hook IC2 armour used in 1.12.2 to
 * absorb damage with energy instead of with armour points.
 *
 * <p>Fabric has no equivalent, so the contract is kept here as part of the IC2 API and
 * {@link #applyArmor} is driven from {@code ic2.core.mixin.LivingEntityArmorMixin}, which runs it
 * on whatever damage vanilla armour left over.
 */
public interface ISpecialArmor {
    /**
     * How much of the incoming damage this piece takes, see {@link ArmorProperties}.
     */
    ArmorProperties getProperties(LivingEntity entity, ItemStack armor, DamageSource source, double damage, EquipmentSlot slot);

    /**
     * Armour points to show in the HUD for this piece.
     */
    int getArmorDisplay(PlayerEntity player, ItemStack armor, EquipmentSlot slot);

    /**
     * Charge or durability to spend for the damage this piece absorbed.
     */
    void damageArmor(LivingEntity entity, ItemStack stack, DamageSource source, int damage, EquipmentSlot slot);

    /**
     * The absorption of a single armour piece: {@code priority} groups pieces (a higher priority
     * group gets the damage first and passes the rest on), {@code absorbRatio} is the share of the
     * damage reaching the group that the piece eats, and {@code absorbMax} caps how much damage the
     * piece can eat at all.
     */
    class ArmorProperties implements Comparable<ArmorProperties> {
        public int priority;
        public int absorbMax;
        public double absorbRatio;
        public EquipmentSlot slot;

        public ArmorProperties(int priority, double ratio, int max) {
            this.priority = priority;
            this.absorbRatio = ratio;
            this.absorbMax = max;
        }

        public ArmorProperties copy() {
            ArmorProperties copy = new ArmorProperties(priority, absorbRatio, absorbMax);
            copy.slot = slot;
            return copy;
        }

        @Override
        public int compareTo(ArmorProperties other) {
            if (other.priority != priority) {
                return other.priority - priority;
            }

            double left = absorbRatio == 0.0 ? 0.0 : absorbMax / absorbRatio;
            double right = other.absorbRatio == 0.0 ? 0.0 : other.absorbMax / other.absorbRatio;
            return (int) (right - left);
        }

        @Override
        public String toString() {
            return "ArmorProperties[priority=" + priority + ", ratio=" + absorbRatio + ", max=" + absorbMax + "]";
        }
    }

    /**
     * Run the IC2 armour pieces the entity wears over {@code damage} and return what is left.
     *
     * <p>This is Forge's {@code ArmorProperties.applyArmor} with the 1.12.2 inventory index
     * replaced by the equipment slot. Vanilla armour is not considered here - by the time the mixin
     * calls this, vanilla has already taken its share - so only items implementing
     * {@link ISpecialArmor} take part.
     */
    static float applyArmor(LivingEntity entity, DamageSource source, double damage) {
        if (source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return (float) damage;
        }

        Map<EquipmentSlot, ItemStack> worn = new EnumMap<>(EquipmentSlot.class);
        List<ArmorProperties> parts = new ArrayList<>(4);

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) continue;

            ItemStack stack = entity.getEquippedStack(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof ISpecialArmor armor)) continue;

            ArmorProperties prop = armor.getProperties(entity, stack, source, damage, slot).copy();
            if (prop.absorbRatio <= 0.0) continue;

            prop.slot = slot;
            worn.put(slot, stack);
            parts.add(prop);
        }

        if (parts.isEmpty()) {
            return (float) damage;
        }

        ArmorProperties[] props = parts.toArray(new ArmorProperties[0]);
        standardize(props, damage);

        int level = props[0].priority;
        double ratio = 0.0;

        for (ArmorProperties prop : props) {
            if (level != prop.priority) {
                damage -= damage * ratio;
                ratio = 0.0;
                level = prop.priority;
            }

            ratio += prop.absorbRatio;

            double absorb = damage * prop.absorbRatio;
            if (absorb > 0.0) {
                ItemStack stack = worn.get(prop.slot);
                int itemDamage = (int) (absorb / 25.0 < 1.0 ? 1.0 : absorb / 25.0);
                ((ISpecialArmor) stack.getItem()).damageArmor(entity, stack, source, itemDamage, prop.slot);
            }
        }

        damage -= damage * ratio;
        return (float) Math.max(0.0, damage);
    }

    /**
     * Forge's {@code StandardizeList}: normalise the ratios inside each priority group so they sum
     * to at most 1, and clamp any piece that would absorb more than its {@code absorbMax}.
     */
    static void standardize(ArmorProperties[] armor, double damage) {
        Arrays.sort(armor);

        int start = 0;
        double total = 0.0;
        int priority = armor[0].priority;
        int pStart = 0;
        boolean pChange = false;
        boolean pFinished = false;

        for (int x = 0; x < armor.length; x++) {
            total += armor[x].absorbRatio;

            if (x == armor.length - 1 || armor[x].priority != priority) {
                if (x != armor.length - 1) {
                    pChange = true;
                }

                if (total > 1.0) {
                    if (!pFinished) {
                        pStart = start;
                        pFinished = true;
                    }

                    double progress = 0.0;

                    for (int y = start; y <= x; y++) {
                        double newRatio = armor[y].absorbRatio / total;

                        if (newRatio * damage > armor[y].absorbMax) {
                            armor[y].absorbRatio = armor[y].absorbMax / damage;
                            total = 0.0;
                            for (int z = pStart; z <= x; z++) {
                                total += armor[z].absorbRatio;
                            }
                            start = y + 1;
                            x = y;
                            progress = 0.0;
                            break;
                        }

                        armor[y].absorbRatio = newRatio;
                        progress += newRatio;
                    }

                    total -= progress;
                } else {
                    for (int y = start; y <= x; y++) {
                        total -= armor[y].absorbRatio;
                        if (armor[y].absorbRatio * damage > armor[y].absorbMax) {
                            armor[y].absorbRatio = armor[y].absorbMax / damage;
                        }
                    }
                }

                if (pChange) {
                    priority = armor[x].priority;
                    total = 0.0;
                    start = x + 1;
                    pChange = false;
                    pFinished = false;
                }
            }
        }
    }

    /**
     * Armour points the HUD shows: the vanilla points of plain armour plus whatever the IC2 pieces
     * report through {@link #getArmorDisplay}.
     */
    static int getTotalArmorDisplay(PlayerEntity player) {
        int total = 0;

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) continue;

            ItemStack stack = player.getEquippedStack(slot);
            if (stack.isEmpty()) continue;

            if (stack.getItem() instanceof ISpecialArmor armor) {
                total += armor.getArmorDisplay(player, stack, slot);
            } else if (stack.getItem() instanceof ArmorItem armor) {
                total += armor.getProtection();
            }
        }

        return total;
    }
}
