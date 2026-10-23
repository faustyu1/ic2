package ic2.core.item.armor;

import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;

/**
 * The chest-slot battery packs - batpack, advanced batpack, energy pack, lappack.
 *
 * <p>In 1.12.2 each was its own subclass of {@code ItemArmorElectric} differing only in capacity,
 * transfer limit and tier; they all absorb nothing and all feed the tools in the hotbar.
 */
public class ItemArmorPack extends ItemArmorElectric {

    public ItemArmorPack(String itemName, RegistryEntry<ArmorMaterial> material, double maxCharge,
                         double transferLimit, int tier) {
        super(itemName, material, Type.CHESTPLATE, maxCharge, transferLimit, tier);
    }

    @Override
    public boolean canProvideEnergy(ItemStack stack) {
        return true;
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
