package ic2.core.item;

import java.util.List;

import ic2.api.tile.IWrenchable;
import ic2.core.IC2;
import ic2.core.block.TileEntityBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * Fabric port of {@code ic2.core.item.tool.ItemToolWrench}: turns a machine to face another way,
 * or takes it back out of the world in one piece.
 *
 * <p>Right click turns the machine to face the side clicked, sneaking turns it the other way, and
 * holding ALT spins it around the axis of that side. When the facing does not change, the wrench
 * removes the machine instead. Turning costs 1 point of wear, removing costs 10, as in 1.12.2.
 *
 * <p>Machines in the port do not implement {@link IWrenchable} yet, so the facing, whether the
 * block may be taken and what it drops are asked of {@link TileEntityBlock}, as 1.12.2 did.
 */
public class ItemWrench extends ItemIC2 {

    /** What a wrench did to the block it was used on. */
    public enum WrenchResult {
        NOTHING,
        ROTATED,
        REMOVED
    }

    public ItemWrench() {
        this("wrench", new Settings().maxCount(1).maxDamage(120));
    }

    protected ItemWrench(String name, Settings settings) {
        super(name, settings);
    }

    /** Whether the wrench still has the {@code amount} of wear a job would cost. */
    public boolean canTakeDamage(ItemStack stack, int amount) {
        return stack.getMaxDamage() - stack.getDamage() > amount;
    }

    /** Charge the wrench for a job. */
    public void damage(ItemStack stack, int amount, PlayerEntity player) {
        stack.damage(amount, player, EquipmentSlot.MAINHAND);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        PlayerEntity player = context.getPlayer();
        ItemStack stack = context.getStack();
        if (player == null || !canTakeDamage(stack, 1)) {
            return ActionResult.FAIL;
        }

        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        if (world.getBlockEntity(pos) instanceof ic2.api.transport.IPipe pipe) {
            // a pipe connects or parts the side picked by where the click landed - the middle of
            // a face means that face, its edge strips the side they lie towards
            net.minecraft.util.math.Vec3d hit = context.getHitPos();
            Direction side = ic2.core.util.RotationUtil.rotateByHit(context.getSide(),
                    (float) (hit.x - pos.getX()), (float) (hit.y - pos.getY()), (float) (hit.z - pos.getZ()));
            flipPipeConnection(world, pos, pipe, side);
            if (!world.isClient) {
                world.playSound(null, player.getBlockPos(), ic2.core.init.IC2Sounds.TOOLS_WRENCH, SoundCategory.PLAYERS, 1.0F, 1.0F);
            }
            return ActionResult.SUCCESS;
        }

        WrenchResult result = wrenchBlock(world, pos, context.getSide(), player,
                canTakeDamage(stack, 10));

        if (result == WrenchResult.NOTHING) {
            return ActionResult.FAIL;
        }

        if (!world.isClient) {
            damage(stack, result == WrenchResult.ROTATED ? 1 : 10, player);
        } else {
            world.playSound(player, context.getBlockPos(), SoundEvents.BLOCK_ANVIL_USE, SoundCategory.BLOCKS, 0.3f, 1.8f);
        }

        return world.isClient ? ActionResult.PASS : ActionResult.SUCCESS;
    }

    /** Flips the pipe's side, and the side of the pipe across it to match, so the two stay joined or apart. */
    public static void flipPipeConnection(World world, BlockPos pos, ic2.api.transport.IPipe pipe, Direction side) {
        pipe.flipConnection(side);
        if (world.getBlockEntity(pos.offset(side)) instanceof ic2.api.transport.IPipe other
                && pipe.isConnected(side) != other.isConnected(side.getOpposite())) {
            other.flipConnection(side.getOpposite());
        }
    }

