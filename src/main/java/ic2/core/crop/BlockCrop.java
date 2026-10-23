package ic2.core.crop;

import com.mojang.serialization.MapCodec;
import ic2.core.block.BlockTileEntity;
import ic2.core.init.IC2Items;
import ic2.core.ref.TeBlock;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

/**
 * The crop stick block. 1.12.2 had no block of its own for it - the generic tile entity block
 * asked TileEntityCrop for its shapes, clicks and drops; here those answers live on the block,
 * which forwards to the tile.
 *
 * <p>The stick has no collision and an outline a bit smaller than a block, is dropped as a
 * single crop stick whatever grew on it, and carries the light its plant gives off in its state,
 * since 1.21 takes a block's light from the state alone.
 */
public class BlockCrop extends BlockTileEntity {
    public static final IntProperty LIGHT = Properties.LEVEL_15;

    private static final VoxelShape OUTLINE = VoxelShapes.cuboid(0.2, -0.0625, 0.2, 0.8, 0.85, 0.8);

    public BlockCrop(Settings settings) {
        super(settings.luminance(state -> state.get(LIGHT)), TeBlock.crop);
        setDefaultState(getStateManager().getDefaultState().with(LIGHT, 0));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return createCodec(BlockCrop::new);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(LIGHT);
    }

    /** @return the state carrying that light, or the same state when it already does */
    public static BlockState withLight(BlockState state, int light) {
        int clamped = Math.max(0, Math.min(15, light));
        return state.contains(LIGHT) && state.get(LIGHT) != clamped ? state.with(LIGHT, clamped) : state;
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return OUTLINE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.empty();
    }

    /** A held item goes to the crop first; whatever it does not want goes on to the item. */
    @Override
    protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos,
                                             PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (stack.isEmpty()) {
            return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        // the trowel acted before the block in 1.12.2 (onItemUseFirst); asked second, a ripe weed
        // would be harvested back to size one before the trowel could pull it
        if (stack.getItem() instanceof ic2.core.item.tool.ItemWeedingTrowel) {
            return ItemActionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        if (world.isClient) {
            return ItemActionResult.SUCCESS;
        }
        if (world.getBlockEntity(pos) instanceof TileEntityCrop crop && crop.rightClick(player, hand)) {
            return ItemActionResult.SUCCESS;
        }
        return ItemActionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
    }

    /** An empty hand harvests. */
    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }
        if (world.getBlockEntity(pos) instanceof TileEntityCrop crop && crop.rightClick(player, Hand.MAIN_HAND)) {
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    @Override
    protected void onBlockBreakStart(BlockState state, World world, BlockPos pos, PlayerEntity player) {
        if (!world.isClient && world.getBlockEntity(pos) instanceof TileEntityCrop crop) {
            crop.leftClick(player);
        }
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (world.getBlockEntity(pos) instanceof TileEntityCrop crop) {
            crop.onBlockBreak();
        }
        return super.onBreak(world, pos, state, player);
    }

    @Override
    protected void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!world.isClient && world.getBlockEntity(pos) instanceof TileEntityCrop crop) {
            crop.onEntityCollision(entity);
        }
    }

    @Override
    protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify);
        if (!world.isClient && world.getBlockEntity(pos) instanceof TileEntityCrop crop) {
            crop.onNeighborChange();
        }
    }

    @Override
    protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        return world.getBlockState(pos.down()).isOf(net.minecraft.block.Blocks.FARMLAND);
    }

    /** Whatever grew there, breaking the block gives back one stick; the plant is picked apart. */
    @Override
    protected List<ItemStack> getDroppedStacks(BlockState state, LootContextParameterSet.Builder builder) {
        return List.of(new ItemStack(IC2Items.CROP_STICK));
    }

    @Override
    public ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state) {
        BlockEntity be = world.getBlockEntity(pos);
        return be instanceof TileEntityCrop crop ? crop.getPickStack() : new ItemStack(IC2Items.CROP_STICK);
    }
}
