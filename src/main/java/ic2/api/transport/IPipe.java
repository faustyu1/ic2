package ic2.api.transport;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.Direction;

public interface IPipe {
    BlockEntity getTile();
    boolean isConnected(Direction direction);
    void flipConnection(Direction direction);
}
