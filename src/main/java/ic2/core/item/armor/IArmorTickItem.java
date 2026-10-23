package ic2.core.item.armor;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * Replacement for Forge's {@code Item.onArmorTick}, which 1.21.1 has no equivalent for.
 * {@code ic2.core.mixin.PlayerEntityArmorTickMixin} calls this once per tick for every worn piece.
 */
public interface IArmorTickItem {
    void onArmorTick(World world, PlayerEntity player, ItemStack stack);
}
