package ic2.api.energy.tile;

import net.minecraft.util.math.Direction;

/**
 * A tile that can emit energy into the EU grid.
 */
public interface IEnergyEmitter extends IEnergyTile {
    boolean emitsEnergyTo(IEnergyAcceptor receiver, Direction side);
}
