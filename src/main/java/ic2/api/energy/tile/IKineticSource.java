package ic2.api.energy.tile;

import net.minecraft.util.math.Direction;

public interface IKineticSource {
    @Deprecated
    int maxrequestkineticenergyTick(Direction directionFrom);

    default int getConnectionBandwidth(Direction side) {
        return maxrequestkineticenergyTick(side);
    }

    @Deprecated
    int requestkineticenergy(Direction directionFrom, int amount);

    default int drawKineticEnergy(Direction side, int request, boolean simulate) {
        return !simulate ? requestkineticenergy(side, request) : maxrequestkineticenergyTick(side);
    }
}
