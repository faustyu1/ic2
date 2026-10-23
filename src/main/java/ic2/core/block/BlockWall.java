package ic2.core.block;

import ic2.core.util.Ic2Color;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Fabric port of BlockWall - hardened construction foam, in sixteen colours.
 *
 * <p>1.12.2 had one block with the colour as metadata; since the flattening each colour is its
 * own block, {@code ic2:wall_<colour>}, the way vanilla does wool. Foam hardens into the light
 * grey one, and the painter swaps one colour for another.
 */
public class BlockWall extends Block {
    public static final Ic2Color DEFAULT_COLOR = Ic2Color.light_gray;

    private static final Map<Ic2Color, BlockWall> BY_COLOR = new EnumMap<>(Ic2Color.class);

    public final Ic2Color color;

    public BlockWall(Settings settings, Ic2Color color) {
        super(settings);
        this.color = color;
        BY_COLOR.put(color, this);
    }

    /** The wall block of that colour. */
    public static BlockWall get(Ic2Color color) {
        return BY_COLOR.get(color);
    }

    /** The painter: the wall turns into the wall of the new colour. */
    public boolean recolor(World world, BlockPos pos, Ic2Color newColor) {
        if (newColor == color) {
            return false;
        }
        BlockState state = get(newColor).getDefaultState();
        return world.setBlockState(pos, state);
    }
}
