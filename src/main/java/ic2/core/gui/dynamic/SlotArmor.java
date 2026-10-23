package ic2.core.gui.dynamic;

import com.mojang.datafixers.util.Pair;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;

/**
 * One of the player's armour slots shown in a machine window, as ic2.core.slot.SlotArmor put
 * them into the energy storage window so suits can be charged without leaving it.
 */
public class SlotArmor extends Slot {
    private final PlayerEntity player;
    private final EquipmentSlot armorSlot;

    public SlotArmor(PlayerInventory inventory, EquipmentSlot armorSlot, int x, int y) {
        super(inventory, PlayerInventory.MAIN_SIZE + armorSlot.getEntitySlotId(), x, y);
        this.player = inventory.player;
        this.armorSlot = armorSlot;
    }

    /** The guidef name of an armour slot: head, chest, legs or feet. */
    public static EquipmentSlot byName(String name) {
        return switch (name) {
            case "head" -> EquipmentSlot.HEAD;
            case "chest" -> EquipmentSlot.CHEST;
            case "legs" -> EquipmentSlot.LEGS;
            case "feet" -> EquipmentSlot.FEET;
            default -> null;
        };
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        return player.getPreferredEquipmentSlot(stack) == armorSlot;
    }

    @Override
    public int getMaxItemCount() {
        return 1;
    }

    @Override
    public boolean canTakeItems(PlayerEntity player) {
        ItemStack stack = getStack();
        return (stack.isEmpty() || player.isCreative()
                || !EnchantmentHelper.hasAnyEnchantmentsWith(stack, net.minecraft.component.EnchantmentEffectComponentTypes.PREVENT_ARMOR_CHANGE))
                && super.canTakeItems(player);
    }

    @Override
    public Pair<Identifier, Identifier> getBackgroundSprite() {
        Identifier sprite = switch (armorSlot) {
            case HEAD -> PlayerScreenHandler.EMPTY_HELMET_SLOT_TEXTURE;
            case CHEST -> PlayerScreenHandler.EMPTY_CHESTPLATE_SLOT_TEXTURE;
            case LEGS -> PlayerScreenHandler.EMPTY_LEGGINGS_SLOT_TEXTURE;
            default -> PlayerScreenHandler.EMPTY_BOOTS_SLOT_TEXTURE;
        };
        return Pair.of(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, sprite);
    }
}
