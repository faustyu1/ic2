package ic2.core.world;

import com.mojang.serialization.Codec;
import ic2.core.block.BlockRubWood;
import ic2.core.init.IC2ClassicBlocks;
import static net.minecraft.block.Block.NOTIFY_ALL;

import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * Fabric port of WorldGenRubTree - the rubber tree, shape and all.
 *
 * <p>A vanilla {@code minecraft:tree} cannot describe it: the trunk carries resin spots, the
 * first one likely and every next one less so, and the leaves thin out towards the bottom rather
 * than sitting in a blob on top. The generation is the original's, so a tree grown here looks the
 * way it did in 1.12.2.
 */
public class RubberTreeFeature extends Feature<DefaultFeatureConfig> {
    public static final Identifier ID = Identifier.of("ic2", "rubber_tree");
    public static final RubberTreeFeature INSTANCE = new RubberTreeFeature(DefaultFeatureConfig.CODEC);

    public static final int MAX_HEIGHT = 8;

    public RubberTreeFeature(Codec<DefaultFeatureConfig> codec) {
        super(codec);
    }

    public static void register() {
        if (!Registries.FEATURE.containsId(ID)) {
            Registry.register(Registries.FEATURE, ID, INSTANCE);
        }
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        BlockPos origin = context.getOrigin();
        return grow(world, origin, context.getRandom());
    }

    /**
     * Grows one tree with its foot at {@code pos}.
     *
     * @return false when there is no room, in which case nothing was placed
     */
    public static boolean grow(WorldAccess world, BlockPos pos, Random random) {
        if (world == null || IC2ClassicBlocks.RUBBER_WOOD == null || IC2ClassicBlocks.RUBBER_LEAVES == null) {
            return false;
        }

        int height = getGrowHeight(world, pos);
        if (height < 2) {
            return false;
        }
        height -= random.nextInt(height / 2 + 1);

        BlockState leaves = IC2ClassicBlocks.RUBBER_LEAVES.getDefaultState();
        // leaves know how far they are from the trunk, or they decay the moment the tree is done
        BlockState plainLog = IC2ClassicBlocks.RUBBER_WOOD.getDefaultState()
                .with(BlockRubWood.STATE, BlockRubWood.RubberWoodState.plain_y);

        // the first log is the likeliest to carry a spot, and each one placed makes the next less so
        int holeChance = 25;
        BlockPos.Mutable cursor = new BlockPos.Mutable();

        for (int level = 0; level < height; level++) {
            BlockPos logPos = pos.up(level);

            if (random.nextInt(100) <= holeChance) {
                holeChance -= 10;
                Direction facing = Direction.fromHorizontal(random.nextInt(4));
                world.setBlockState(logPos, IC2ClassicBlocks.RUBBER_WOOD.getDefaultState()
                        .with(BlockRubWood.STATE, BlockRubWood.RubberWoodState.wet(facing)), NOTIFY_ALL);
            } else {
                world.setBlockState(logPos, plainLog, NOTIFY_ALL);
            }

            // the crown starts higher up on a tall tree, so a short one is not all leaves
            if (height >= 4 && (height >= 7 || level <= 1) && level <= 2) {
                continue;
            }

            for (int x = pos.getX() - 2; x <= pos.getX() + 2; x++) {
                for (int z = pos.getZ() - 2; z <= pos.getZ() + 2; z++) {
                    int chance = Math.max(1, level + 4 - height);
                    int dx = Math.abs(x - pos.getX());
                    int dz = Math.abs(z - pos.getZ());

                    boolean place = dx <= 1 && dz <= 1
                            || dx <= 1 && random.nextInt(chance) == 0
                            || dz <= 1 && random.nextInt(chance) == 0;
                    if (!place) {
                        continue;
                    }

                    cursor.set(x, pos.getY() + level, z);
                    if (world.isAir(cursor)) {
                        world.setBlockState(cursor.toImmutable(), withDistance(leaves, dx + dz), NOTIFY_ALL);
                    }
                }
            }
        }

        // the tip above the last log
        for (int i = 0; i <= height / 4 + random.nextInt(2); i++) {
            cursor.set(pos.getX(), pos.getY() + height + i, pos.getZ());
            if (world.isAir(cursor)) {
                world.setBlockState(cursor.toImmutable(), withDistance(leaves, i + 1), NOTIFY_ALL);
            }
        }

        return true;
    }

    /**
     * The leaf block with its distance to the trunk filled in, the same number vanilla's leaf
     * decay would work out for it. Placing leaves at the default distance of 7 makes every one of
     * them decay as soon as the chunk ticks.
     */
    private static BlockState withDistance(BlockState leaves, int distance) {
        return leaves.with(LeavesBlock.DISTANCE, Math.max(1, Math.min(7, distance)));
    }

    /**
     * How much room there is above that spot, capped at {@link #MAX_HEIGHT}.
     *
     * @return 0 when the ground below cannot hold a tree at all
     */
    public static int getGrowHeight(WorldAccess world, BlockPos pos) {
        BlockPos below = pos.down();
        BlockState base = world.getBlockState(below);
        if (base.isAir() || !base.isSideSolidFullSquare(world, below, Direction.UP)) {
            return 0;
        }

        BlockPos above = pos.up();
        if (!world.isAir(above) && !world.getBlockState(above).isOf(IC2ClassicBlocks.RUBBER_SAPLING)) {
            return 0;
        }

        int height = 1;
        for (BlockPos cursor = above; world.isAir(cursor) && height < MAX_HEIGHT; height++) {
            cursor = cursor.up();
        }
        return height;
    }
}
