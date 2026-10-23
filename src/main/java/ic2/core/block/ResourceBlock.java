package ic2.core.block;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.sound.BlockSoundGroup;

/**
 * Storage/metal blocks parity with original {@code ic2.core.block.type.ResourceBlock}.
 * Original (decompiled/src/ic2/core/block/type/ResourceBlock.java:7): enum ResourceBlock
 * with variants bronze_block(5.0F,10.0F,true), copper_block(4.0F,10.0F,true),
 * lead_block(4.0F,10.0F,true), steel_block(8.0F,10.0F,true), tin_block(4.0F,10.0F,true),
 * uranium_block(6.0F,10.0F,true), silver_block(4.0F,10.0F,true) — all metal sound.
 *
 * <p>Fabric port: plain {@link Block} subclass with inner {@link Metal} enum carrying
 * hardness/resistance. Settings mirror {@code BlockMoltenMetal} pattern but per task
 * use {@code Settings.copy(Blocks.IRON_BLOCK)} as base.</p>
 */
public class ResourceBlock extends Block {

    public enum Metal {
        COPPER(4.0F, 10.0F),
        TIN(4.0F, 10.0F),
        BRONZE(5.0F, 10.0F),
        LEAD(4.0F, 10.0F),
        STEEL(8.0F, 10.0F),
        SILVER(4.0F, 10.0F),
        URANIUM(6.0F, 10.0F);

        public final float hardness;
        public final float resistance;

        Metal(float hardness, float resistance) {
            this.hardness = hardness;
            this.resistance = resistance;
        }
    }

    private final Metal metal;

    public ResourceBlock(Metal metal) {
        super(Settings.copy(Blocks.IRON_BLOCK)
                .strength(metal.hardness, metal.resistance)
                .sounds(BlockSoundGroup.METAL)
                .requiresTool());
        this.metal = metal;
    }

    /**
     * String-based ctor like {@link BlockMoltenMetal} for dynamic creation.
     * Accepts lower/upper case metal name, e.g. "copper", "STEEL".
     */
    public ResourceBlock(String metalName) {
        this(Metal.valueOf(metalName.toUpperCase()));
    }

    public Metal getMetal() {
        return metal;
    }

    public String getMetalName() {
        return metal.name().toLowerCase();
    }
}
