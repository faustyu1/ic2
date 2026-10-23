package ic2.core.block;

import ic2.core.ref.TeBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.TntBlock;

/**
 * Marks a block that goes off rather than breaks - what {@code MaterialIC2TNT} told the mining
 * laser in 1.12.2. The dynamite stick has it; the industrial TNT and the nuke are tile entity
 * blocks and are recognised by their kind.
 */
public interface ITnt {
    static boolean isExplosive(BlockState state) {
        return state.getBlock() instanceof ITnt || state.getBlock() instanceof TntBlock
                || state.getBlock() instanceof BlockTileEntity block && (block.teBlock == TeBlock.itnt || block.teBlock == TeBlock.nuke);
    }
}
