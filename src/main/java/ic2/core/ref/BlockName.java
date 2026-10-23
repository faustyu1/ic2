package ic2.core.ref;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;

public enum BlockName {
    te;

    public Block getInstance() { return Blocks.IRON_BLOCK; }
    public Block getBlock() { return getInstance(); }
}
