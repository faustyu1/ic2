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

        double chargeAmount = TileEntitySolarGenerator.getSkyLight(world, player.getBlockPos());
        if (chargeAmount <= 0.0) return;

        // the chestplate, or the pack on the back when the chestplate takes no charge (plain armour
        // over a battery pack in the back slot)
        ItemStack chest = player.getEquippedStack(EquipmentSlot.CHEST);
        if (ElectricItem.manager.charge(chest, chargeAmount, Integer.MAX_VALUE, true, false) <= 0.0) {
            ElectricItem.manager.charge(ic2.core.item.armor.jetpack.BackSlot.get(player), chargeAmount, Integer.MAX_VALUE, true, false);
        }
    }
}
