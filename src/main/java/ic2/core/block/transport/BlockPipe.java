package ic2.core.block.transport;

import ic2.api.transport.IFluidPipe;
import ic2.core.block.BlockTileEntity;
import ic2.core.block.transport.items.PipeSize;
import ic2.core.block.transport.items.PipeType;
import ic2.core.ref.TeBlock;
import ic2.core.util.LiquidUtil;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

/**
 * The fluid pipe block, one per pipe type and size ({@code ic2:bronze_pipe_small}): a core as thick
 * as the size, an arm towards every side the pipe is connected on and a thin plate on every side
 * that carries a cover.
 *
 * <p>1.12.2 had a single block whose tile entity carried type and size, and drew it with a baked
 * model built from the synced connection and cover bytes. Here type and size are the block, and
 * the two bytes of {@link TileEntityPipe} are mirrored into twelve block state booleans, so the
 * model is a multipart (see {@code tools/gen_pipe_models.py}) and the collision box follows the
 * same booleans, as getAabbs did.
 */
public class BlockPipe extends BlockTileEntity {
    public static final Map<Direction, BooleanProperty> CONNECTIONS = new EnumMap<>(Direction.class);
    public static final Map<Direction, BooleanProperty> COVERS = new EnumMap<>(Direction.class);

    static {
        CONNECTIONS.put(Direction.DOWN, Properties.DOWN);
        CONNECTIONS.put(Direction.UP, Properties.UP);
        CONNECTIONS.put(Direction.NORTH, Properties.NORTH);
        CONNECTIONS.put(Direction.SOUTH, Properties.SOUTH);
        CONNECTIONS.put(Direction.WEST, Properties.WEST);
        CONNECTIONS.put(Direction.EAST, Properties.EAST);
        for (Direction side : Direction.values()) {
            COVERS.put(side, BooleanProperty.of("cover_" + side.getName()));
        }
    }

    private final PipeType pipeType;
    private final PipeSize pipeSize;
    /** The shape of every combination of connections and covers, indexed by the two bit masks. */
    private final VoxelShape[] shapes = new VoxelShape[1 << 12];

    public BlockPipe(Settings settings, PipeType pipeType, PipeSize pipeSize) {
        super(settings, TeBlock.fluid_pipe);
        this.pipeType = pipeType;
        this.pipeSize = pipeSize;

        BlockState state = getStateManager().getDefaultState();
        for (Direction side : Direction.values()) {
            state = state.with(CONNECTIONS.get(side), false).with(COVERS.get(side), false);
        }
        setDefaultState(state);
    }

    public PipeType getPipeType() {
        return pipeType;
    }

    public PipeSize getPipeSize() {
        return pipeSize;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        for (Direction side : Direction.values()) {
            builder.add(CONNECTIONS.get(side), COVERS.get(side));
        }
    }

    /** The connection bits of a pipe state, one per {@link Direction#ordinal()}. */
    public static int getConnectivity(BlockState state) {
        int ret = 0;
        for (Direction side : Direction.values()) {
            if (state.get(CONNECTIONS.get(side))) {
                ret |= 1 << side.ordinal();
            }
        }
        return ret;
    }

    /** The cover bits of a pipe state. */
    public static int getCovers(BlockState state) {
        int ret = 0;
        for (Direction side : Direction.values()) {
            if (state.get(COVERS.get(side))) {
                ret |= 1 << side.ordinal();
            }
        }
        return ret;
    }

    public static BlockState withPipeState(BlockState state, int connectivity, int covers) {
        for (Direction side : Direction.values()) {
            state = state.with(CONNECTIONS.get(side), (connectivity & 1 << side.ordinal()) != 0)
                    .with(COVERS.get(side), (covers & 1 << side.ordinal()) != 0);
        }
        return state;
    }

    /**
     * A pipe put against another pipe or a block that holds fluid connects to it straight away,
     * as TileEntityFluidPipe.onPlaced did; everything else takes the wrench.
     */
    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        BlockState state = getDefaultState();
        Direction facing = context.getSide();
        BlockPos against = context.getBlockPos().offset(facing.getOpposite());
        BlockEntity be = context.getWorld().getBlockEntity(against);
        if (be != null && (be instanceof IFluidPipe || LiquidUtil.isFluidTile(be, facing))) {
            state = state.with(CONNECTIONS.get(facing.getOpposite()), true);
        }
        return state;
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        int key = getConnectivity(state) | getCovers(state) << 6;
        VoxelShape shape = shapes[key];
        if (shape == null) {
            shapes[key] = shape = buildShape(getConnectivity(state), getCovers(state));
        }
        return shape;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return getOutlineShape(state, world, pos, context);
    }

    /**
     * The core, an arm per connected side and, on a connected side, the 0.1 thick cover plate -
     * TileEntityFluidPipe.getAabbs for collision.
     */
    private VoxelShape buildShape(int connectivity, int covers) {
        double th = pipeSize.thickness;
        double sp = (1.0 - th) / 2.0;
        double e = sp + th;
        double ch = 0.1;
        VoxelShape ret = VoxelShapes.cuboid(sp, sp, sp, e, e, e);

        for (Direction side : Direction.values()) {
            if ((connectivity & 1 << side.ordinal()) == 0) {
                continue;
            }
            ret = VoxelShapes.union(ret, switch (side) {
                case DOWN -> VoxelShapes.cuboid(sp, 0, sp, e, sp, e);
                case UP -> VoxelShapes.cuboid(sp, e, sp, e, 1, e);
                case NORTH -> VoxelShapes.cuboid(sp, sp, 0, e, e, sp);
                case SOUTH -> VoxelShapes.cuboid(sp, sp, e, e, e, 1);
                case WEST -> VoxelShapes.cuboid(0, sp, sp, sp, e, e);
                case EAST -> VoxelShapes.cuboid(e, sp, sp, 1, e, e);
            });
            if ((covers & 1 << side.ordinal()) != 0) {
                ret = VoxelShapes.union(ret, switch (side) {
                    case DOWN -> VoxelShapes.cuboid(0, 0, 0, 1, ch, 1);
                    case UP -> VoxelShapes.cuboid(0, 1 - ch, 0, 1, 1, 1);
                    case NORTH -> VoxelShapes.cuboid(0, 0, 0, 1, 1, ch);
                    case SOUTH -> VoxelShapes.cuboid(0, 0, 1 - ch, 1, 1, 1);
                    case WEST -> VoxelShapes.cuboid(0, 0, 0, ch, 1, 1);
                    case EAST -> VoxelShapes.cuboid(1 - ch, 0, 0, 1, 1, 1);
                });
            }
        }
        return ret.simplify();
    }

    /** A pipe is a thin thing in the middle of its block: it neither blocks light nor sky. */
    @Override
    protected boolean isTransparent(BlockState state, BlockView world, BlockPos pos) {
        return true;
    }

    @Override
    protected int getOpacity(BlockState state, BlockView world, BlockPos pos) {
        return 0;
    }

    /** Pipes have no comparator output. */
    @Override
    protected boolean hasComparatorOutput(BlockState state) {
        return false;
    }
}
