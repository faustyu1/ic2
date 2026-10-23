package ic2.core.item.armor;

import java.util.LinkedList;
import java.util.List;

import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;
import ic2.api.item.IItemHudInfo;
import ic2.api.item.ISpecialArmor;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.util.Rarity;

/**
 * Fabric port of {@code ic2.core.item.armor.ItemArmorElectric}: armour that absorbs damage by
 * spending EU rather than by wearing out.
 *
 * <p>The absorption numbers are the ones from 1.12.2 - base ratio per slot times the suit's own
 * ratio, capped by what the remaining charge can pay for at {@link #getEnergyPerDamage} EU per
 * damage point.
 */
public abstract class ItemArmorElectric extends ItemArmorIC2
        implements ISpecialArmor, IElectricItem, IItemHudInfo, IArmorTickItem {

    protected final double maxCharge;
    protected final double transferLimit;
    protected final int tier;

    protected ItemArmorElectric(String itemName, RegistryEntry<ArmorMaterial> material, Type type,
                                double maxCharge, double transferLimit, int tier) {
        this(itemName, material, type, maxCharge, transferLimit, tier, Rarity.COMMON);
    }

    protected ItemArmorElectric(String itemName, RegistryEntry<ArmorMaterial> material, Type type,
                                double maxCharge, double transferLimit, int tier, Rarity rarity) {
        super(itemName, material, type, new Settings().maxCount(1).maxDamage(27).rarity(rarity));
        this.maxCharge = maxCharge;
        this.transferLimit = transferLimit;
        this.tier = tier;
    }

    // ---- ISpecialArmor ----

    @Override
    public ISpecialArmor.ArmorProperties getProperties(LivingEntity entity, ItemStack armor, DamageSource source,
                                                       double damage, EquipmentSlot slot) {
        if (source.isIn(DamageTypeTags.BYPASSES_ARMOR)) {
            return new ISpecialArmor.ArmorProperties(0, 0.0, 0);
        }

        double absorptionRatio = getBaseAbsorptionRatio() * getDamageAbsorptionRatio();
        return new ISpecialArmor.ArmorProperties(0, absorptionRatio, damageLimit(armor));
    }

    @Override
    public int getArmorDisplay(PlayerEntity player, ItemStack armor, EquipmentSlot slot) {
        return ElectricItem.manager.getCharge(armor) >= getEnergyPerDamage()
                ? (int) Math.round(20.0 * getBaseAbsorptionRatio() * getDamageAbsorptionRatio())
                : 0;
    }

    @Override
    public void damageArmor(LivingEntity entity, ItemStack stack, DamageSource source, int damage, EquipmentSlot slot) {
        ElectricItem.manager.discharge(stack, (double) damage * getEnergyPerDamage(), Integer.MAX_VALUE, true, false, false);
    }

    /** How much damage this piece can still pay for with the charge it holds. */
    protected int damageLimit(ItemStack armor) {
        int energyPerDamage = getEnergyPerDamage();
        if (energyPerDamage <= 0) return Integer.MAX_VALUE;
        return (int) Math.min(Integer.MAX_VALUE, 25.0 * ElectricItem.manager.getCharge(armor) / energyPerDamage);
    }

    /** The share of the damage the slot takes, as in 1.12.2. */
    protected final double getBaseAbsorptionRatio() {
        return switch (getType()) {
            case HELMET -> 0.15;
            case CHESTPLATE, BODY -> 0.4;
            case LEGGINGS -> 0.3;
            case BOOTS -> 0.15;
        };
    }

    public abstract double getDamageAbsorptionRatio();

    public abstract int getEnergyPerDamage();

    // ---- IElectricItem ----

    @Override
    public boolean canProvideEnergy(ItemStack stack) {
        return false;
    }

    @Override
    public double getMaxCharge(ItemStack stack) {
        return maxCharge;
    }

    @Override
    public int getTier(ItemStack stack) {
        return tier;
    }

    @Override
    public double getTransferLimit(ItemStack stack) {
        return transferLimit;
    }

    // ---- misc ----

    @Override
    public List<String> getHudInfo(ItemStack stack, boolean advanced) {
        List<String> info = new LinkedList<>();
        info.add(ElectricItem.manager.getToolTip(stack));
        info.add("Tier " + tier);
        return info;
    }

    @Override
    public boolean canRepair(ItemStack stack, ItemStack ingredient) {
        return false;
    }

    @Override
    public int getEnchantability() {
        return 0;
    }

    /** Most electric armour does nothing per tick; the pieces that do override this. */
    @Override
    public void onArmorTick(net.minecraft.world.World world, PlayerEntity player, ItemStack stack) {
    }

    /** Fall damage the piece can swallow entirely, see {@code LivingEntityArmorMixin}. */
    public boolean absorbFallDamage(LivingEntity entity, ItemStack stack, float fallDistance) {
        return false;
    }
}
