package ic2.core;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

/**
 * Fabric port of ChunkLoaderLogic - keeps track of the chunks the chunk loaders keep loaded.
 *
 * <p>1.12.2 held one Forge chunk ticket per chunk loader. The port forces the chunks with the
 * world's forced chunk list, which the game saves, so they come back loaded after a restart and
 * the loaders in them pick up where they were. Since that list knows no owners, this class
 * remembers which loader wants which chunk and which chunks the loaders forced themselves: a
 * chunk is let go only when no loader wants it any more, and never when something else (the
 * {@code /forceload} command, say) had forced it first.
 */
public final class ChunkLoaderLogic {
    /** Forge's default maximumChunksPerTicket, which limited a loader to 25 chunks. */
    public static final int MAX_CHUNKS_PER_LOADER = 25;

    private static final Map<ServerWorld, State> STATES = new WeakHashMap<>();

    private static final class State {
        final Map<BlockPos, Set<ChunkPos>> loaders = new HashMap<>();
        /** Chunks forced by the loaders, as opposed to forced by something else before. */
        final Set<ChunkPos> forcedByLoaders = new HashSet<>();

        boolean isWanted(ChunkPos chunk) {
            for (Set<ChunkPos> chunks : loaders.values()) {
                if (chunks.contains(chunk)) {
                    return true;
                }
            }
            return false;
        }
    }

    private ChunkLoaderLogic() {
    }

    private static State state(ServerWorld world) {
        return STATES.computeIfAbsent(world, w -> new State());
    }

    /**
     * The loader at pos keeps the chunk loaded.
     *
     * @param reclaim whether the loader takes up a chunk it forced before the world was saved,
     *                which is then its own even though it is already forced
     */
    public static void addChunk(ServerWorld world, BlockPos pos, ChunkPos chunk, boolean reclaim) {
        State state = state(world);
        if (!state.isWanted(chunk) && (reclaim || !world.getForcedChunks().contains(chunk.toLong()))) {
            state.forcedByLoaders.add(chunk);
        }
        state.loaders.computeIfAbsent(pos.toImmutable(), p -> new HashSet<>()).add(chunk);
        world.setChunkForced(chunk.x, chunk.z, true);
    }

    /** The loader at pos no longer wants the chunk; it is let go unless another loader wants it. */
    public static void removeChunk(ServerWorld world, BlockPos pos, ChunkPos chunk) {
        State state = state(world);
        Set<ChunkPos> chunks = state.loaders.get(pos);
        if (chunks != null) {
            chunks.remove(chunk);
            if (chunks.isEmpty()) {
                state.loaders.remove(pos);
            }
        }
        letGo(world, state, chunk);
    }

    /**
     * The loader at pos stops loading anything, as releasing its ticket did.
     *
     * @param chunks the loader's chunks, which it may no longer be registered with when it is
     *               broken after its block entity was unloaded
     */
    public static void release(ServerWorld world, BlockPos pos, Collection<ChunkPos> chunks) {
        State state = state(world);
        Set<ChunkPos> all = new HashSet<>(chunks);
        Set<ChunkPos> registered = state.loaders.remove(pos);
        if (registered != null) {
            all.addAll(registered);
        }
        for (ChunkPos chunk : all) {
            letGo(world, state, chunk);
        }
    }

    private static void letGo(ServerWorld world, State state, ChunkPos chunk) {
        if (!state.isWanted(chunk) && state.forcedByLoaders.remove(chunk)) {
            world.setChunkForced(chunk.x, chunk.z, false);
        }
    }

    /**
     * The loader's chunk is unloaded, which happens to a working loader only when the world
     * closes: its chunks stay in the saved forced list and are taken up again on the next load.
     */
    public static void forget(ServerWorld world, BlockPos pos) {
        state(world).loaders.remove(pos);
    }

    /** Whether the loader at pos currently keeps the chunk loaded. */
    public static boolean isLoading(ServerWorld world, BlockPos pos, ChunkPos chunk) {
        Set<ChunkPos> chunks = state(world).loaders.get(pos);
        return chunks != null && chunks.contains(chunk) && world.getForcedChunks().contains(chunk.toLong());
    }

    public static ChunkPos getChunkCoords(BlockPos pos) {
        return new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
    }
}
