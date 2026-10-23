package ic2.api.energy.tile;

import net.minecraft.util.math.Direction;

public interface IHeatSource {
    @Deprecated
    int maxrequestHeatTick(Direction directionFrom);

    default int getConnectionBandwidth(Direction side) {
        return maxrequestHeatTick(side);
    }

    @Deprecated
    int requestHeat(Direction directionFrom, int amount);

    default int drawHeat(Direction side, int request, boolean simulate) {
        return !simulate ? requestHeat(side, request) : maxrequestHeatTick(side);
    }
}
