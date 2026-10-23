package ic2.core.item.armor;

import ic2.api.item.ISpecialArmor;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.World;

/**
 * Fabric port of {@code ic2.core.item.armor.ItemArmorUtility}: a worn piece that gives no
 * protection at all and exists for what it does each tick - the solar helmet, the static boots and
 * the night vision goggles.
 */
public class ItemArmorUtility extends ItemArmorIC2 implements ISpecialArmor, IArmorTickItem {

    public ItemArmorUtility(String itemName, RegistryEntry<ArmorMaterial> material, Type type, Settings settings) {
        super(itemName, material, type, settings);
    }

    public ItemArmorUtility(String itemName, RegistryEntry<ArmorMaterial> material, Type type) {
        this(itemName, material, type, new Settings().maxCount(1));
    }

    @Override
    public int getEnchantability() {
        return 0;
    }

    @Override
    public boolean canRepair(ItemStack stack, ItemStack ingredient) {
        return false;
    }

    @Override
    public ISpecialArmor.ArmorProperties getProperties(LivingEntity entity, ItemStack armor, DamageSource source,
                                                       double damage, EquipmentSlot slot) {
        return new ISpecialArmor.ArmorProperties(0, 0.0, 0);
    }

    @Override
    public int getArmorDisplay(PlayerEntity player, ItemStack armor, EquipmentSlot slot) {
        return 0;
    }

    @Override
    public void damageArmor(LivingEntity entity, ItemStack stack, DamageSource source, int damage, EquipmentSlot slot) {
    }

    @Override
    public void onArmorTick(World world, PlayerEntity player, ItemStack stack) {
    }
}
