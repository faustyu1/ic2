package ic2.core.util;

import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * Which side of a block a click means, for tools that work on any of the six sides: the face is
 * split into a 3x3 grid, the middle means the face itself, the edge strips the side they lie
 * towards, and the corners the face opposite the one clicked.
 */
public final class RotationUtil {
    private RotationUtil() {
    }

    public static Direction rotateByRay(BlockHitResult hit) {
        Vec3d pos = hit.getPos();
        BlockPos block = hit.getBlockPos();
        return rotateByHit(hit.getSide(),
                (float) (pos.x - block.getX()), (float) (pos.y - block.getY()), (float) (pos.z - block.getZ()));
    }

    public static Direction rotateByHit(Direction facingHit, float hitX, float hitY, float hitZ) {
        switch (facingHit) {
            case DOWN:
            case UP:
                return pick(facingHit, hitX, hitZ, Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH);
            case NORTH:
            case SOUTH:
                return pick(facingHit, hitX, hitY, Direction.WEST, Direction.EAST, Direction.DOWN, Direction.UP);
            case WEST:
            case EAST:
                return pick(facingHit, hitZ, hitY, Direction.NORTH, Direction.SOUTH, Direction.DOWN, Direction.UP);
            default:
                return facingHit;
        }
    }

    /**
     * The 1.12.2 grid on one face: {@code a} runs from {@code lowA} to {@code highA}, {@code b}
     * from {@code lowB} to {@code highB}.
     */
    private static Direction pick(Direction face, float a, float b, Direction lowA, Direction highA,
                                  Direction lowB, Direction highB) {
        boolean midA = a > 0.25F && a < 0.75F;
        boolean midB = b > 0.25F && b < 0.75F;
        if (midA && midB) {
            return face;
        }
        if (midB) {
            return a <= 0.25F ? lowA : highA;
        }
        if (midA) {
            return b <= 0.25F ? lowB : highB;
        }
        return face.getOpposite();
    }
}
