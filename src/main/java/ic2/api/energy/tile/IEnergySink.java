package ic2.api.energy.tile;

import net.minecraft.util.math.Direction;

/**
 * A tile that consumes EU (e.g. a machine).
 */
public interface IEnergySink extends IEnergyAcceptor {
    double getDemandedEnergy();

    int getSinkTier();

    double injectEnergy(Direction directionFrom, double amount, double voltage);
}
