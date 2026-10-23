package ic2.core.item.tool;

import ic2.core.block.transport.cover.ICoverHolder;
import ic2.core.init.IC2Sounds;
import ic2.core.item.ItemIC2;
import ic2.core.recipe.OreDictionaryCompat;
import ic2.core.util.RotationUtil;
import java.util.List;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * The crowbar ("Attachment Remover"): right click a side of a pipe to take the cover off it, which
 * drops as an item. The side is picked the way the cover was put on - the middle of a face means
 * that face, the edge strips of a face the side they lie towards. Each cover taken off costs one of
 * its 250 uses; it is repaired with bronze.
 */
public class ItemToolCrowbar extends ItemIC2 {
    public ItemToolCrowbar() {
        super("crowbar", new Settings().maxCount(1).maxDamage(250));
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        PlayerEntity player = context.getPlayer();
        if (world.getBlockState(pos).isAir()) {
            return ActionResult.FAIL;
        }
        if (!(world.getBlockEntity(pos) instanceof ICoverHolder target)) {
            return ActionResult.FAIL;
        }

        Vec3d hit = context.getHitPos();
        Direction side = RotationUtil.rotateByHit(context.getSide(),
                (float) (hit.x - pos.getX()), (float) (hit.y - pos.getY()), (float) (hit.z - pos.getZ()));
        if (target.canRemoveCover(world, pos, side) && !world.isClient) {
            target.removeCover(world, pos, side);
            if (player != null) {
                context.getStack().damage(1, player, EquipmentSlot.MAINHAND);
                world.playSound(null, player.getBlockPos(), IC2Sounds.TOOLS_CROWBAR, SoundCategory.PLAYERS, 1.0F, 1.0F);
                player.sendMessage(Text.literal("Attachment removed"), false);
            }
        }

        return world.isClient ? ActionResult.PASS : ActionResult.SUCCESS;
    }

    @Override
    public boolean canRepair(ItemStack stack, ItemStack ingredient) {
        return ingredient.isIn(OreDictionaryCompat.getTag("ingotBronze"));
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType options) {
        super.appendTooltip(stack, context, tooltip, options);
        tooltip.add(Text.translatable("key.use").append(":"));
        tooltip.add(Text.literal(" Remove attachments from blocks"));
    }
}
