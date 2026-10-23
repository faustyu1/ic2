package ic2.core.world;

import ic2.core.IC2Mod;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.PlacedFeature;

/**
 * Fabric port of {@code ic2.core.Ic2WorldDecorator}.
 * Original was an {@code IWorldGenerator} that placed rubber trees and ores per chunk.
 *
 * Fabric 1.21.1 equivalent is {@code BiomeModifications.addFeature} with a {@code PlacedFeature}.
 * This class keeps a minimal procedural helper {@link #generateRubberTree(World, BlockPos)}
 * used by sapling grow / debug commands and registers the {@code rubber_tree} placed feature
 * into overworld biomes.
 */
public final class Ic2WorldDecorator {

    private static final Identifier RUBBER_TREE_ID = Identifier.of("ic2", "rubber_tree");
    private static final RegistryKey<PlacedFeature> RUBBER_TREE_PLACED_KEY =
            RegistryKey.of(RegistryKeys.PLACED_FEATURE, RUBBER_TREE_ID);
    private static final RegistryKey<PlacedFeature> RUBBER_TREE_SWAMP_PLACED_KEY =
            RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of("ic2", "rubber_tree_swamp"));

    private Ic2WorldDecorator() {}

    /**
     * Grows one rubber tree, the way the sapling and the {@code /ic2 tree} debug path need it.
     * The shape itself lives in {@link RubberTreeFeature}, so a tree from worldgen and one from a
     * sapling come out the same.
     *
     * @param world  the world to build in
     * @param origin the foot of the trunk
     * @return false when there was no room, in which case nothing was placed
     */
    public static boolean generateRubberTree(World world, BlockPos origin) {
        if (world == null || origin == null) {
            return false;
        }

        try {
            return RubberTreeFeature.grow(world, origin, world.getRandom());
        } catch (Throwable t) {
            IC2Mod.LOGGER.warn("[IC2] generateRubberTree failed at {}: {}", origin, t.toString());
            return false;
        }
    }

    public static void register() {
        IC2Mod.LOGGER.info("[IC2] Registering world generation (rubber tree + ores) - Fabric");

        // the feature itself has to exist before the configured feature JSON that names it loads
        RubberTreeFeature.register();

        // Rubber trees — forest and jungle get a few, swamps get many, and nothing else gets any.
        // 1.12.2 counted biome types per chunk: swamp added 5..14 trees, forest or jungle 1..5,
        // the total was halved and then used both as a percentage gate and as the attempt count.
        // The two placed features below are that in 1.21.1 terms: a rarity filter for the gate
        // and a count for the attempts.
        try {
            BiomeModifications.addFeature(
                    BiomeSelectors.tag(ConventionalBiomeTags.IS_FOREST).or(BiomeSelectors.tag(ConventionalBiomeTags.IS_JUNGLE)),
                    GenerationStep.Feature.VEGETAL_DECORATION,
                    RUBBER_TREE_PLACED_KEY);
            BiomeModifications.addFeature(
                    BiomeSelectors.tag(ConventionalBiomeTags.IS_SWAMP),
                    GenerationStep.Feature.VEGETAL_DECORATION,
                    RUBBER_TREE_SWAMP_PLACED_KEY);
            IC2Mod.LOGGER.info("[IC2] Registered rubber tree worldgen in forest, jungle and swamp biomes");
        } catch (Throwable t) {
            IC2Mod.LOGGER.warn("[IC2] Skipping rubber_tree worldgen registration (PlacedFeature not present yet): {}", t.toString());
        }

        // Ores — UNDERGROUND_ORES, also best-effort so missing JSON doesn't break boot.
        // Keeping same keys as original ores: tin/lead/uranium. Copper is Minecraft's own since 1.17
        // and generates already, so IC2 no longer adds a second copper ore.
        try {
            RegistryKey<PlacedFeature> tin = RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of("ic2", "ore_tin"));
            BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Feature.UNDERGROUND_ORES, tin);
            RegistryKey<PlacedFeature> lead = RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of("ic2", "ore_lead"));
            BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Feature.UNDERGROUND_ORES, lead);
            RegistryKey<PlacedFeature> uranium = RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of("ic2", "ore_uranium"));
            BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Feature.UNDERGROUND_ORES, uranium);
        } catch (Throwable t) {
            IC2Mod.LOGGER.warn("[IC2] Skipping ore worldgen registration: {}", t.toString());
        }
    }
}
