package ic2.core.item.tool;

import ic2.api.crops.Crops;
import ic2.core.crop.TileEntityCrop;
import ic2.core.init.IC2ItemVariants;
import ic2.core.item.ItemIC2;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;

/**
 * Fabric port of ItemWeedingTrowel - pulls weed out of a crop stick and leaves the stick.
 *
 * <p>The weed comes out as one weed item per size it had grown to. On anything that is not weed
 * the trowel does nothing, so a crop cannot be lost by a misclick.
 */
public class ItemWeedingTrowel extends ItemIC2 {
    public ItemWeedingTrowel() {
        super("weeding_trowel", new Item.Settings().maxCount(1));
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        if (!(world.getBlockEntity(context.getBlockPos()) instanceof TileEntityCrop crop) || crop.getCrop() != Crops.weed) {
            return ActionResult.PASS;
        }

        if (!world.isClient) {
            Item weed = IC2ItemVariants.get("crop_res", "weed");
            if (weed != null) {
                Block.dropStack(world, context.getBlockPos(), new ItemStack(weed, crop.getCurrentSize()));
            }
            crop.reset();
        }
        return ActionResult.success(world.isClient);
    }
}
