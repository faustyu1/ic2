package ic2.core.item.tool;

import ic2.core.item.ItemWrench;

/**
 * Fabric port of {@code ic2.core.item.tool.ItemToolWrenchNew}: the advanced wrench, which in
 * 1.12.2 lasted longer and could also turn pipes.
 *
 * <p>The pipe handling is not here - the port has no {@code IPipe} - so for now it is the hand
 * wrench with its own durability.
 */
public class ItemWrenchAdvanced extends ItemWrench {

    public ItemWrenchAdvanced() {
        super("wrench_advanced", new Settings().maxCount(1).maxDamage(240));
    }
}
