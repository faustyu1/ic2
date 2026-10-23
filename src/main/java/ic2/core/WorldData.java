package ic2.core;

import ic2.core.energy.grid.EnergyNetLocal;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Fabric port of ic2.core.WorldData.
 * Original was dimension-indexed via dimension id (ConcurrentMap<Integer, WorldData>).
 * Fabric 1.21.1 uses RegistryKey<World> with multiple ServerWorld instances sharing dimension;
 * for the stub we use a WeakHashMap<World, WorldData> so unloading is automatic and
 * per-World instance (matches Fabric world lifecycle).
 */
public class WorldData {
    private static final Map<World, WorldData> INSTANCES = Collections.synchronizedMap(new WeakHashMap<>());

    // Tick queues — accessed by TickHandler
    final Queue<IWorldTickCallback> singleUpdates = new ConcurrentLinkedQueue<>();
    final Set<IWorldTickCallback> continuousUpdates = new HashSet<>();
    boolean continuousUpdatesInUse = false;
    final List<IWorldTickCallback> continuousUpdatesToAdd = new ArrayList<>();
    final List<IWorldTickCallback> continuousUpdatesToRemove = new ArrayList<>();

    public final EnergyNetLocal energyNet;
    public final WindSim windSim;

    // kept for compatibility with decompiled fields (world gen decorators)
    public final Map<net.minecraft.world.chunk.Chunk, net.minecraft.nbt.NbtCompound> worldGenData = new IdentityHashMap<>();
    public final Set<net.minecraft.world.chunk.Chunk> chunksToDecorate = Collections.newSetFromMap(new IdentityHashMap<>());
    public final Set<net.minecraft.world.chunk.Chunk> pendingUnloadChunks = Collections.newSetFromMap(new IdentityHashMap<>());

    public WorldData(World world) {
        this.energyNet = EnergyNetLocal.create(world);
        // windSim is null on client side in original; keep non-null stub for both sides
        // so TileEntityWindGenerator never NPEs.
        this.windSim = new WindSim(world);
    }

    public static WorldData get(World world) {
        return get(world, true);
    }

    public static WorldData get(World world, boolean load) {
        if (world == null) {
            throw new IllegalArgumentException("world is null");
        }
        WorldData ret = INSTANCES.get(world);
        if (ret == null && load) {
            synchronized (INSTANCES) {
                ret = INSTANCES.get(world);
                if (ret == null) {
                    ret = new WorldData(world);
                    INSTANCES.put(world, ret);
                }
            }
        }
        return ret;
    }

    public static void onWorldUnload(World world) {
        if (world != null) {
            INSTANCES.remove(world);
        }
    }
}
