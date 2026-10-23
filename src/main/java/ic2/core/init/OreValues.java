package ic2.core.init;

import ic2.core.block.BlockOre;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

/**
 * What the OD scanner and the miner count as worth reporting.
 *
 * <p>1.12.2 kept a map from item stack to a value, filled from the ore dictionary as ores were
 * registered ({@code oreCoal} 1, {@code oreCopper} 2, {@code oreDiamond} 5 and so on). Nothing
 * read those numbers for anything but a {@code > 0} test, so the port drops the table and asks
 * the question the test was really asking: is this block an ore?
 *
 * <p>Ore-ness is a tag now: {@code c:ores} covers vanilla and every mod that follows the
 * convention, and IC2's own ore blocks answer for themselves.
 */
public final class OreValues {

    /** The convention tag every mod's ores are expected to be in. */
    public static final TagKey<Block> ORES = TagKey.of(RegistryKeys.BLOCK, Identifier.of("c", "ores"));

    private OreValues() {}

    /** True when a block is one a scanner should report. */
    public static boolean isValuable(BlockState state) {
        return state.getBlock() instanceof BlockOre || state.isIn(ORES);
    }
}
