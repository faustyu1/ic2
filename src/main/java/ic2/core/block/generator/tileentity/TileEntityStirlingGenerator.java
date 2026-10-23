package ic2.core.block.generator.tileentity;

import ic2.api.energy.tile.IHeatSource;
import ic2.core.init.IC2Blocks;
import ic2.core.init.MainConfig;
import ic2.core.ref.TeBlock;
import ic2.core.util.ConfigUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

/**
 * Fabric port of TileEntityStirlingGenerator - turns the heat of the block in front of it into
 * EU, one EU for two HU (times {@code balance/energy/generator/Stirling}).
 */
public class TileEntityStirlingGenerator extends TileEntityConversionGenerator {
    private final double productionPerHeat = 0.5 * ConfigUtil.getDouble(MainConfig.get(), "balance/energy/generator/Stirling");
    protected IHeatSource source;

    public TileEntityStirlingGenerator(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.STIRLING_BE : type, pos, state, TeBlock.stirling_generator);
    }

    public TileEntityStirlingGenerator(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    @Override
    protected void updateSource() {
        if (world == null) {
            return;
        }
        if (source == null || ((BlockEntity) source).isRemoved() || !((BlockEntity) source).getPos().equals(pos.offset(getFacing()))) {
            BlockEntity te = world.getBlockEntity(pos.offset(getFacing()));
            source = te instanceof IHeatSource heat ? heat : null;
        }
    }

    @Override
    protected int getEnergyAvailable() {
        updateSource();
        if (source == null) {
            return 0;
        }
        return source.drawHeat(getFacing().getOpposite(), source.getConnectionBandwidth(getFacing().getOpposite()), true);
    }

    @Override
    protected void drawEnergyAvailable(int amount) {
        if (source != null) {
            source.drawHeat(getFacing().getOpposite(), amount, false);
        }
    }

    @Override
    protected double getMultiplier() {
        return productionPerHeat;
    }
}
