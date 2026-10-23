package ic2.core.block;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Blocks;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.MapColor;
import net.minecraft.block.WoodType;

public class BlockIC2FenceGate extends FenceGateBlock {

    public BlockIC2FenceGate() {
        super(WoodType.OAK, AbstractBlock.Settings.copy(Blocks.OAK_FENCE_GATE).mapColor(MapColor.GRAY));
    }

    public BlockIC2FenceGate(Settings settings) {
        super(WoodType.OAK, settings);
    }

    public BlockIC2FenceGate(WoodType woodType, Settings settings) {
        super(woodType, settings);
    }
}
