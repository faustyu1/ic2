package ic2.core.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.MapColor;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;

/**
 * Fabric stub for IC2 metal sheet.
 * Original: extends BlockMultiID&lt;SheetType&gt; with Material.IRON, low AABB (0.125 height), no opaque/full cube.
 * Fabric 1.21.1: extends Block — оставлено сознательно, прочность задаётся через Settings.strength() в createSettings().
 */
public class BlockSheet extends Block { // extends Block — сохранено для Fabric 1.21.1 (не BlockBase)

    public BlockSheet(Settings settings) {
        super(settings);
    }

    public static Settings createSettings() {
        MapColor color = MapColor.GRAY;
        BlockSoundGroup sound = BlockSoundGroup.METAL;
        return Settings.create().mapColor(color).sounds(sound).strength(0.5F, 6.0F).requiresTool().nonOpaque();
    }

    @Override
    protected boolean isTransparent(BlockState state, BlockView world, BlockPos pos) {
        return true;
    }

    // convenience no-arg per task spec
    public boolean isTransparent() {
        return true;
    }

    /**
     * Fabric port of BlockSheet.SheetType. Metadata is gone since flattening, so the members are
     * registered as separate items; the hardness values are kept for when the block variants follow.
     */
    public enum SheetType implements ic2.core.block.state.IIdProvider {
        resin(1.6F, 0.5F),
        rubber(0.8F, 2.0F),
        wool(0.8F, 0.8F);

        public final float hardness;
        public final float explosionResistance;

        SheetType(float hardness, float explosionResistance) {
            this.hardness = hardness;
            this.explosionResistance = explosionResistance;
        }

        @Override
        public String getName() {
            return name();
        }

        @Override
        public int getId() {
            return ordinal();
        }
    }
}
