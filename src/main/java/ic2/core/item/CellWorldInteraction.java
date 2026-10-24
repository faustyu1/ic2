package ic2.core.item;

import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.FluidDrainable;
import net.minecraft.block.FluidFillable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

/**
 * Fluid cells in the world, as buckets: an empty cell scoops up the source block it is pointed at
 * (water, lava, or an IC2 fluid that has a block), and a full cell pours its fluid out where a
 * bucket would. The cell swaps through its Fabric item fluid storage, so the same rules decide
 * what fits as in a tank. 1.12.2 got this from Forge's universal fluid container handling.
 */
public final class CellWorldInteraction {
    private CellWorldInteraction() {}

    public static void register() {
        UseItemCallback.EVENT.register(CellWorldInteraction::use);
    }

    private static boolean isCell(ItemStack stack) {
        Identifier id = net.minecraft.registry.Registries.ITEM.getId(stack.getItem());
        return id.getNamespace().equals("ic2") && (id.getPath().equals("fluid_cell") || id.getPath().startsWith("fluid_cell_"));
    }

    private static TypedActionResult<ItemStack> use(PlayerEntity player, World world, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (stack.isEmpty() || player.isSpectator() || !isCell(stack)) {
            return TypedActionResult.pass(stack);
        }
        Storage<FluidVariant> cell = FluidStorage.ITEM.find(stack, ContainerItemContext.ofPlayerHand(player, hand));
        if (cell == null) {
            return TypedActionResult.pass(stack);
        }
        FluidVariant inside = StorageUtil.findStoredResource(cell);
        BlockHitResult hit = raycast(world, player, inside == null ? RaycastContext.FluidHandling.SOURCE_ONLY : RaycastContext.FluidHandling.NONE);
        if (hit.getType() != HitResult.Type.BLOCK || !world.canPlayerModifyAt(player, hit.getBlockPos())) {
            return TypedActionResult.pass(stack);
        }
        boolean done = inside == null ? scoop(world, player, hit.getBlockPos(), cell) : pour(world, player, hit, cell, inside);
        return done ? TypedActionResult.success(player.getStackInHand(hand), world.isClient) : TypedActionResult.pass(stack);
    }

    /** Takes the source block at {@code pos} into the empty cell. */
    private static boolean scoop(World world, PlayerEntity player, BlockPos pos, Storage<FluidVariant> cell) {
        BlockState state = world.getBlockState(pos);
        FluidState fluidState = state.getFluidState();
        if (!fluidState.isStill() || !(state.getBlock() instanceof FluidDrainable drainable)) {
            return false;
        }
        Fluid fluid = fluidState.getFluid();
        try (Transaction transaction = Transaction.openOuter()) {
            if (cell.insert(FluidVariant.of(fluid), FluidConstants.BUCKET, transaction) != FluidConstants.BUCKET) {
                return false;
            }
            if (world.isClient) {
                return true;
            }
            if (state.getBlock() instanceof FluidBlock) {
                // an IC2 fluid has no bucket item, so the vanilla drain would report nothing taken
                world.setBlockState(pos, net.minecraft.block.Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL_AND_REDRAW);
            } else if (drainable.tryDrainFluid(player, world, pos, state).isEmpty()) {
                return false;
            }
            if (!player.getAbilities().creativeMode) {
                transaction.commit();
            }
        }
        SoundEvent sound = fluid.isIn(FluidTags.LAVA) ? SoundEvents.ITEM_BUCKET_FILL_LAVA : SoundEvents.ITEM_BUCKET_FILL;
        world.playSound(null, pos, sound, SoundCategory.BLOCKS, 1.0F, 1.0F);
        return true;
    }

    /** Pours the full cell out against the face it is pointed at, where a bucket would place its fluid. */
    private static boolean pour(World world, PlayerEntity player, BlockHitResult hit, Storage<FluidVariant> cell, FluidVariant inside) {
        Fluid fluid = inside.getFluid();
        if (fluid.getDefaultState().getBlockState().isAir()) {
            return false; // a gas or a fluid that only lives in tanks
        }
        BlockPos pos = hit.getBlockPos();
        BlockState state = world.getBlockState(pos);
        if (!canPourInto(world, pos, state, fluid)) {
            pos = pos.offset(hit.getSide());
            state = world.getBlockState(pos);
            if (!canPourInto(world, pos, state, fluid)) {
                return false;
            }
        }
        try (Transaction transaction = Transaction.openOuter()) {
            if (cell.extract(inside, FluidConstants.BUCKET, transaction) != FluidConstants.BUCKET) {
                return false;
            }
            if (world.isClient) {
                return true;
            }
            if (world.getDimension().ultrawarm() && fluid.isIn(FluidTags.WATER)) {
                world.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.5F, 2.6F);
                ((net.minecraft.server.world.ServerWorld) world).spawnParticles(ParticleTypes.LARGE_SMOKE,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.0);
            } else if (state.getBlock() instanceof FluidFillable fillable && fluid == Fluids.WATER) {
                fillable.tryFillWithFluid(world, pos, state, Fluids.WATER.getStill(false));
            } else {
                if (!state.isAir() && !state.isLiquid()) {
                    world.breakBlock(pos, true);
                }
                world.setBlockState(pos, fluid.getDefaultState().getBlockState(), Block.NOTIFY_ALL_AND_REDRAW);
            }
            if (!player.getAbilities().creativeMode) {
                transaction.commit();
            }
        }
        SoundEvent sound = fluid.isIn(FluidTags.LAVA) ? SoundEvents.ITEM_BUCKET_EMPTY_LAVA : SoundEvents.ITEM_BUCKET_EMPTY;
        world.playSound(null, pos, sound, SoundCategory.BLOCKS, 1.0F, 1.0F);
        return true;
    }

    private static boolean canPourInto(World world, BlockPos pos, BlockState state, Fluid fluid) {
        return state.isAir() || state.canBucketPlace(fluid)
                || state.getBlock() instanceof FluidFillable fillable && fillable.canFillWithFluid(null, world, pos, state, fluid);
    }

    private static BlockHitResult raycast(World world, PlayerEntity player, RaycastContext.FluidHandling fluids) {
        Vec3d eye = player.getEyePos();
        Vec3d end = eye.add(player.getRotationVector().multiply(player.getBlockInteractionRange()));
        return world.raycast(new RaycastContext(eye, end, RaycastContext.ShapeType.OUTLINE, fluids, player));
    }
}
