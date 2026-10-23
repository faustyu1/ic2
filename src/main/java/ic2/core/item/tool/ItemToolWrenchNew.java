package ic2.core.item.tool;

import ic2.api.transport.IPipe;
import ic2.core.block.TileEntityBlock;
import ic2.core.init.IC2Sounds;
import ic2.core.item.ItemWrench;
import ic2.core.recipe.OreDictionaryCompat;
import ic2.core.util.RotationUtil;
import java.util.List;
import net.minecraft.block.entity.BlockEntity;
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
 * The new wrench, the pipe tool. Right click a pipe to connect or disconnect the side picked by
 * where the click landed (the middle of a face means that face, its edge strips the side they lie
 * towards); a pipe on the other side follows, so the two stay joined or apart together. On a
 * machine it sets the facing to the picked side instead. It never wears out from this; mined with
 * it, a machine drops itself, as with the wrench.
 */
public class ItemToolWrenchNew extends ItemWrench {
    public ItemToolWrenchNew() {
        super("wrench_new", new Settings().maxCount(1).maxDamage(120));
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        PlayerEntity player = context.getPlayer();
        if (world.getBlockState(pos).isAir()) {
            return ActionResult.FAIL;
        }

        Vec3d hit = context.getHitPos();
        Direction newFacing = RotationUtil.rotateByHit(context.getSide(),
                (float) (hit.x - pos.getX()), (float) (hit.y - pos.getY()), (float) (hit.z - pos.getZ()));
        BlockEntity be = world.getBlockEntity(pos);

        if (be instanceof IPipe target) {
            flipConnection(world, pos, target, newFacing);
        } else if (be instanceof TileEntityBlock machine) {
            if (!world.isClient) {
                machine.setFacingWrench(newFacing, player);
            }
        } else {
            return ActionResult.FAIL;
        }

        if (!world.isClient && player != null) {
            world.playSound(null, player.getBlockPos(), IC2Sounds.TOOLS_WRENCH, SoundCategory.PLAYERS, 1.0F, 1.0F);
        }
        return ActionResult.SUCCESS;
    }

    /** Flips the pipe's side, and the side of the pipe across it to match. */
    public static void flipConnection(World world, BlockPos pos, IPipe target, Direction side) {
        target.flipConnection(side);
        if (world.getBlockEntity(pos.offset(side)) instanceof IPipe other
                && target.isConnected(side) != other.isConnected(side.getOpposite())) {
            other.flipConnection(side.getOpposite());
        }
    }

    @Override
    public boolean canRepair(ItemStack stack, ItemStack ingredient) {
        return ingredient.isIn(OreDictionaryCompat.getTag("ingotBronze"));
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType options) {
        tooltip.add(Text.translatable("key.attack").append(":"));
        tooltip.add(Text.literal(" Safely mine IC2 machines (Yes you will get the machine and not the machine block)"));
        tooltip.add(Text.empty());
        tooltip.add(Text.translatable("key.use").append(":"));
        tooltip.add(Text.literal(" Set the machine facing (rotate)"));
        tooltip.add(Text.literal(" Connect pipes together and to covers"));
    }
}
