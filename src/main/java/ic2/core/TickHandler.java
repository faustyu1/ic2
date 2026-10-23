package ic2.core;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Fabric port of ic2.core.TickHandler.
 * Original used Forge TickEvent (WorldTickEvent, ServerTickEvent, ClientTickEvent).
 * Fabric equivalent registers via ServerTickEvents.END_SERVER_TICK and END_WORLD_TICK.
 * Single / continuous queues are stored in WorldData and drained in processUpdates.
 */
public class TickHandler {

    private static final boolean debugupdate = System.getProperty("ic2.debugupdate") != null;
    private static final Map<IWorldTickCallback, Throwable> debugTraces = debugupdate ? new WeakHashMap<>() : null;
    private static Throwable lastDebugTrace;

    private static TickHandler INSTANCE;

    public TickHandler() {
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
        ServerTickEvents.START_WORLD_TICK.register(this::onWorldTickStart);
        ServerTickEvents.END_WORLD_TICK.register(this::onWorldTickEnd);
        INSTANCE = this;
    }

    /** Explicit init hook for IC2Mod to call — idempotent. */
    private static boolean initialized = false;
    public static synchronized void init() {
        if (!initialized) {
            initialized = true;
            new TickHandler();
        }
    }

    public static synchronized TickHandler getInstance() {
        if (INSTANCE == null) init();
        return INSTANCE;
    }

    // ---- Fabric event callbacks ----

    private void onServerTick(MinecraftServer server) {
        // Original ServerTickEvent START incremented ItemNanoSaber.ticker; keep a simple global tick
        // We process per-world updates here so even if END_WORLD_TICK is not fired (e.g. no worlds) we still tick.
        // Iterate snapshot of worlds to drain single/continuous queues.
        for (ServerWorld world : server.getWorlds()) {
            if (world == null) continue;
            WorldData wd = WorldData.get(world, false);
            if (wd != null) {
                // mimic original WorldTickEvent START processing
                processUpdates(world, wd);
                // wind is normally updated per world tick
                if (wd.windSim != null) {
                    wd.windSim.updateWind();
                }
                // energy net tick end/start is handled by EventHandler; do not duplicate here
            }
        }
    }

    /**
     * Start of a world tick: the energy net collects what its grids ask for.
     *
     * <p>1.12.2 drove this from Forge's WorldTickEvent; without it the net never runs and no
     * energy moves at all, which is why it hangs off the Fabric world tick here.
     */
    private void onWorldTickStart(ServerWorld world) {
        ic2.core.energy.grid.EventHandler.onWorldTick(world, true);
    }

    private void onWorldTickEnd(ServerWorld world) {
        ic2.core.energy.grid.EventHandler.onWorldTick(world, false);

        // Called after each ServerWorld finishes ticking. Ensure any remaining callbacks for this world are processed.
        // This keeps parity with original WorldTickEvent where each world had its own pipe.
        WorldData wd = WorldData.get(world, false);
        if (wd != null) {
            // Only call processUpdates if onServerTick hasn't already drained (avoid double call).
            // We guard by checking if queues have pending entries; safe to call again — it will just poll empty.
            // Wind already updated in onServerTick, so we skip it here to avoid double increment.
            if (!wd.singleUpdates.isEmpty() || !wd.continuousUpdates.isEmpty() || !wd.continuousUpdatesToAdd.isEmpty()) {
                processUpdates(world, wd);
            }
        }
    }

    // ---- Public API used by IC2 code (TileEntityBlock etc.) ----

    public void requestSingleWorldTick(World world, IWorldTickCallback callback) {
        if (world == null || callback == null) throw new IllegalArgumentException("world/callback is null");
        WorldData.get(world).singleUpdates.add(callback);
        if (debugupdate) {
            debugTraces.put(callback, new Throwable());
        }
    }

    public void requestContinuousWorldTick(World world, IWorldTickCallback update) {
        if (world == null || update == null) throw new IllegalArgumentException("world/update is null");
        WorldData worldData = WorldData.get(world);
        if (!worldData.continuousUpdatesInUse) {
            worldData.continuousUpdates.add(update);
        } else {
            worldData.continuousUpdatesToRemove.remove(update);
            worldData.continuousUpdatesToAdd.add(update);
        }
        if (debugupdate) {
            debugTraces.put(update, new Throwable());
        }
    }

    public void removeContinuousWorldTick(World world, IWorldTickCallback update) {
        if (world == null || update == null) return;
        WorldData worldData = WorldData.get(world, false);
        if (worldData == null) return;
        if (!worldData.continuousUpdatesInUse) {
            worldData.continuousUpdates.remove(update);
        } else {
            worldData.continuousUpdatesToAdd.remove(update);
            worldData.continuousUpdatesToRemove.add(update);
        }
    }

    public static Throwable getLastDebugTrace() {
        return lastDebugTrace;
    }

    // ---- Internal drain ----

    private static void processUpdates(World world, WorldData worldData) {
        IWorldTickCallback callback;
        while ((callback = worldData.singleUpdates.poll()) != null) {
            if (debugupdate) {
                lastDebugTrace = debugTraces.remove(callback);
            }
            try {
                callback.onTick(world);
            } catch (Throwable t) {
                IC2.log.warn(ic2.core.util.LogCategory.General, t, "IWorldTickCallback single tick failed: %s", callback);
            }
        }

        worldData.continuousUpdatesInUse = true;
        for (IWorldTickCallback update : worldData.continuousUpdates) {
            if (debugupdate) {
                lastDebugTrace = debugTraces.get(update);
            }
            try {
                update.onTick(world);
            } catch (Throwable t) {
                IC2.log.warn(ic2.core.util.LogCategory.General, t, "IWorldTickCallback continuous tick failed: %s", update);
            }
        }
        worldData.continuousUpdatesInUse = false;
        if (debugupdate) {
            lastDebugTrace = null;
        }

        if (!worldData.continuousUpdatesToAdd.isEmpty()) {
            worldData.continuousUpdates.addAll(worldData.continuousUpdatesToAdd);
            worldData.continuousUpdatesToAdd.clear();
        }
        if (!worldData.continuousUpdatesToRemove.isEmpty()) {
            worldData.continuousUpdates.removeAll(worldData.continuousUpdatesToRemove);
            worldData.continuousUpdatesToRemove.clear();
        }
    }
}
