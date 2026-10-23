package ic2.core.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.MapColor;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.WallTorchBlock;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.explosion.Explosion;
import org.jetbrains.annotations.Nullable;

/**
 * Fabric port of {@code ic2.core.block.BlockDynamite}: a stick of dynamite placed like a torch,
 * on the floor or on a wall.
 *
 * <p>It goes off - turns into lit sticky dynamite with a two-second fuse - when it gets a redstone
 * signal, when someone breaks it, or from a Dynamite-O-Mote it is linked to ({@link #LINKED}); a
 * blast nearby lights it with a quarter-second fuse. Losing its support pops it off with nothing
 * left, as in 1.12.2, where it dropped no item.
 */
public class BlockDynamite extends Block implements ITnt {
    public static final MapCodec<BlockDynamite> CODEC = createCodec(BlockDynamite::new);
    /** The way it points: up when standing, away from the wall when hanging. */
    public static final DirectionProperty FACING = DirectionProperty.of("facing", dir -> dir != Direction.DOWN);
    /** Whether a Dynamite-O-Mote knows it. */
    public static final BooleanProperty LINKED = BooleanProperty.of("linked");

    private static final VoxelShape STANDING_SHAPE = Block.createCuboidShape(6.0, 0.0, 6.0, 10.0, 10.0, 10.0);
    private static final Direction[] PLACEMENT_ORDER = {Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    public BlockDynamite() {
        this(AbstractBlock.Settings.create()
                .mapColor(MapColor.BRIGHT_RED)
                .breakInstantly()
                .noCollision()
                .ticksRandomly()
                .sounds(BlockSoundGroup.GRASS)
                .pistonBehavior(PistonBehavior.DESTROY));
    }

    public BlockDynamite(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.UP).with(LINKED, false));
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, LINKED);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        Direction facing = state.get(FACING);
        return facing == Direction.UP ? STANDING_SHAPE
                : WallTorchBlock.getBoundingShape(net.minecraft.block.Blocks.WALL_TORCH.getDefaultState().with(WallTorchBlock.FACING, facing));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.empty();
    }

    /** Whether there is something to hold dynamite pointing {@code facing} at the position. */
    public static boolean isSupported(WorldView world, BlockPos pos, Direction facing) {
        BlockPos support = pos.offset(facing.getOpposite());
        return world.getBlockState(support).isSideSolidFullSquare(world, support, facing);
    }

    @Override
    protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        for (Direction dir : PLACEMENT_ORDER) {
            if (isSupported(world, pos, dir)) {
                return true;
            }
        }
        return false;
    }

    /** The clicked side if it holds the stick, otherwise the first side that does. */
    public @Nullable BlockState getPlacementState(WorldView world, BlockPos pos, Direction side) {
        Direction facing = side;
        if (facing == Direction.DOWN || !isSupported(world, pos, facing)) {
            facing = null;
            for (Direction dir : PLACEMENT_ORDER) {
                if (isSupported(world, pos, dir)) {
                    facing = dir;
                    break;
                }
            }
        }
        return facing == null ? null : getDefaultState().with(FACING, facing);
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        return getPlacementState(ctx.getWorld(), ctx.getBlockPos(), ctx.getSide());
    }

    @Override
    protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        if (!oldState.isOf(this)) {
            checkPlacement(world, pos, state);
        }
    }

    @Override
    protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        checkPlacement(world, pos, state);
    }

    @Override
    protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        checkPlacement(world, pos, state);
    }

    @Override
    public void onDestroyedByExplosion(World world, BlockPos pos, Explosion explosion) {
        explode(world, pos, explosion.getCausingEntity(), true);
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient) {
            explode(world, pos, player, false);
        }
        return super.onBreak(world, pos, state, player);
    }

    private void checkPlacement(World world, BlockPos pos, BlockState state) {
        if (world.isClient || !world.getBlockState(pos).isOf(this)) {
            return;
        }

        if (world.isReceivingRedstonePower(pos)) {
            explode(world, pos, null, false);
        } else if (!isSupported(world, pos, state.get(FACING))) {
            world.removeBlock(pos, false);
        }
    }

    /** Takes the stick away and lights it where it was. */
    public static void explode(World world, BlockPos pos, @Nullable LivingEntity igniter, boolean byExplosion) {
        world.removeBlock(pos, false);
        EntityDynamite entity = new EntityStickyDynamite(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        entity.owner = igniter;
        entity.fuse = byExplosion ? 5 : 40;
        world.spawnEntity(entity);
        world.playSound(null, pos, SoundEvents.ENTITY_TNT_PRIMED, SoundCategory.BLOCKS, 1.0F, 1.0F);
    }
}
