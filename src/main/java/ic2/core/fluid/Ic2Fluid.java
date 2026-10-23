package ic2.core.fluid;

import ic2.core.ref.FluidName;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.state.StateManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

/**
 * An IC2 fluid as a Minecraft fluid, so it can sit in a {@code FluidVariant} and travel through
 * Fabric's transfer API like water does. 1.12.2 registered these with Forge's fluid registry.
 *
 * <p>Each {@link FluidName} gets a still and a flowing fluid. Only a few of them have a block
 * (hot water, coolant, hot coolant, UU-matter); for the rest {@link #toBlockState} is air, so
 * they exist in tanks, pipes and cells only. None of them has a bucket: IC2 carries fluids in
 * cells.
 */
public abstract class Ic2Fluid extends FlowableFluid {
    protected final FluidName name;

    protected Ic2Fluid(FluidName name) {
        this.name = name;
    }

    public FluidName getFluidName() {
        return name;
    }

    @Override
    public Fluid getStill() {
        return name.getInstance();
    }

    @Override
    public Fluid getFlowing() {
        return name.getFlowing();
    }

    @Override
    public boolean matchesType(Fluid fluid) {
        return fluid == getStill() || fluid == getFlowing();
    }

    @Override
    public Item getBucketItem() {
        return Items.AIR;
    }

    @Override
    protected boolean isInfinite(World world) {
        return false;
    }

    @Override
    protected void beforeBreakingBlock(WorldAccess world, BlockPos pos, BlockState state) {
        Block.dropStacks(state, world, pos, state.hasBlockEntity() ? world.getBlockEntity(pos) : null);
    }

    @Override
    protected int getMaxFlowDistance(WorldView world) {
        return 4;
    }

    @Override
    protected int getLevelDecreasePerBlock(WorldView world) {
        return 1;
    }

    @Override
    public int getTickRate(WorldView world) {
        return 5;
    }

    @Override
    protected float getBlastResistance() {
        return 100.0F;
    }

    @Override
    protected boolean canBeReplacedWith(FluidState state, BlockView world, BlockPos pos, Fluid fluid, Direction direction) {
        return false;
    }

    @Override
    protected BlockState toBlockState(FluidState state) {
        Block block = Registries.BLOCK.get(Identifier.of("ic2", name.name()));
        return block instanceof FluidBlock
                ? block.getDefaultState().with(FluidBlock.LEVEL, getBlockStateLevel(state))
                : Blocks.AIR.getDefaultState();
    }

    public static class Still extends Ic2Fluid {
        public Still(FluidName name) {
            super(name);
        }

        @Override
        public boolean isStill(FluidState state) {
            return true;
        }

        @Override
        public int getLevel(FluidState state) {
            return 8;
        }
    }

    public static class Flowing extends Ic2Fluid {
        public Flowing(FluidName name) {
            super(name);
        }

        @Override
        protected void appendProperties(StateManager.Builder<Fluid, FluidState> builder) {
            super.appendProperties(builder);
            builder.add(LEVEL);
        }

        @Override
        public boolean isStill(FluidState state) {
            return false;
        }

        @Override
        public int getLevel(FluidState state) {
            return state.get(LEVEL);
        }
    }
}
