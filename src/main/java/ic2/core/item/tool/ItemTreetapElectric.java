package ic2.core.item.tool;

import java.util.List;

import ic2.api.item.ElectricItem;
import ic2.core.block.BlockRubWood;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;

/**
 * Fabric port of {@code ic2.core.item.tool.ItemTreetapElectric}: taps a rubber tree for 50 EU
 * instead of wearing a treetap out.
 *
 * <p>The tapping itself lives on {@link BlockRubWood}, which is also what the plain treetap
 * triggers, so both give the same 1 to 3 resin and leave the spot dry.
 */
public class ItemTreetapElectric extends ItemElectricTool {

    public ItemTreetapElectric() {
        super("electric_treetap", 50, 10000, 100, 1, 1.0f, BlockTags.INCORRECT_FOR_WOODEN_TOOL, List.of());
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        ItemStack stack = context.getStack();
        World world = context.getWorld();

        if (!(world.getBlockState(context.getBlockPos()).getBlock() instanceof BlockRubWood)) {
            return ActionResult.PASS;
        }
        if (!ElectricItem.manager.canUse(stack, operationEnergyCost)) {
            return ActionResult.PASS;
        }

        ActionResult result = BlockRubWood.tap(world, context.getBlockPos(), world.getBlockState(context.getBlockPos()));
        if (result.isAccepted()) {
            ElectricItem.manager.use(stack, operationEnergyCost, context.getPlayer());
        }
        return result;
    }
}
