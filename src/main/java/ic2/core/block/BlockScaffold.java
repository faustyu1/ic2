package ic2.core.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.MapColor;
import net.minecraft.block.ShapeContext;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

/**
 * Fabric stub for IC2 scaffold.
 * Original: extends BlockMultiID&lt;ScaffoldType&gt; with Material.WOOD, partial AABB, custom support logic.
 * Fabric 1.21.1: extends Block — оставлено сознательно, прочность задаётся через Settings.strength() в createSettings().
 */
public class BlockScaffold extends Block { // extends Block — сохранено для Fabric 1.21.1 (не BlockBase)

    public BlockScaffold(Settings settings) {
        super(settings);
    }

    public static Settings createSettings() {
        MapColor color = MapColor.BROWN;
        BlockSoundGroup sound = BlockSoundGroup.WOOD;
        // Scaffold is fragile, no tool required, low hardness
        return Settings.create().mapColor(color).sounds(sound).strength(0.5F).nonOpaque();
    }

    // translucency stub — nonOpaque scaffold keeps vanilla collision; override for future translucency handling
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return super.getCollisionShape(state, world, pos, context);
    }

    /**
     * Fabric port of BlockScaffold.ScaffoldType, kept for the flattened item registration; the
     * strength value drives the original collapse logic once the block variants are ported.
     */
    public enum ScaffoldType implements ic2.core.block.state.IIdProvider {
        wood(2, 0.5F, 0.12F),
        reinforced_wood(5, 0.6F, 0.24F),
        iron(5, 0.8F, 6.0F),
        reinforced_iron(12, 1.0F, 8.0F);

        public final int strength;
        public final float hardness;
        public final float explosionResistance;

        ScaffoldType(int strength, float hardness, float explosionResistance) {
            this.strength = strength;
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
