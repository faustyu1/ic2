package ic2.core.item;

import ic2.core.init.IC2ClassicBlocks;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Fabric port of {@code ItemHandlers.cfPowderApply}: construction foam powder thrown into water
 * sets, turning the water into foam.
 *
 * <p>1.12.2 ray-traced from the player and replaced a water source with the construction foam
 * fluid block; here the click already carries the block, and the foam is the registered
 * {@code ic2:foam} block.
 */
public class ItemCfPowder extends ItemCrafting {

    public ItemCfPowder(String name) {
        super(name);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        PlayerEntity player = context.getPlayer();

        if (!world.getBlockState(pos).isOf(Blocks.WATER) || IC2ClassicBlocks.FOAM == null) {
            return ActionResult.PASS;
        }
        if (player != null && !world.canPlayerModifyAt(player, pos)) {
            return ActionResult.FAIL;
        }

        if (!world.isClient) {
            world.setBlockState(pos, IC2ClassicBlocks.FOAM.getDefaultState());
            ItemStack stack = context.getStack();
            if (player == null || !player.isCreative()) {
                stack.decrement(1);
            }
        }

        return ActionResult.success(world.isClient);
    }
}
