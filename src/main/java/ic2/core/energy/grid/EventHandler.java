package ic2.core.energy.grid;

import ic2.api.energy.EnergyNet;
import ic2.api.energy.tile.IEnergyTile;
import ic2.api.info.ILocatable;
import ic2.core.IC2;
import ic2.core.util.LogCategory;
import ic2.core.util.Util;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.world.World;

/**
 * Fabric replacement for Forge EventHandler.
 * Original used MinecraftForge.EVENT_BUS + EnergyTileLoadEvent/EnergyTileUnloadEvent/WorldTickEvent.
 * On Fabric we call EnergyNet directly from BlockEntity hooks and ServerTickEvents.
 */
public class EventHandler {
    private static boolean initialized;

    public static void init() {
        if (initialized) {
            throw new IllegalStateException("already initialized");
        }
        initialized = true;
        // On Fabric, registration is done via ServerTickEvents / BlockEntity callbacks elsewhere.
        // No Forge bus registration needed.
    }

    private EventHandler() {}

    @SuppressWarnings("unchecked")
    private static <T extends BlockEntity & IEnergyTile> void addTileBE(T tile) {
        EnergyNet.instance.addTile(tile);
    }

    @SuppressWarnings("unchecked")
    private static <T extends ILocatable & IEnergyTile> void addTileLoc(T tile) {
        EnergyNet.instance.addTile(tile);
    }

    public static void onEnergyTileLoad(IEnergyTile tile, World world) {
        if (world.isClient) {
            IC2.log.warn(LogCategory.EnergyNet, "EnergyTileLoad: posted for %s client-side, aborting", Util.toString(tile, world, EnergyNet.instance.getPos(tile)));
        } else {
            if (tile instanceof BlockEntity) {
                addTileBE((BlockEntity & IEnergyTile) tile);
            } else if (tile instanceof ILocatable) {
                addTileLoc((ILocatable & IEnergyTile) tile);
            } else {
                throw new IllegalArgumentException("invalid tile type: " + tile);
            }
        }
    }

    public static void onEnergyTileUnload(IEnergyTile tile, World world) {
        if (world.isClient) {
            IC2.log.warn(LogCategory.EnergyNet, "EnergyTileUnload: posted for %s client-side, aborting", Util.toString(tile, world, EnergyNet.instance.getPos(tile)));
        } else {
            EnergyNet.instance.removeTile(tile);
        }
    }

    public static void onWorldTick(World world, boolean isStart) {
        EnergyNetLocal enet = EnergyNetGlobal.getLocal(world);
        if (enet == null) return;
        if (isStart) {
            enet.onTickStart();
        } else {
            enet.onTickEnd();
        }
    }
}
