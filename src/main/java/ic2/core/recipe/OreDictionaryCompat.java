package ic2.core.recipe;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

/**
 * Translates the Forge ore dictionary names used throughout {@code assets/ic2/config/*.ini}
 * into Fabric item tags.
 *
 * <p>Forge's {@code OreDictionary} does not exist on Fabric; the equivalent cross-mod mechanism is
 * a tag in the {@code c} namespace. Most IC2 entries follow the regular {@code <prefix><Material>}
 * pattern and map mechanically to {@code c:<prefix plural>/<material>}; the irregular ones are
 * listed explicitly so the shipped config files stay untouched.
 */
public final class OreDictionaryCompat {
    /** Ore dict prefix -> tag directory, e.g. ingotCopper -> c:ingots/copper. */
    private static final Map<String, String> PREFIX_DIRS = new HashMap<>();
    /** Names that do not follow the prefix+material pattern. */
    private static final Map<String, Identifier> EXPLICIT = new HashMap<>();
    private static final Map<String, TagKey<Item>> CACHE = new HashMap<>();

    static {
        PREFIX_DIRS.put("ingot", "c:ingots");
        PREFIX_DIRS.put("ore", "c:ores");
        PREFIX_DIRS.put("block", "c:storage_blocks");
        PREFIX_DIRS.put("dust", "c:dusts");
        PREFIX_DIRS.put("gem", "c:gems");
        PREFIX_DIRS.put("nugget", "c:nuggets");
        PREFIX_DIRS.put("plate", "c:plates");
        PREFIX_DIRS.put("dye", "c:dyes");
        // raw metal, which ores drop since 1.17; the port's macerator recipes name it rawCopper etc.
        PREFIX_DIRS.put("raw", "c:raw_materials");
        // IC2-specific intermediate products keep their own namespace
        PREFIX_DIRS.put("crushedPurified", "ic2:purified");
        PREFIX_DIRS.put("crushed", "ic2:crushed");
        PREFIX_DIRS.put("circuit", "ic2:circuits");
        PREFIX_DIRS.put("craftingTool", "ic2:tools");

        EXPLICIT.put("logWood", Identifier.of("minecraft", "logs"));
        EXPLICIT.put("plankWood", Identifier.of("minecraft", "planks"));
        EXPLICIT.put("treeLeaves", Identifier.of("minecraft", "leaves"));
        EXPLICIT.put("treeSapling", Identifier.of("minecraft", "saplings"));
        EXPLICIT.put("stickWood", Identifier.of("c", "rods/wooden"));
        EXPLICIT.put("chestWood", Identifier.of("c", "chests/wooden"));
        EXPLICIT.put("enderpearl", Identifier.of("c", "ender_pearls"));
        EXPLICIT.put("itemRubber", Identifier.of("ic2", "rubber"));
        EXPLICIT.put("woodRubber", Identifier.of("ic2", "rubber_wood"));
        EXPLICIT.put("itemSlag", Identifier.of("ic2", "slag"));
        EXPLICIT.put("materialScrap", Identifier.of("ic2", "scrap"));
    }

    private OreDictionaryCompat() {}

    /**
     * @param oreDictName ore dictionary name as written in the config, e.g. {@code ingotCopper}
     * @return the item tag that stands in for it
     */
    public static TagKey<Item> getTag(String oreDictName) {
        return CACHE.computeIfAbsent(oreDictName, name -> TagKey.of(RegistryKeys.ITEM, getTagId(name)));
    }

    public static Identifier getTagId(String oreDictName) {
        Identifier explicit = EXPLICIT.get(oreDictName);
        if (explicit != null) {
            return explicit;
        }

        // longest prefix wins so crushedPurifiedGold does not resolve as crushed + PurifiedGold
        String bestPrefix = null;
        for (String prefix : PREFIX_DIRS.keySet()) {
            if (oreDictName.length() > prefix.length()
                    && oreDictName.startsWith(prefix)
                    && Character.isUpperCase(oreDictName.charAt(prefix.length()))
                    && (bestPrefix == null || prefix.length() > bestPrefix.length())) {
                bestPrefix = prefix;
            }
        }

        if (bestPrefix == null) {
            return Identifier.of("c", toSnakeCase(oreDictName));
        }

        String dir = PREFIX_DIRS.get(bestPrefix);
        String material = toSnakeCase(oreDictName.substring(bestPrefix.length()));
        int sep = dir.indexOf(':');
        return Identifier.of(dir.substring(0, sep), dir.substring(sep + 1) + "/" + material);
    }

    /** {@code SiliconDioxide} -> {@code silicon_dioxide}, {@code Uranium235} -> {@code uranium_235}. */
    static String toSnakeCase(String name) {
        StringBuilder ret = new StringBuilder(name.length() + 4);
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            boolean boundary = i > 0
                    && (Character.isUpperCase(c)
                        || (Character.isDigit(c) && !Character.isDigit(name.charAt(i - 1))));
            if (boundary && ret.length() > 0 && ret.charAt(ret.length() - 1) != '_') {
                ret.append('_');
            }
            ret.append(Character.toLowerCase(c));
        }
        return ret.toString();
    }
}
