package ic2.core.block.wiring;

import ic2.api.energy.EnergyNet;
import ic2.core.init.IC2Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

/**
 * Fabric port of TileEntityCableDetector - the EU detector cable.
 *
 * <p>Every 32 ticks it looks at the energy that went through it: any at all and it lights up and
 * gives a full redstone signal. A comparator reads how much, 15 at its full 8192 EU/t.
 */
public class TileEntityCableDetector extends TileEntityCable {
    private static final int TICK_RATE = 32;

    private int ticker = 0;
    private int redstoneLevel = 0;
    private int comparatorLevel = 0;

    public TileEntityCableDetector(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.CABLE_BE : type, pos, state, CableType.detector, 0);
    }

    public TileEntityCableDetector(BlockPos pos, BlockState state) {
        this(IC2Blocks.CABLE_BE, pos, state);
    }

    @Override
    public void tick() {
        if (world == null || world.isClient || ++ticker % TICK_RATE != 0) {
            return;
        }

        double energy = EnergyNet.instance.getNodeStats(this).getEnergyIn();
        setActive(energy > 0.0);
        setRedstoneLevel(energy > 0.0 ? 15 : 0);

        // Util.map(energy / capacity, 1, 15): the share of the cable's capacity, cut at full
        double ratio = energy / (getConductorBreakdownEnergy() - 1.0);
        if (ratio < 0.0 || Double.isNaN(ratio)) {
            ratio = 0.0;
        }
        setComparatorLevel((int) (Math.min(ratio, 1.0) * 15.0));
    }

    private void setRedstoneLevel(int level) {
        if (level != redstoneLevel) {
            redstoneLevel = level;
            world.updateNeighbors(pos, getCachedState().getBlock());
        }
    }

    private void setComparatorLevel(int level) {
        if (level != comparatorLevel) {
            comparatorLevel = level;
            world.updateComparators(pos, getCachedState().getBlock());
        }
    }

    @Override
    public int getRedstoneOutput() {
        return redstoneLevel;
    }

    @Override
    public int getComparatorOutput() {
        return comparatorLevel;
    }

    public static Class<? extends TileEntityCable> delegate() {
        return TileEntityCableDetector.class;
    }
}
