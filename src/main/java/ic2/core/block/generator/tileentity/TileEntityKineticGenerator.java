package ic2.core.block.generator.tileentity;

import ic2.api.energy.tile.IKineticSource;
import ic2.core.init.IC2Blocks;
import ic2.core.init.MainConfig;
import ic2.core.ref.TeBlock;
import ic2.core.util.ConfigUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

/**
 * Fabric port of TileEntityKineticGenerator - turns the kinetic energy of the block in front of
 * it into EU, one EU for four kU (times {@code balance/energy/generator/Kinetic}).
 */
public class TileEntityKineticGenerator extends TileEntityConversionGenerator {
    private final double euPerKu = 0.25 * ConfigUtil.getDouble(MainConfig.get(), "balance/energy/generator/Kinetic");
    protected IKineticSource source;

    public TileEntityKineticGenerator(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.KINETIC_BE : type, pos, state, TeBlock.kinetic_generator);
    }

    public TileEntityKineticGenerator(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    @Override
    protected void updateSource() {
        if (world == null) {
            return;
        }
        if (source == null || ((BlockEntity) source).isRemoved() || !((BlockEntity) source).getPos().equals(pos.offset(getFacing()))) {
            BlockEntity te = world.getBlockEntity(pos.offset(getFacing()));
            source = te instanceof IKineticSource kinetic ? kinetic : null;
        }
    }

    @Override
    protected int getEnergyAvailable() {
        updateSource();
        if (source == null) {
            return 0;
        }
        return source.drawKineticEnergy(getFacing().getOpposite(), source.getConnectionBandwidth(getFacing().getOpposite()), true);
    }

    @Override
    protected void drawEnergyAvailable(int amount) {
        if (source != null) {
            source.drawKineticEnergy(getFacing().getOpposite(), amount, false);
        }
    }

    @Override
    protected double getMultiplier() {
        return euPerKu;
    }
}
