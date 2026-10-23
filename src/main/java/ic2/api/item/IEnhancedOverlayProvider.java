package ic2.api.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public interface IEnhancedOverlayProvider {
    boolean providesEnhancedOverlay(World world, BlockPos pos, Direction facing, PlayerEntity player, ItemStack stack);
}
