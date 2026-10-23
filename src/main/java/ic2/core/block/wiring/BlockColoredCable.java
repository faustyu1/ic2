package ic2.core.block.wiring;

import ic2.core.ref.TeBlock;
import ic2.core.util.Ic2Color;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;

/**
 * A cable that takes paint: insulated cables and the glass fibre cable.
 *
 * <p>1.12.2 kept the colour in the tile entity and drew it through the extended block state;
 * here it is a block state property, so the multipart model picks the painted texture and the
 * arms see their neighbours' paint when they decide whether to connect. Black is the colour of
 * an unpainted cable and connects to everything.
 */
public class BlockColoredCable extends BlockCable {
    public static final EnumProperty<Ic2Color> COLOR = EnumProperty.of("color", Ic2Color.class);

    public BlockColoredCable(Settings settings, TeBlock teBlock, CableType cableType, int insulation) {
        super(settings, teBlock, cableType, insulation);
        setDefaultState(getDefaultState().with(COLOR, Ic2Color.black));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(COLOR);
    }
}
