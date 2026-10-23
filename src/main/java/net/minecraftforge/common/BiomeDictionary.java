package net.minecraftforge.common;

import java.util.Set;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.registry.entry.RegistryEntry;

/**
 * Minimal shim for Forge's BiomeDictionary, which IC2 uses to give a crop a bonus for the kind of
 * place it grows in.
 *
 * <p>Forge kept a hand-curated table of biome types; on Fabric the closest equivalent is the
 * conventional biome tags, so each type is answered from the tags a biome carries. A biome that
 * carries no matching tag simply has no bonus, which is what an unknown biome got in 1.12.2 too.
 */
public class BiomeDictionary {
    public enum Type {
        HOT, COLD, SPARSE, DENSE, WET, DRY, SAVANNA, CONIFEROUS, JUNGLE,
        SPOOKY, DEAD, LUSH, NETHER, END, MUSHROOM, MAGICAL, RARE,
        OCEAN, RIVER, WATER, MESA, FOREST, PLAINS, MOUNTAIN, HILLS,
        SWAMP, SANDY, SNOWY, WASTELAND, BEACH, VOID
    }

    private BiomeDictionary() {}

    /** Whether the biome counts as that kind of place. */
    public static boolean hasType(RegistryEntry<Biome> biome, Type type) {
        if (biome == null || type == null) {
            return false;
        }

        return switch (type) {
            case OCEAN -> biome.isIn(BiomeTags.IS_OCEAN) || biome.isIn(BiomeTags.IS_DEEP_OCEAN);
            case RIVER -> biome.isIn(BiomeTags.IS_RIVER);
            case WATER -> biome.isIn(BiomeTags.IS_OCEAN) || biome.isIn(BiomeTags.IS_DEEP_OCEAN)
                    || biome.isIn(BiomeTags.IS_RIVER);
            case BEACH -> biome.isIn(BiomeTags.IS_BEACH);
            case FOREST -> biome.isIn(BiomeTags.IS_FOREST);
            case JUNGLE -> biome.isIn(BiomeTags.IS_JUNGLE);
            case SAVANNA -> biome.isIn(BiomeTags.IS_SAVANNA);
            case MOUNTAIN, HILLS -> biome.isIn(BiomeTags.IS_MOUNTAIN)
                    || biome.isIn(BiomeTags.IS_HILL);
            case NETHER -> biome.isIn(BiomeTags.IS_NETHER);
            case END -> biome.isIn(BiomeTags.IS_END);
            case CONIFEROUS -> biome.isIn(BiomeTags.IS_TAIGA);
            default -> conventional(biome, type.name().toLowerCase(java.util.Locale.ROOT));
        };
    }

    /** The conventional tag of that name, e.g. {@code c:is_swamp}, if the biome carries it. */
    private static boolean conventional(RegistryEntry<Biome> biome, String name) {
        TagKey<Biome> tag = TagKey.of(RegistryKeys.BIOME, Identifier.of("c", "is_" + name));
        return biome.isIn(tag);
    }

    /** The types a biome has, for callers that want the whole set. */
    public static Set<Type> getTypes(RegistryEntry<Biome> biome) {
        Set<Type> ret = java.util.EnumSet.noneOf(Type.class);
        for (Type type : Type.values()) {
            if (hasType(biome, type)) {
                ret.add(type);
            }
        }
        return ret;
    }
}
