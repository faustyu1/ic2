package ic2.core.util;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.block.FluidBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Fabric port of ic2.core.util.PumpUtil: finds the source block a pump drains.
 *
 * <p>From the start it climbs up the flow (towards smaller flow decay) for up to 64 steps, then
 * walks along the surface for up to 64 more looking for a source. Failing that, it takes a source
 * in the 5x5 around where it ended up, shortening (or removing) the flowing blocks there so the
 * next search gets closer - the same walk as 1.12.2.
 */
public final class PumpUtil {
    private PumpUtil() {
    }

    private static int moveUp(World world, BlockPos.Mutable pos) {
        pos.set(pos.getX(), pos.getY() + 1, pos.getZ());
        int newDecay = getFlowDecay(world, pos);
        if (newDecay >= 0) {
            return newDecay;
        }
        pos.set(pos.getX() + 1, pos.getY(), pos.getZ());
        newDecay = getFlowDecay(world, pos);
        if (newDecay >= 0) {
            return newDecay;
        }
        pos.set(pos.getX() - 2, pos.getY(), pos.getZ());
        newDecay = getFlowDecay(world, pos);
        if (newDecay >= 0) {
            return newDecay;
        }
        pos.set(pos.getX() + 1, pos.getY(), pos.getZ() + 1);
        newDecay = getFlowDecay(world, pos);
        if (newDecay >= 0) {
            return newDecay;
        }
        pos.set(pos.getX(), pos.getY(), pos.getZ() - 2);
        newDecay = getFlowDecay(world, pos);
        if (newDecay >= 0) {
            return newDecay;
        }
        pos.set(pos.getX(), pos.getY() - 1, pos.getZ() + 1);
        return -1;
    }

    private static int moveSideways(World world, BlockPos.Mutable pos, int decay) {
        pos.set(pos.getX() - 1, pos.getY(), pos.getZ());
        int newDecay = getFlowDecay(world, pos);
        if (newDecay >= 0 && newDecay < decay) {
            return newDecay;
        }
        pos.set(pos.getX() + 1, pos.getY(), pos.getZ() + 1);
        newDecay = getFlowDecay(world, pos);
        if (newDecay >= 0 && newDecay < decay) {
            return newDecay;
        }
        pos.set(pos.getX(), pos.getY(), pos.getZ() - 2);
        newDecay = getFlowDecay(world, pos);
        if (newDecay >= 0 && newDecay < decay) {
            return newDecay;
        }
        pos.set(pos.getX() + 1, pos.getY(), pos.getZ() + 1);
        newDecay = getFlowDecay(world, pos);
        if (newDecay >= 0 && newDecay < decay) {
            return newDecay;
        }
        pos.set(pos.getX() - 1, pos.getY(), pos.getZ());
        return -1;
    }

    public static BlockPos searchFluidSource(World world, BlockPos startPos) {
        BlockPos.Mutable pos = startPos.mutableCopy();
        int decay = getFlowDecay(world, pos);

        for (int i = 0; i < 64; i++) {
            int newDecay = moveUp(world, pos);
            if (newDecay < 0) {
                newDecay = moveSideways(world, pos, decay);
                if (newDecay < 0) {
                    break;
                }
            }
            decay = newDecay;
        }

        Set<BlockPos> visited = new HashSet<>(64);
        for (int i = 0; i < 64; i++) {
            visited.add(pos.toImmutable());
            int[][] steps = {{-1, 0}, {1, 1}, {0, -2}, {1, 1}};
            boolean moved = false;
            for (int[] step : steps) {
                pos.set(pos.getX() + step[0], pos.getY(), pos.getZ() + step[1]);
                if (!visited.contains(pos)) {
                    int newDecay = getFlowDecay(world, pos);
                    if (newDecay >= 0) {
                        if (newDecay == 0) {
                            return pos.toImmutable();
                        }
                        moved = true;
                        break;
                    }
                }
            }
            if (!moved) {
                // back to where this step started
                pos.set(pos.getX() - 1, pos.getY(), pos.getZ());
                break;
            }
        }

        BlockPos.Mutable cPos = new BlockPos.Mutable();
        for (int ix = -2; ix <= 2; ix++) {
            for (int iz = -2; iz <= 2; iz++) {
                cPos.set(pos.getX() + ix, pos.getY(), pos.getZ() + iz);
                BlockState state = world.getBlockState(cPos);
                decay = getFlowDecay(state);
                if (decay >= 0) {
                    if (decay == 0) {
                        return cPos.toImmutable();
                    }
                    if (decay < 7 && state.getBlock() instanceof FluidBlock) {
                        world.setBlockState(cPos, state.with(FluidBlock.LEVEL, decay + 1));
                    } else {
                        world.removeBlock(cPos, false);
                    }
                }
            }
        }
        return null;
    }

    protected static int getFlowDecay(World world, BlockPos pos) {
        return getFlowDecay(world.getBlockState(pos));
    }

    /** 0 for a source, 1-7 for flowing fluid further and further from it (8+ falling), -1 for no fluid. */
    protected static int getFlowDecay(BlockState state) {
        return state.getBlock() instanceof FluidBlock ? state.get(FluidBlock.LEVEL) : -1;
    }
}
