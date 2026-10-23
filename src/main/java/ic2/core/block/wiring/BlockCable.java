package ic2.core.block.wiring;

import ic2.api.energy.tile.IColoredEnergyTile;
import ic2.api.energy.tile.IEnergyTile;
import ic2.core.block.BlockTileEntity;
import ic2.core.block.BlockWall;
import ic2.core.init.IC2ClassicBlocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.property.EnumProperty;
import ic2.core.ref.TeBlock;
import ic2.core.util.Ic2Color;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.DyeColor;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

/**
 * The cable block: a thin core with an arm towards every neighbour it is wired to.
 *
 * <p>1.12.2 built the shape from the cable's thickness and the sides it was connected on; here the
 * connections are block state properties, so the model can be a multipart and the collision box
 * follows the same six booleans.
 */
public class BlockCable extends BlockTileEntity {
    private static final Map<Direction, BooleanProperty> CONNECTIONS = new EnumMap<>(Direction.class);

    static {
        CONNECTIONS.put(Direction.NORTH, Properties.NORTH);
        CONNECTIONS.put(Direction.EAST, Properties.EAST);
        CONNECTIONS.put(Direction.SOUTH, Properties.SOUTH);
        CONNECTIONS.put(Direction.WEST, Properties.WEST);
        CONNECTIONS.put(Direction.UP, Properties.UP);
        CONNECTIONS.put(Direction.DOWN, Properties.DOWN);
    }

    private final CableType cableType;
    private final int insulation;
    private final VoxelShape core;
    private final Map<Direction, VoxelShape> arms = new EnumMap<>(Direction.class);

    public BlockCable(Settings settings, TeBlock teBlock, CableType cableType, int insulation) {
        super(settings, teBlock);

        this.cableType = cableType;
        this.insulation = insulation;

        // the thickness of the original: the bare cable plus two pixels per layer of insulation
        double thickness = (cableType.thickness + insulation * 2 * 0.0625f) * 16.0;
        double min = (16.0 - thickness) / 2.0;
        double max = 16.0 - min;

        this.core = Block.createCuboidShape(min, min, min, max, max, max);
        arms.put(Direction.DOWN, Block.createCuboidShape(min, 0, min, max, min, max));
        arms.put(Direction.UP, Block.createCuboidShape(min, max, min, max, 16, max));
        arms.put(Direction.NORTH, Block.createCuboidShape(min, min, 0, max, max, min));
        arms.put(Direction.SOUTH, Block.createCuboidShape(min, min, max, max, max, 16));
        arms.put(Direction.WEST, Block.createCuboidShape(0, min, min, min, max, max));
        arms.put(Direction.EAST, Block.createCuboidShape(max, min, min, 16, max, max));

        BlockState state = getStateManager().getDefaultState();
        for (BooleanProperty property : CONNECTIONS.values()) {
            state = state.with(property, false);
        }
        setDefaultState(state.with(FOAM, CableFoam.none));
    }

    /** How far the cable is buried in construction foam; see {@link TileEntityCable#changeFoam}. */
    public static final EnumProperty<CableFoam> FOAM = EnumProperty.of("foam", CableFoam.class);

