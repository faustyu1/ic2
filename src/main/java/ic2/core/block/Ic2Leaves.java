package ic2.core.block;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

/**
 * Fabric port of IC2 rubber leaves.
 * Original decompiled: extends BlockLeaves with PropertyEnum LeavesType.
 * Fabric 1.21.1: extends LeavesBlock(Settings). Settings copy Blocks.OAK_LEAVES.
 */
public class Ic2Leaves extends LeavesBlock {

    public Ic2Leaves(Settings settings) {
        super(settings);
    }

    public Ic2Leaves() {
        super(AbstractBlock.Settings.copy(Blocks.OAK_LEAVES));
    }

    @Override
    protected boolean hasRandomTicks(BlockState state) {
        return super.hasRandomTicks(state);
    }

    // Yarn 1.21.1: hasRandomTicks = Mojang isRandomlyTicking
    // decay stub — delegates to LeavesBlock vanilla logic
    // Bridge for Mojang mapping name
    public boolean isRandomlyTicking(BlockState state) {
        return hasRandomTicks(state);
    }

    @Override
    protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        super.randomTick(state, world, pos, random);
    }
}
