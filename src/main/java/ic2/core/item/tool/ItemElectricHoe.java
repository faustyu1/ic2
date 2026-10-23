package ic2.core.item.tool;

import java.util.List;

import ic2.api.item.ElectricItem;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * Fabric port of {@code ic2.core.item.tool.ItemElectricToolHoe}: tills ground for 50 EU and mines
 * what a hoe mines at 16.
 */
public class ItemElectricHoe extends ItemElectricTool {

    public ItemElectricHoe() {
        super("electric_hoe", 50, 10000, 100, 1, 16.0f, BlockTags.INCORRECT_FOR_IRON_TOOL,
                List.of(BlockTags.HOE_MINEABLE));
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        ItemStack stack = context.getStack();
        if (!ElectricItem.manager.canUse(stack, operationEnergyCost)) {
            return ActionResult.PASS;
        }

        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();

        boolean tillable = world.getBlockState(pos).isOf(Blocks.GRASS_BLOCK)
                || world.getBlockState(pos).isOf(Blocks.DIRT)
                || world.getBlockState(pos).isOf(Blocks.DIRT_PATH)
                || world.getBlockState(pos).isOf(Blocks.COARSE_DIRT)
                || world.getBlockState(pos).isOf(Blocks.ROOTED_DIRT);

        if (context.getSide() == Direction.DOWN || !world.isAir(pos.up()) || !tillable) {
            return ActionResult.PASS;
        }

        world.playSound(null, pos, SoundEvents.ITEM_HOE_TILL, SoundCategory.BLOCKS, 1.0f, 1.0f);

        if (!world.isClient) {
            world.setBlockState(pos, Blocks.FARMLAND.getDefaultState());
            ElectricItem.manager.use(stack, operationEnergyCost, context.getPlayer());
        }

        return ActionResult.success(world.isClient);
    }
}
