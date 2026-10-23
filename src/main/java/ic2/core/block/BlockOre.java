package ic2.core.block;

import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.sound.BlockSoundGroup;

/**
 * Fabric stub for IC2 ores.
 * Original: extends BlockMultiID&lt;ResourceBlock&gt; with Material.ROCK, hardness per ResourceBlock.
 * This version is plain Block with OreType field; instances registered separately in IC2ClassicBlocks.
 */
public class BlockOre extends Block {

    public enum OreType {
        COPPER(3.0F, 5.0F),
        TIN(3.0F, 5.0F),
        URANIUM(4.0F, 6.0F),
        LEAD(2.0F, 4.0F);

        public final float hardness;
        public final float resistance;

        OreType(float hardness, float resistance) {
            this.hardness = hardness;
            this.resistance = resistance;
        }
    }

    public final OreType oreType;

    public BlockOre(Settings settings, OreType oreType) {
        super(settings);
        this.oreType = oreType;
    }

    public OreType getOreType() {
        return oreType;
    }

    /**
     * Helper to create Fabric settings for given ore variant.
     * Uses mapColor STONE_GRAY, stone sounds, requiresTool.
     */
    public static Settings createSettings(OreType type) {
        // Keep reference to MapColor/BlockSoundGroup for import validity
        MapColor color = MapColor.STONE_GRAY;
        BlockSoundGroup sound = BlockSoundGroup.STONE;
        // Caller will use FabricBlockSettings; this is fallback if AbstractBlock.Settings is used directly
        return Settings.create().mapColor(color).sounds(sound).strength(type.hardness, type.resistance).requiresTool();
    }
}
