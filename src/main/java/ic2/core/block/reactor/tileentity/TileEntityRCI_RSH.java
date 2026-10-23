package ic2.core.block.reactor.tileentity;

import ic2.core.ref.TeBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/** Refills RSH condensators, which drink redstone blocks. */
public class TileEntityRCI_RSH extends TileEntityAbstractRCI {
    public TileEntityRCI_RSH(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, TeBlock.rci_rsh,
                new ItemStack(Registries.ITEM.get(Identifier.of("ic2", "rsh_condensator"))),
                new ItemStack(Blocks.REDSTONE_BLOCK));
    }

    public TileEntityRCI_RSH(BlockPos pos, BlockState state) { this(null, pos, state); }
    public TileEntityRCI_RSH() { this(null, BlockPos.ORIGIN, null); }
}
