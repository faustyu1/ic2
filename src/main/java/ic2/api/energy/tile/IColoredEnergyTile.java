package ic2.api.energy.tile;

import net.minecraft.util.DyeColor;
import net.minecraft.util.math.Direction;

public interface IColoredEnergyTile extends IEnergyTile {
    DyeColor getColor(Direction side);
}
