package ic2.core.block;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockSetType;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.MapColor;

/**
 * Fabric port of IC2 reinforced door.
 * Original decompiled: extends BlockDoor (net.minecraft.block.BlockDoor) with Material.IRON.
 * Fabric 1.21.1: extends DoorBlock(BlockSetType, Settings).
 * Settings copy Blocks.IRON_DOOR, MapColor.STONE_GRAY, requiresTool.
 */
public class BlockIC2Door extends DoorBlock {

    public BlockIC2Door(BlockSetType blockSetType, Settings settings) {
        super(blockSetType, settings);
    }

    public BlockIC2Door(Settings settings) {
        super(BlockSetType.IRON, settings);
    }

    public BlockIC2Door() {
        super(BlockSetType.IRON, AbstractBlock.Settings.copy(Blocks.IRON_DOOR)
                .mapColor(MapColor.STONE_GRAY)
                .requiresTool());
    }
}
