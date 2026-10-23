package ic2.core.block.machine.tileentity;

import ic2.api.energy.tile.IEnergyAcceptor;
import ic2.api.energy.tile.IMultiEnergySource;
import ic2.core.block.TileEntityBlock;
import ic2.core.init.IC2Blocks;
import ic2.core.ref.TeBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Fabric port of TileEntityCreativeGenerator: an endless source of energy for creative mode,
 * sending ten packets of tier 1 (32 EU) a tick to every side.
 */
public class TileEntityCreativeGenerator extends TileEntityBlock implements IMultiEnergySource {
    public TileEntityCreativeGenerator(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.CREATIVE_GENERATOR_BE : type, pos, state, TeBlock.creative_generator);
    }

    public TileEntityCreativeGenerator(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    @Override
    public double getOfferedEnergy() {
        return Double.POSITIVE_INFINITY;
    }

    @Override
    public void drawEnergy(double amount) {
    }

    @Override
    public int getSourceTier() {
        return 1;
    }

    @Override
    public boolean emitsEnergyTo(IEnergyAcceptor receiver, Direction side) {
        return true;
    }

    @Override
    public boolean sendMultipleEnergyPackets() {
        return true;
    }

    @Override
    public int getMultipleEnergyPacketAmount() {
        return 10;
    }
}
