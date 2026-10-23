package ic2.core.block.machine;

import ic2.core.init.IC2ItemVariants;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;

/**
 * Fabric port of BlockMiningPipe: the pipe a miner lays down its shaft, a thin post with a full
 * block tip at the bottom. Only the miner places it - the item cannot be placed by hand, as in
 * 1.12.2 - and it drops the mining pipe item either way.
 */
public class BlockMiningPipe extends Block {
    public enum MiningPipeType implements StringIdentifiable {
        pipe,
        tip;

        @Override
        public String asString() {
            return name();
        }
    }

    public static final EnumProperty<MiningPipeType> TYPE = EnumProperty.of("type", MiningPipeType.class);
    private static final VoxelShape PIPE_SHAPE = Block.createCuboidShape(6.0, 0.0, 6.0, 10.0, 16.0, 10.0);

    public BlockMiningPipe(AbstractBlock.Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(TYPE, MiningPipeType.pipe));
    }

    public BlockState getState(MiningPipeType type) {
        return getDefaultState().with(TYPE, type);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(TYPE);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return state.get(TYPE) == MiningPipeType.pipe ? PIPE_SHAPE : VoxelShapes.fullCube();
    }

    @Override
    public ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state) {
        return new ItemStack(IC2ItemVariants.get("mining_pipe", "pipe"));
    }
}
