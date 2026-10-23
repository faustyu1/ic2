package ic2.api.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public interface IMetalArmor {
    boolean isMetalArmor(ItemStack stack, PlayerEntity player);
}
