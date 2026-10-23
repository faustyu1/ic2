package ic2.core.block;

import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.sound.BlockSoundGroup;

public class BlockTexGlass extends Block {
    public BlockTexGlass(){
        super(Settings.create().mapColor(MapColor.CLEAR).sounds(BlockSoundGroup.GLASS).strength(5.0f, 180.0f).nonOpaque().allowsSpawning((a,b,c,d)->false).suffocates((a,b,c)->false).blockVision((a,b,c)->false));
    }
}