    /** The detector and splitter cables get their own block entity; see {@link TileEntityCable#create}. */
    @Override
    public net.minecraft.block.entity.BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return TileEntityCable.create(pos, state);
    }

    public CableType getCableType() {
        return cableType;
    }

    public int getInsulation() {
        return insulation;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        for (BooleanProperty property : CONNECTIONS.values()) {
            builder.add(property);
        }
        builder.add(FOAM);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        return withConnections(getDefaultState(), context.getWorld(), context.getBlockPos());
    }

    /** The state with every arm set by what is around the cable now. */
    public BlockState withConnections(BlockState state, WorldView world, BlockPos pos) {
        for (Direction side : Direction.values()) {
            state = state.with(CONNECTIONS.get(side), connectsTo(state, world, pos.offset(side), side));
        }
        return state;
    }

    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, Direction side, BlockState neighbourState,
                                                   WorldAccess world, BlockPos pos, BlockPos neighbourPos) {
        return state.with(CONNECTIONS.get(side), connectsTo(state, world, neighbourPos, side));
    }

    /**
     * A cable connects to another cable and to anything that takes or gives energy, unless both
     * are painted and in different colours - canInteractWith in 1.12.2.
     */
    private static boolean connectsTo(BlockState own, WorldView world, BlockPos pos, Direction side) {
        BlockState other = world.getBlockState(pos);
        boolean energy = other.getBlock() instanceof BlockCable || world.getBlockEntity(pos) instanceof IEnergyTile;
        if (!energy) {
            return false;
        }

        DyeColor ownColor = colorOf(own);
        DyeColor otherColor = other.getBlock() instanceof BlockCable
                ? colorOf(other)
                : world.getBlockEntity(pos) instanceof IColoredEnergyTile colored ? colored.getColor(side.getOpposite()) : null;
        return ownColor == null || otherColor == null || ownColor == otherColor;
    }

    /** The paint of a cable state, or null for an unpainted one - black counts as unpainted. */
    public static DyeColor colorOf(BlockState state) {
        if (!state.contains(BlockColoredCable.COLOR)) {
            return null;
        }
        Ic2Color color = state.get(BlockColoredCable.COLOR);
        return color == Ic2Color.black ? null : color.mcColor;
    }

    /**
     * Foam fills the whole block: soft foam only to aim at, hardened foam also to stand on, as
     * getAabbs gave it in 1.12.2.
     */
    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return state.get(FOAM) != CableFoam.none ? VoxelShapes.fullCube() : cableShape(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return state.get(FOAM) == CableFoam.hardened ? VoxelShapes.fullCube() : cableShape(state);
    }

    /** The core and the arms of the bare cable. */
    private VoxelShape cableShape(BlockState state) {
        VoxelShape shape = core;
        for (Map.Entry<Direction, BooleanProperty> connection : CONNECTIONS.entrySet()) {
            if (state.get(connection.getValue())) {
                shape = VoxelShapes.combine(shape, arms.get(connection.getKey()), BooleanBiFunction.OR);
            }
        }
        return shape.simplify();
    }

    /**
     * A cable is a thin thing in the middle of its block, so it neither blocks light nor sky -
     * until hardened foam walls it in, which is as dark as any wall.
     */
    @Override
    protected boolean isTransparent(BlockState state, BlockView world, BlockPos pos) {
        return state.get(FOAM) != CableFoam.hardened;
    }

    @Override
    protected int getOpacity(BlockState state, BlockView world, BlockPos pos) {
        return state.get(FOAM) == CableFoam.hardened ? world.getMaxLightLevel() : 0;
    }

    /**
     * Foam is mined like the foam block, hardened foam like the foam wall - getHardness in
     * 1.12.2 - with the same tool rules as those blocks.
     */
    @Override
    protected float calcBlockBreakingDelta(BlockState state, PlayerEntity player, BlockView world, BlockPos pos) {
        BlockState like = switch (state.get(FOAM)) {
            case soft -> IC2ClassicBlocks.FOAM == null ? null : IC2ClassicBlocks.FOAM.getDefaultState();
            case hardened -> BlockWall.get(BlockWall.DEFAULT_COLOR) == null ? null : BlockWall.get(BlockWall.DEFAULT_COLOR).getDefaultState();
            case none -> null;
        };
        if (like == null) {
            return super.calcBlockBreakingDelta(state, player, world, pos);
        }
        float hardness = like.getHardness(world, pos);
        if (hardness < 0.0F) {
            return 0.0F;
        }
        int divisor = player.canHarvest(like) ? 30 : 100;
        return player.getBlockBreakingSpeed(like) / hardness / divisor;
    }
}
