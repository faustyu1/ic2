package ic2.core.util;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;

/**
 * Fabric port of ic2.core.util.WorldSearchUtil - finding the block entities around a position.
 *
 * <p>It walks the chunks the search box touches and looks at the block entities they already
 * hold, rather than asking the world for every position in the box; the reactor runs this over a
 * 9x9x9 box every cycle, and the difference is what keeps that cheap.
 */
public final class WorldSearchUtil {
    private WorldSearchUtil() {}

    public static void findTileEntities(World world, BlockPos center, int range, ITileEntityResultHandler handler) {
        int minX = center.getX() - range;
        int minY = center.getY() - range;
        int minZ = center.getZ() - range;
        int maxX = center.getX() + range;
        int maxY = center.getY() + range;
        int maxZ = center.getZ() + range;

        for (int chunkX = minX >> 4; chunkX <= maxX >> 4; chunkX++) {
            for (int chunkZ = minZ >> 4; chunkZ <= maxZ >> 4; chunkZ++) {
                if (!world.isChunkLoaded(chunkX, chunkZ)) {
                    continue;
                }

                Chunk chunk = world.getChunk(chunkX, chunkZ);
                if (!(chunk instanceof WorldChunk worldChunk)) {
                    continue;
                }

                for (BlockPos pos : worldChunk.getBlockEntityPositions()) {
                    if (pos.getY() < minY || pos.getY() > maxY
                            || pos.getX() < minX || pos.getX() > maxX
                            || pos.getZ() < minZ || pos.getZ() > maxZ) {
                        continue;
                    }

                    BlockEntity be = worldChunk.getBlockEntity(pos);
                    if (be != null && handler.onMatch(be)) {
                        return;
                    }
                }
            }
        }
    }

    public interface ITileEntityResultHandler {
        /** @return true to stop the search */
        boolean onMatch(BlockEntity te);
    }
}
