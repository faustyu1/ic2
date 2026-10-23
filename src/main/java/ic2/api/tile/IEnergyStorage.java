package ic2.api.tile;

import net.minecraft.util.math.Direction;

public interface IEnergyStorage {
    int getStored();
    void setStored(int amount);
    int addEnergy(int amount);
    int getCapacity();
    int getOutput();
    double getOutputEnergyUnitsPerTick();
    boolean isTeleporterCompatible(Direction direction);
}
