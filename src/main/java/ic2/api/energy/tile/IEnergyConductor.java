package ic2.api.energy.tile;

/**
 * A tile that conducts EU between other tiles (e.g. a cable).
 */
public interface IEnergyConductor extends IEnergyAcceptor, IEnergyEmitter {
    double getConductionLoss();

    double getInsulationEnergyAbsorption();

    double getInsulationBreakdownEnergy();

    double getConductorBreakdownEnergy();

    void removeInsulation();

    void removeConductor();
}
