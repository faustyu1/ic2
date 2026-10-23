package ic2.core.item.tfbp;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** What one kind of terraforming blueprint does to the column the terraformer picked. */
abstract class TerraformerBase {
    /**
     * Works on the column at pos; pos has the terraformer's height.
     *
     * @return whether something was changed, which is what the terraformer pays the full price for
     */
    abstract boolean terraform(World world, BlockPos pos);
}
