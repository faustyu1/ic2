package ic2.api.energy;

import ic2.api.energy.tile.IEnergyTile;

public interface IEnergyNetEventReceiver {
    void onAdd(IEnergyTile tile);

    void onRemove(IEnergyTile tile);
}
