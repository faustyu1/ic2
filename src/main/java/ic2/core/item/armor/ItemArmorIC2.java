package ic2.core.item.armor;

import ic2.api.item.IMetalArmor;
import ic2.core.item.ElectricItemBar;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;

/**
 * Fabric port of {@code ic2.core.item.armor.ItemArmorIC2}: plain IC2 armour that protects with
 * armour points, like the bronze set and the alloy chestplate.
 *
 * <p>The 1.12.2 class also carried the model provider, the creative tab and the Forge capability
 * map; registration, the tab and the texture now come from {@link IC2ArmorMaterials} and from
 * {@code ic2.core.init.IC2Items}.
 */
public class ItemArmorIC2 extends ArmorItem implements IMetalArmor {
    private final String itemName;

    public ItemArmorIC2(String itemName, RegistryEntry<ArmorMaterial> material, Type type, Settings settings) {
        super(material, type, settings);
        this.itemName = itemName;
    }

    public ItemArmorIC2(String itemName, RegistryEntry<ArmorMaterial> material, Type type, int durabilityMultiplier) {
        this(itemName, material, type, new Settings().maxDamage(type.getMaxDamage(durabilityMultiplier)));
    }

    public String getItemName() {
        return itemName;
    }

    @Override
    public boolean isMetalArmor(ItemStack stack, PlayerEntity player) {
        return true;
    }

    // Electric armour shows its charge on the bar; plain armour keeps the vanilla damage bar.

    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return ElectricItemBar.visible(stack) || super.isItemBarVisible(stack);
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        return ElectricItemBar.visible(stack) ? ElectricItemBar.step(stack) : super.getItemBarStep(stack);
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        return ElectricItemBar.visible(stack) ? ElectricItemBar.color(stack) : super.getItemBarColor(stack);
    }
}
