package ic2.core.block;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraftforge.fluids.Fluid;

/**
 * Fabric 1.21.1 stub for IC2 fluid blocks.
 * Original was ic2.core.block.BlockIC2Fluid extends BlockFluidClassic (Forge).
 * Fabric port extends vanilla {@link FluidBlock} (net.minecraft.block.FluidBlock)
 * where available, otherwise would extend {@link net.minecraft.block.Block}.
 * Stores Forge-compat {@link Fluid} instance plus vanilla FlowableFluid for the super call.
 */
public class BlockIC2Fluid extends FluidBlock {

    private final Fluid fluid;

    public BlockIC2Fluid(Fluid fluid, AbstractBlock.Settings settings) {
        super(toFlowable(fluid), settings);
        this.fluid = fluid;
    }

    /**
     * Convenience ctor matching task description: Settings.copy(Blocks.WATER) is passed by caller,
     * but keep default helper if needed.
     */
    public BlockIC2Fluid(Fluid fluid) {
        this(fluid, AbstractBlock.Settings.copy(Blocks.WATER));
    }

    private static FlowableFluid toFlowable(Fluid fluid) {
        if (fluid != null) {
            net.minecraft.fluid.Fluid mc = fluid.getMinecraftFluid();
            if (mc instanceof FlowableFluid ff) return ff;
            // Try registry lookup by name as fallback (e.g. if getMinecraftFluid returned null)
            try {
                String name = fluid.getName();
                // Fluids.WATER is the safest default; lookup by Identifier not strictly needed here
                // since Fluid shim already does Registries.FLUID lookup.
                if (name != null) {
                    // heuristic: map known IC2 fluids to water/lava for rendering/collision
                    if (name.contains("lava") || name.contains("pahoehoe")) {
                        // LavaFluid is FlowableFluid as well, but Fluids.LAVA is not Flowable? In 1.21.1 it is.
                        // Cast via Fluids.LAVA
                        if (Fluids.LAVA instanceof FlowableFluid lff) return lff;
                    }
                }
            } catch (Exception ignored) {}
        }
        return (FlowableFluid) Fluids.WATER;
    }

    public Fluid getFluid() {
        return fluid;
    }

    public Fluid getIc2Fluid() {
        return fluid;
    }

    public boolean isFluid() {
        return true;
    }

    public boolean isTranslucent() {
        return true;
    }

    public boolean getTranslucent() {
        return true;
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
