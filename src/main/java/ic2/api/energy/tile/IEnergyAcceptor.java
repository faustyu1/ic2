package ic2.api.energy.tile;

import net.minecraft.util.math.Direction;

/**
 * A tile that can accept energy from the EU grid.
 */
public interface IEnergyAcceptor extends IEnergyTile {
    boolean acceptsEnergyFrom(IEnergyEmitter emitter, Direction side);
}
