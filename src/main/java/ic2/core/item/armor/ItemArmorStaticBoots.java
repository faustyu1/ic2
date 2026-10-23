package ic2.core.item.armor;

import ic2.api.item.ElectricItem;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

/**
 * Fabric port of {@code ic2.core.item.armor.ItemArmorStaticBoots}: walking charges what is worn in
 * the chest slot, up to 3 EU per 5 blocks covered. Riding or swimming resets the counter instead
 * of paying out, as in 1.12.2.
 */
public class ItemArmorStaticBoots extends ItemArmorUtility {

    public ItemArmorStaticBoots() {
        super("static_boots", IC2ArmorMaterials.RUBBER, Type.BOOTS);
    }

    @Override
    public void onArmorTick(World world, PlayerEntity player, ItemStack stack) {
        if (world.isClient) return;

        // the chestplate, or the battery pack on the back when no chestplate is worn
        ItemStack chest = ic2.core.item.armor.jetpack.BackSlot.getChestOrBack(player);
        if (chest.isEmpty()) return;

        NbtCompound data = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        boolean isNotWalking = player.hasVehicle() || player.isTouchingWater();

        int px = (int) player.getX();
        int pz = (int) player.getZ();

        if (!data.contains("x") || isNotWalking) data.putInt("x", px);
        if (!data.contains("z") || isNotWalking) data.putInt("z", pz);

        int dx = data.getInt("x") - px;
        int dz = data.getInt("z") - pz;
        double distance = Math.sqrt((double) dx * dx + (double) dz * dz);

        if (distance >= 5.0) {
            data.putInt("x", px);
            data.putInt("z", pz);
            ElectricItem.manager.charge(chest, Math.min(3.0, distance / 5.0), Integer.MAX_VALUE, true, false);
        }

        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(data));
    }
}
