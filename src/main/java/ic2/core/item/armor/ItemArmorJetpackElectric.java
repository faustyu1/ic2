package ic2.core.item.armor;

import ic2.api.item.ElectricItem;
import ic2.core.item.armor.jetpack.IJetpack;
import ic2.core.item.armor.jetpack.JetpackLogic;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * Fabric port of {@code ic2.core.item.armor.ItemArmorJetpackElectric}: the electric jetpack, worn
 * in the chest slot, 30k EU, absorbing nothing and flying on {@link JetpackLogic}.
 *
 * <p>The fuel jetpack ({@code ic2:jetpack}) is {@link ItemArmorJetpack}, flying on biogas.
 */
public class ItemArmorJetpackElectric extends ItemArmorElectric implements IJetpack {

    public ItemArmorJetpackElectric(String itemName, double maxCharge, double transferLimit, int tier) {
        super(itemName, IC2ArmorMaterials.JETPACK, Type.CHESTPLATE, maxCharge, transferLimit, tier);
    }

    @Override
    public void onArmorTick(World world, PlayerEntity player, ItemStack stack) {
        JetpackLogic.onArmorTick(world, player, stack, this);
    }

    // ---- IJetpack ----

    @Override
    public boolean drainEnergy(ItemStack pack, int amount) {
        return ElectricItem.manager.discharge(pack, amount + EU_ENERGY_INCREASE, Integer.MAX_VALUE, true, false, false) > 0.0;
    }

    @Override
    public float getPower(ItemStack stack) {
        return 0.7f;
    }

    @Override
    public float getDropPercentage(ItemStack stack) {
        return 0.05f;
    }

    @Override
    public double getChargeLevel(ItemStack stack) {
        return ElectricItem.manager.getCharge(stack) / getMaxCharge(stack);
    }

    @Override
    public boolean isJetpackActive(ItemStack stack) {
        return true;
    }

    @Override
    public float getHoverMultiplier(ItemStack stack, boolean upwards) {
        return 0.1f;
    }

    @Override
    public float getWorldHeightDivisor(ItemStack stack) {
        return 1.28f;
    }

    // ---- no protection ----

    @Override
    public boolean canProvideEnergy(ItemStack stack) {
        return false;
    }

    @Override
    public double getDamageAbsorptionRatio() {
        return 0.0;
    }

    @Override
    public int getEnergyPerDamage() {
        return 0;
    }
}
