package ic2.core.block;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.FluidBlock;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;

/**
 * The block of an IC2 fluid that can stand in the world: hot water, coolant, hot coolant and
 * UU-matter. 1.12.2 extended Forge's BlockFluidClassic; here it is a vanilla {@link FluidBlock} of
 * the registered IC2 fluid.
 */
public class BlockIC2Fluid extends FluidBlock {
    public BlockIC2Fluid(Fluid fluid, AbstractBlock.Settings settings) {
        super((FlowableFluid) fluid, settings);
    }

    /** The still fluid the block is made of. */
    public Fluid getIc2Fluid() {
        return fluid;
    }

    @Override
    protected boolean isTransparent(BlockState state, BlockView world, BlockPos pos) {
        return true;
    }

    @Override
    protected int getOpacity(BlockState state, BlockView world, BlockPos pos) {
        return 0;
    }
}
