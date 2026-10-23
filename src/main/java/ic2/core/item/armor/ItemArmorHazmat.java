package ic2.core.item.armor;

import ic2.api.item.IHazmatLike;
import ic2.api.item.ISpecialArmor;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.World;

/**
 * Fabric port of {@code ic2.core.item.armor.ItemArmorHazmat}: the hazmat suit and the rubber boots.
 *
 * <p>Worn as a complete set it takes all of fire, lava, suffocation, electricity and radiation;
 * worn in pieces it still soaks 5% of everything, as in 1.12.2. The boots also cut fall damage.
 *
 * <p>Electricity is not in the list below because {@code ic2.core.IC2DamageSource} still aliases
 * it onto a vanilla damage type - taking that would mean taking generic damage too. Radiation is
 * its own {@code ic2:radiation} type, so it is listed; the potion side of radiation goes through
 * {@link IHazmatLike#hasCompleteHazmat}, which this class is what makes true.
 */
public class ItemArmorHazmat extends ItemArmorIC2 implements ISpecialArmor, IHazmatLike, IArmorTickItem {

    public ItemArmorHazmat(String itemName, RegistryEntry<ArmorMaterial> material, Type type) {
        super(itemName, material, type, new Settings().maxCount(1).maxDamage(64));
    }

    @Override
    public ISpecialArmor.ArmorProperties getProperties(LivingEntity entity, ItemStack armor, DamageSource source,
                                                       double damage, EquipmentSlot slot) {
        if (getType() == Type.HELMET && hazmatAbsorbs(source) && IHazmatLike.hasCompleteHazmat(entity)) {
            if (source.isOf(DamageTypes.IN_FIRE) || source.isOf(DamageTypes.ON_FIRE) || source.isOf(DamageTypes.HOT_FLOOR)) {
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 60, 1));
            }

            return new ISpecialArmor.ArmorProperties(10, 1.0, Integer.MAX_VALUE);
        }

        int remaining = armor.getMaxDamage() - armor.getDamage();

        if (getType() == Type.BOOTS && source.isOf(DamageTypes.FALL)) {
            return new ISpecialArmor.ArmorProperties(10, damage < 8.0 ? 1.0 : 0.875, (remaining + 2) * 2 * 25);
        }

        return new ISpecialArmor.ArmorProperties(0, 0.05, (remaining + 2) / 2 * 25);
    }

    @Override
    public void damageArmor(LivingEntity entity, ItemStack stack, DamageSource source, int damage, EquipmentSlot slot) {
        if (hazmatAbsorbs(source) && IHazmatLike.hasCompleteHazmat(entity)) {
            return;
        }

        int damageTotal = damage * 2;
        if (getType() == Type.BOOTS && source.isOf(DamageTypes.FALL)) {
            damageTotal = (damage + 1) / 2;
        }

        stack.damage(damageTotal, entity, slot);
    }

    @Override
    public int getArmorDisplay(PlayerEntity player, ItemStack armor, EquipmentSlot slot) {
        return 1;
    }

    @Override
    public boolean addsProtection(LivingEntity entity, EquipmentSlot slot, ItemStack stack) {
        return true;
    }

    @Override
    public boolean isMetalArmor(ItemStack stack, PlayerEntity player) {
        return false;
    }

    @Override
    public boolean canRepair(ItemStack stack, ItemStack ingredient) {
        return true;
    }

    /**
     * The helmet of a complete suit puts the fire out, as the 1.12.2 {@code onArmorTick} did.
     *
     * <p>The air refill from air cells in the inventory is not here yet: it needs
     * {@code LiquidUtil.drainContainer}, which the port does not have.
     */
    @Override
    public void onArmorTick(World world, PlayerEntity player, ItemStack stack) {
        if (world.isClient || getType() != Type.HELMET) return;

        if (player.isOnFire() && IHazmatLike.hasCompleteHazmat(player)) {
            if (player.isInLava()) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 20, 0, true, true));
            }
            player.extinguish();
        }
    }

    /**
     * Fall damage the boots swallow outright, the 1.12.2 {@code LivingFallEvent} handler.
     *
     * @return true when the boots took the fall and the entity should take none
     */
    public boolean absorbFallDamage(LivingEntity entity, ItemStack stack, float fallDistance) {
        if (getType() != Type.BOOTS) return false;

        int fallDamage = (int) fallDistance - 3;
        if (fallDamage >= 8) return false;

        int armorDamage = (fallDamage + 1) / 2;
        if (armorDamage < 0 || armorDamage > stack.getMaxDamage() - stack.getDamage()) return false;

        stack.damage(armorDamage, entity, EquipmentSlot.FEET);
        return true;
    }

    /** The damage the suit is built to keep out, {@code hazmatAbsorbs} in 1.12.2. */
    public static boolean hazmatAbsorbs(DamageSource source) {
        for (RegistryKey<net.minecraft.entity.damage.DamageType> key : ABSORBED) {
            if (source.isOf(key)) return true;
        }
        return false;
    }

    private static final RegistryKey<net.minecraft.entity.damage.DamageType>[] ABSORBED = asArray(
            DamageTypes.IN_FIRE,
            DamageTypes.ON_FIRE,
            DamageTypes.LAVA,
            DamageTypes.HOT_FLOOR,
            DamageTypes.IN_WALL,
            ic2.core.IC2DamageSource.RADIATION,
            ic2.core.IC2DamageSource.ELECTRICITY);

    @SafeVarargs
    private static RegistryKey<net.minecraft.entity.damage.DamageType>[] asArray(
            RegistryKey<net.minecraft.entity.damage.DamageType>... keys) {
        return keys;
    }
}
