package ic2.core.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;

/**
 * Fabric port of BlockFoam - construction foam, soft until it sets.
 *
 * <p>Soft foam can be walked through. Left alone it hardens into a light grey foam wall: on a
 * random tick with the odds that make it take {@code 300 * (16 - light)} seconds on average, so
 * it sets faster in the light. Sand rubbed into it sets it at once. It drops nothing.
 *
 * <p>Only the normal foam is ported; the reinforced one came from spraying iron scaffolds, and
 * the sprayer is not ported.
 */
public class BlockFoam extends Block {
    /** Seconds the foam takes to set on average in full darkness divided by 16. */
    public static final int HARDEN_TIME = 300;

    public BlockFoam(Settings settings) {
        super(settings);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.empty();
    }

    @Override
    protected boolean hasRandomTicks(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        int tickSpeed = world.getGameRules().getInt(GameRules.RANDOM_TICK_SPEED);
        if (tickSpeed <= 0) {
            return;
        }

        // a random tick hits a block 'tickSpeed' times in 4096 a tick, which this undoes
        float chance = getHardenChance(world, pos, state) * 4096.0F / tickSpeed;
        if (random.nextFloat() < chance) {
            harden(world, pos);
        }
    }

    /** The chance per tick that the foam sets, from the light it gets. */
    public static float getHardenChance(World world, BlockPos pos, BlockState state) {
        int light = world.getLightLevel(pos);
        if (state.getOpacity(world, pos) == 0) {
            for (Direction side : Direction.values()) {
                light = Math.max(light, world.getLightLevel(pos.offset(side)));
            }
        }
        return hardenChance(light);
    }

    /** {@code 1 / (300 * (16 - light) * 20)}: the foam sets in 300 * (16 - light) seconds on average. */
    public static float hardenChance(int light) {
        int avgTime = HARDEN_TIME * (16 - light);
        return 1.0F / (avgTime * 20);
    }

    public static void harden(World world, BlockPos pos) {
        world.setBlockState(pos, BlockWall.get(BlockWall.DEFAULT_COLOR).getDefaultState());
    }

    /** Sand sets the foam on the spot. */
    @Override
    protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos,
                                             PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!stack.isOf(Items.SAND)) {
            return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!world.isClient) {
            if (!player.getAbilities().creativeMode) {
                stack.decrement(1);
            }
            harden(world, pos);
        }
        return ItemActionResult.success(world.isClient);
    }
}
