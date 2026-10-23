package ic2.core.block.generator.tileentity;

import ic2.api.energy.tile.IEnergyAcceptor;
import ic2.api.energy.tile.IMultiEnergySource;
import ic2.core.block.TileEntityBlock;
import ic2.core.init.IC2Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Fabric port of TileEntityCreativeGenerator - infinite source.
 */
public class TileEntityCreativeGenerator extends TileEntityBlock implements IMultiEnergySource {

    public TileEntityCreativeGenerator(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public TileEntityCreativeGenerator(BlockPos pos, BlockState state) { this(IC2Blocks.CREATIVE_GENERATOR_BE, pos, state); }
    public TileEntityCreativeGenerator() { this(IC2Blocks.CREATIVE_GENERATOR_BE, BlockPos.ORIGIN, null); }

    @Override public double getOfferedEnergy() { return Double.POSITIVE_INFINITY; }
    @Override public void drawEnergy(double amount) {}
    @Override public int getSourceTier() { return 1; }
    @Override public boolean emitsEnergyTo(IEnergyAcceptor receiver, Direction side) { return true; }
    @Override public boolean sendMultipleEnergyPackets() { return true; }
    @Override public int getMultipleEnergyPacketAmount() { return 10; }

    public void onLoaded() {
        super.onLoad();
        if (world != null && !world.isClient) ic2.api.energy.EnergyNet.instance.addTile(this);
    }

    public void onUnloaded() {
        if (world != null && !world.isClient) ic2.api.energy.EnergyNet.instance.removeTile(this);
        super.onUnloaded();
    }
}
