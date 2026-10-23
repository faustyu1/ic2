package ic2.api.energy.tile;

/**
 * A tile that produces EU (e.g. a generator).
 */
public interface IEnergySource extends IEnergyEmitter {
    double getOfferedEnergy();

    void drawEnergy(double amount);

    int getSourceTier();
}
