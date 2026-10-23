package ic2.core.block.reactor.tileentity;

import ic2.core.ref.TeBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/** Refills LZH condensators, which drink lapis lazuli blocks. */
public class TileEntityRCI_LZH extends TileEntityAbstractRCI {
    public TileEntityRCI_LZH(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, TeBlock.rci_lzh,
                new ItemStack(Registries.ITEM.get(Identifier.of("ic2", "lzh_condensator"))),
                new ItemStack(Blocks.LAPIS_BLOCK));
    }

    public TileEntityRCI_LZH(BlockPos pos, BlockState state) { this(null, pos, state); }
    public TileEntityRCI_LZH() { this(null, BlockPos.ORIGIN, null); }
}
