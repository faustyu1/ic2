package ic2.api.energy;

/**
 * Central accessor for the energy net implementation.
 *
 * The concrete {@link IEnergyNet} is registered by the mod at startup.
 */
public final class EnergyNet {
    public static IEnergyNet instance;
}
