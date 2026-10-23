package ic2.core.item.logistics;

import ic2.core.block.transport.cover.CoverProperty;
import ic2.core.block.transport.cover.CoverRegistry;
import ic2.core.block.transport.cover.ICoverHolder;
import ic2.core.block.transport.cover.IFluidConsumingCover;
import ic2.core.item.ItemIC2;
import ic2.core.util.LiquidUtil;
import ic2.core.util.RotationUtil;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * The pump attachment, a cover for a side of a fluid pipe. Every tick it pulls fluid out of the
 * block beside that side into the pipe - 640 mB a second (32 mB a tick) for the LV pump, 2560 mB
 * (128 mB a tick) for the MV one - provided the side is connected. Fluid may come in through the
 * cover but never go out of it.
 *
 * <p>Right click a pipe to put it on: the middle of a face means that face, the edge strips of a
 * face the side they lie towards. The crowbar takes it off again.
 */
public class ItemPumpCover extends ItemIC2 implements IFluidConsumingCover {
    private final PumpCoverType type;

    public ItemPumpCover(String name, PumpCoverType type) {
        super(name);
        this.type = type;
        CoverRegistry.register(new ItemStack(this));
    }

    public PumpCoverType getType() {
        return type;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        PlayerEntity player = context.getPlayer();
        ItemStack stack = context.getStack();
        if (!(world.getBlockEntity(pos) instanceof ICoverHolder holder)) {
            return ActionResult.PASS;
        }

        Vec3d hit = context.getHitPos();
        Direction side = RotationUtil.rotateByHit(context.getSide(),
                (float) (hit.x - pos.getX()), (float) (hit.y - pos.getY()), (float) (hit.z - pos.getZ()));
        if (holder.canPlaceCover(world, pos, side, stack)) {
            if (!world.isClient) {
                holder.placeCover(world, pos, side, stack.copyWithCount(1));
                stack.decrement(1);
                if (player != null) {
                    player.sendMessage(Text.literal("Attachment placed"), false);
                }
            }
        }

        return world.isClient ? ActionResult.PASS : ActionResult.SUCCESS;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType options) {
        super.appendTooltip(stack, context, tooltip, options);
        tooltip.add(Text.literal("Transfer rate: " + type.transferRate + " mb/sec (as attachment)"));
    }

    @Override
    public boolean isSuitableFor(ItemStack stack, Set<CoverProperty> properties) {
        return properties.contains(CoverProperty.FluidConsuming);
    }

    /** Pulls up to a twentieth of the rate out of the block beside the cover into the holder. */
    @Override
    public boolean onTick(ItemStack stack, ICoverHolder holder, Direction side) {
        BlockEntity tile = (BlockEntity) holder;
        LiquidUtil.AdjacentFluidHandler target = LiquidUtil.getAdjacentHandler(tile, side);
        if (target != null) {
            LiquidUtil.transfer(target.handler, target.dir.getOpposite(), tile, LiquidUtil.mb(type.transferRate / 20));
        }
        return false;
    }

    @Override
    public boolean allowsInput(ItemStack stack) {
        return false;
    }

    @Override
    public boolean allowsInput(FluidVariant fluid) {
        return true;
    }

    @Override
    public boolean allowsOutput(ItemStack stack) {
        return false;
    }

    @Override
    public boolean allowsOutput(FluidVariant fluid) {
        return false;
    }
}
