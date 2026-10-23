package ic2.core.util;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.BiomeDictionary;

/** Fabric port of the part of ic2.core.util.BiomeUtil the machines use. */
public final class BiomeUtil {
    private BiomeUtil() {
    }

    /** The ambient temperature in degrees Celsius the steam boiler cools down to: 45 hot, 0 cold, 25 otherwise. */
    public static int getBiomeTemperature(World world, BlockPos pos) {
        var biome = world.getBiome(pos);
        if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.HOT)) {
            return 45;
        }
        return BiomeDictionary.hasType(biome, BiomeDictionary.Type.COLD) ? 0 : 25;
    }

    /**
     * Changes the biome of the column at pos, as the mushroom terraformer does. 1.12.2 kept one
     * biome per block column; 1.21 keeps one per 4x4x4 cell, so the whole column of cells the
     * block lies in changes, at every height. Players near the chunk are sent the new biomes.
     */
    public static void setBiome(World world, BlockPos pos, net.minecraft.registry.RegistryKey<net.minecraft.world.biome.Biome> key) {
        if (!(world instanceof net.minecraft.server.world.ServerWorld serverWorld)) {
            return;
        }
        var biome = world.getRegistryManager().get(net.minecraft.registry.RegistryKeys.BIOME).getEntry(key).orElse(null);
        if (biome == null) {
            return;
        }

        net.minecraft.world.chunk.Chunk chunk = world.getChunk(pos);
        int cellX = net.minecraft.world.biome.source.BiomeCoords.fromBlock(pos.getX());
        int cellZ = net.minecraft.world.biome.source.BiomeCoords.fromBlock(pos.getZ());
        chunk.populateBiomes((x, y, z, noise) -> x == cellX && z == cellZ ? biome : chunk.getBiomeForNoiseGen(x, y, z),
                serverWorld.getChunkManager().getNoiseConfig().getMultiNoiseSampler());
        chunk.setNeedsSaving(true);
        serverWorld.getChunkManager().chunkLoadingManager.sendChunkBiomePackets(java.util.List.of(chunk));
    }
}
