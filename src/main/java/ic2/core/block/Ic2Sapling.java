package ic2.core.block;

import java.util.Optional;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SaplingBlock;
import net.minecraft.block.SaplingGenerator;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.feature.ConfiguredFeature;

/**
 * Fabric port of the IC2 rubber sapling.
 *
 * <p>A vanilla {@link SaplingGenerator} names a configured feature to grow; the rubber tree is a
 * feature of its own with no vanilla equivalent, so the generator stays empty and the sapling
 * grows the tree directly, through the same code worldgen uses.
 */
public class Ic2Sapling extends SaplingBlock {

    public static final SaplingGenerator RUBBER_GENERATOR = new SaplingGenerator(
            "rubber",
            Optional.<RegistryKey<ConfiguredFeature<?, ?>>>empty(),
            Optional.<RegistryKey<ConfiguredFeature<?, ?>>>empty(),
            Optional.<RegistryKey<ConfiguredFeature<?, ?>>>empty()
    );

    public Ic2Sapling(SaplingGenerator generator, Settings settings) {
        super(generator, settings);
    }

    public Ic2Sapling(Settings settings) {
        super(RUBBER_GENERATOR, settings);
    }

    public Ic2Sapling() {
        this(AbstractBlock.Settings.copy(Blocks.OAK_SAPLING));
    }

    /**
     * Grows the tree. The sapling is cleared first, because the trunk goes where it stood and the
     * generator refuses to build into a block that is not air.
     */
    @Override
    public void generate(ServerWorld world, BlockPos pos, BlockState state, Random random) {
        world.setBlockState(pos, Blocks.AIR.getDefaultState(), NOTIFY_ALL | FORCE_STATE);

        if (!ic2.core.world.RubberTreeFeature.grow(world, pos, random)) {
            // no room: put the sapling back rather than eating it
            world.setBlockState(pos, state, NOTIFY_ALL | FORCE_STATE);
        }
    }
}