    /**
     * Turn the block, or take it out when turning would change nothing.
     *
     * @param remove whether the wrench has enough left to pay for a removal
     */
    public static WrenchResult wrenchBlock(World world, BlockPos pos, Direction side, PlayerEntity player,
                                           boolean remove) {
        BlockState state = world.getBlockState(pos);
        if (state.isAir()) {
            return WrenchResult.NOTHING;
        }

        BlockEntity blockEntity = world.getBlockEntity(pos);

        Direction currentFacing = facingOf(blockEntity, world, pos);
        if (currentFacing == null) {
            return WrenchResult.NOTHING;
        }

        Direction newFacing = nextFacing(currentFacing, side, player);

        if (newFacing != currentFacing && setFacing(blockEntity, world, pos, newFacing, player)) {
            return WrenchResult.ROTATED;
        }

        if (!remove || !canRemove(blockEntity, world, pos, player)) {
            return WrenchResult.NOTHING;
        }

        if (!world.isClient) {
            List<ItemStack> drops = wrenchDrops(blockEntity, world, pos, state, player);
            world.breakBlock(pos, false, player);
            for (ItemStack drop : drops) {
                if (!drop.isEmpty()) {
                    Block.dropStack(world, pos, drop);
                }
            }
        }

        return WrenchResult.REMOVED;
    }

    /** Which way the wrench turns the block, the 1.12.2 rules. */
    private static Direction nextFacing(Direction current, Direction side, PlayerEntity player) {
        if (!IC2.keyboard.isAltKeyDown(player)) {
            return player.isSneaking() ? side.getOpposite() : side;
        }

        Direction.Axis axis = side.getAxis();
        boolean forwards = side.getDirection() == Direction.AxisDirection.POSITIVE
                ? !player.isSneaking()
                : player.isSneaking();

        Direction facing = current;
        int steps = forwards ? 1 : 3;
        for (int i = 0; i < steps; i++) {
            facing = rotateAround(facing, axis);
        }
        return facing;
    }

    /** {@code EnumFacing.rotateAround} from 1.12.2, which 1.21.1 has no direct equivalent of. */
    private static Direction rotateAround(Direction facing, Direction.Axis axis) {
        return switch (axis) {
            case X -> facing.rotateClockwise(Direction.Axis.X);
            case Y -> facing.rotateYClockwise();
            case Z -> facing.rotateClockwise(Direction.Axis.Z);
        };
    }

    private static Direction facingOf(BlockEntity blockEntity, World world, BlockPos pos) {
        if (blockEntity instanceof IWrenchable wrenchable) return wrenchable.getFacing(world, pos);
        if (blockEntity instanceof TileEntityBlock machine) return machine.getFacing();
        return null;
    }

    private static boolean setFacing(BlockEntity blockEntity, World world, BlockPos pos, Direction facing,
                                     PlayerEntity player) {
        if (blockEntity instanceof IWrenchable wrenchable) {
            return wrenchable.canSetFacing(world, pos, facing, player)
                    && wrenchable.setFacing(world, pos, facing, player);
        }
        if (blockEntity instanceof TileEntityBlock machine) {
            return machine.setFacingWrench(facing, player);
        }
        return false;
    }

    private static boolean canRemove(BlockEntity blockEntity, World world, BlockPos pos, PlayerEntity player) {
        if (blockEntity instanceof IWrenchable wrenchable) return wrenchable.wrenchCanRemove(world, pos, player);
        return blockEntity instanceof TileEntityBlock machine && machine.wrenchCanRemove(player);
    }

    private static List<ItemStack> wrenchDrops(BlockEntity blockEntity, World world, BlockPos pos, BlockState state,
                                               PlayerEntity player) {
        if (blockEntity instanceof IWrenchable wrenchable) {
            List<ItemStack> drops = wrenchable.getWrenchDrops(world, pos, state, blockEntity, player, 0);
            if (drops != null && !drops.isEmpty()) return drops;
        }
        if (blockEntity instanceof TileEntityBlock machine) {
            return machine.getWrenchDrops(player);
        }
        return List.of(new ItemStack(state.getBlock()));
    }
}
