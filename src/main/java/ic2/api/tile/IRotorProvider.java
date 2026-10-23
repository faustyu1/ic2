package ic2.api.tile;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

public interface IRotorProvider {
    int getRotorDiameter();
    Direction getFacing();
    float getAngle();
    Identifier getRotorRenderTexture();
}
