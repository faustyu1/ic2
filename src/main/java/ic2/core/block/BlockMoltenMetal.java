package ic2.core.block;

import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.sound.BlockSoundGroup;

public class BlockMoltenMetal extends Block {
    private final String metal;
    public BlockMoltenMetal(String metal){
        super(Settings.create().mapColor(MapColor.GRAY).sounds(BlockSoundGroup.METAL).strength(3.0f));
        this.metal=metal;
    }
    public String getMetal(){ return metal; }
}
