package ic2.api.energy;

import ic2.api.energy.tile.IEnergyTile;
import ic2.api.info.ILocatable;
import java.io.PrintStream;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface IEnergyNet {
    IEnergyTile getTile(World world, BlockPos pos);

    IEnergyTile getSubTile(World world, BlockPos pos);

    <T extends BlockEntity & IEnergyTile> void addTile(T tile);

    <T extends ILocatable & IEnergyTile> void addTile(T tile);

    void removeTile(IEnergyTile tile);

    World getWorld(IEnergyTile tile);

    BlockPos getPos(IEnergyTile tile);

    NodeStats getNodeStats(IEnergyTile tile);

    boolean dumpDebugInfo(World world, BlockPos pos, PrintStream out, PrintStream err);

    double getPowerFromTier(int tier);

    int getTierFromPower(double power);

    void registerEventReceiver(IEnergyNetEventReceiver receiver);

    void unregisterEventReceiver(IEnergyNetEventReceiver receiver);
}
