package ic2.core.item.armor;

import ic2.api.item.ElectricItem;
import ic2.core.block.generator.tileentity.TileEntitySolarGenerator;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * Fabric port of {@code ic2.core.item.armor.ItemArmorSolarHelmet}: under open sky it charges
 * whatever is worn in the chest slot, at the same rate a solar generator produces.
 */
public class ItemArmorSolarHelmet extends ItemArmorUtility {

    public ItemArmorSolarHelmet() {
        super("solar_helmet", IC2ArmorMaterials.SOLAR, Type.HELMET);
    }

    @Override
    public void onArmorTick(World world, PlayerEntity player, ItemStack stack) {
        if (world.isClient) return;

        // the chestplate, or the battery pack on the back when no chestplate is worn
        ItemStack chest = ic2.core.item.armor.jetpack.BackSlot.getChestOrBack(player);
        if (chest.isEmpty()) return;

        double chargeAmount = TileEntitySolarGenerator.getSkyLight(world, player.getBlockPos());
        if (chargeAmount > 0.0) {
            ElectricItem.manager.charge(chest, chargeAmount, Integer.MAX_VALUE, true, false);
        }
    }
}
