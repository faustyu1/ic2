package ic2.core.init;

import java.util.List;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.entry.LootTableEntry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

/**
 * Fabric port of Ic2Loot - puts IC2 items into the vanilla chests.
 *
 * <p>1.12.2 merged the {@code ic2} pool of {@code ic2:chests/<name>} into the vanilla table of the
 * same name as it loaded. Here the vanilla table gets a pool that rolls the IC2 table once, which
 * rolls its own pool the same 1 to 3 times. Only the game's own tables are touched, so a datapack
 * that replaces a chest table keeps it as it wrote it.
 */
public final class Ic2Loot {
    /** The chests that have an IC2 table in data/ic2/loot_table/chests. */
    public static final List<String> CHESTS = List.of(
            "abandoned_mineshaft", "desert_pyramid", "end_city_treasure", "igloo_chest", "jungle_temple",
            "nether_bridge", "simple_dungeon", "spawn_bonus_chest", "stronghold_corridor", "stronghold_crossing",
            "stronghold_library");

    private Ic2Loot() {}

    public static void init() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            if (!source.isBuiltin() || !key.getValue().getNamespace().equals("minecraft")) {
                return;
            }

            String path = key.getValue().getPath();
            if (!path.startsWith("chests/")) {
                return;
            }

            String chest = path.substring("chests/".length());
            String ic2Table = ic2TableFor(chest);
            if (ic2Table == null) {
                return;
            }

            RegistryKey<LootTable> ic2Key = RegistryKey.of(RegistryKeys.LOOT_TABLE, Identifier.of("ic2", "chests/" + ic2Table));
            tableBuilder.pool(LootPool.builder().with(LootTableEntry.builder(ic2Key)));
        });
    }

    /**
     * The IC2 table a vanilla chest takes. The 1.12.2 village blacksmith became the weaponsmith,
     * the chest that kept its weapons and ingots.
     */
    static String ic2TableFor(String vanillaChest) {
        if (vanillaChest.equals("village/village_weaponsmith")) {
            return "village_blacksmith";
        }
        return CHESTS.contains(vanillaChest) ? vanillaChest : null;
    }
}
