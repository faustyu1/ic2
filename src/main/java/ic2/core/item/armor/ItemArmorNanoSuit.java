package ic2.core.item.armor;

import ic2.api.item.ElectricItem;
import ic2.api.item.HudMode;
import ic2.api.item.IItemHudProvider;
import ic2.api.item.ISpecialArmor;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Rarity;

/**
 * Fabric port of {@code ic2.core.item.armor.ItemArmorNanoSuit}: 1M EU, absorbs 90% of what reaches
 * its slot at 5000 EU per damage point, and the boots take the fall.
 *
 * <p>The night vision toggle is not here: it hangs off IC2's own key bindings
 * ({@code IC2.keyboard.isAltKeyDown}), which the port has no equivalent for yet.
 */
public class ItemArmorNanoSuit extends ItemArmorElectric implements IItemHudProvider {

    public ItemArmorNanoSuit(String itemName, Type type) {
        super(itemName, IC2ArmorMaterials.NANO, type, 1000000.0, 1600.0, 3, Rarity.UNCOMMON);
    }

    @Override
    public ISpecialArmor.ArmorProperties getProperties(LivingEntity entity, ItemStack armor, DamageSource source,
                                                       double damage, EquipmentSlot slot) {
        if (source.isOf(DamageTypes.FALL) && getType() == Type.BOOTS) {
            return new ISpecialArmor.ArmorProperties(10, damage < 8.0 ? 1.0 : 0.875, damageLimit(armor));
        }

        return super.getProperties(entity, armor, source, damage, slot);
    }

    @Override
    public boolean absorbFallDamage(LivingEntity entity, ItemStack stack, float fallDistance) {
        if (getType() != Type.BOOTS) return false;

        int fallDamage = (int) fallDistance - 3;
        if (fallDamage >= 8) return false;

        double energyCost = (double) getEnergyPerDamage() * fallDamage;
        if (energyCost > ElectricItem.manager.getCharge(stack)) return false;

        ElectricItem.manager.discharge(stack, energyCost, Integer.MAX_VALUE, true, false, false);
        return true;
    }

    @Override
    public double getDamageAbsorptionRatio() {
        return 0.9;
    }

    @Override
    public int getEnergyPerDamage() {
        return 5000;
    }

    @Override
    public void onArmorTick(net.minecraft.world.World world, net.minecraft.entity.player.PlayerEntity player, ItemStack stack) {
        if (getType() == Type.HELMET) {
            HelmetToggles.tick(world, player, stack);
        }
    }

    @Override
    public boolean doesProvideHUD(ItemStack stack) {
        return getType() == Type.HELMET && ElectricItem.manager.getCharge(stack) > 0.0;
    }

    @Override
    public HudMode getHudMode(ItemStack stack) {
        NbtComponent data = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        return HudMode.getFromID(data.copyNbt().getShort("HudMode"));
    }

}
