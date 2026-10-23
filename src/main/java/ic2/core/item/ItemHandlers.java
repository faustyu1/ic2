package ic2.core.item;

import ic2.core.IC2;
import ic2.core.util.LogCategory;
import net.minecraft.item.ItemStack;

/**
 * Fabric stub of decompiled ic2.core.item.ItemHandlers (1.12.2, 227 lines).
 * Original registered CF powder, scrapbox, resin, radioactive update, reactor chamber,
 * upgrade GUI, cell fill, fluid placer handlers via ItemMulti / TeBlock callbacks.
 * Fabric 1.21.1 port: ItemMulti / TeBlock handlers not ported yet — init() is a
 * logging stub to keep registration call sites compiling without side effects.
 *
 * <p>Decompiled head (80 lines) reference:
 * emptyCellFill (IItemUseHandler) traced liquids via LiquidUtil.getLiquid(world,pos),
 * checked isSource, handled WATER/LAVA -> water/lava cells via StackUtil.storeInventoryItem
 * and world.setBlockToAir replacement + StackUtil.consumeOrError.
 * reactorChamberPlace (ITePlaceHandler.canReplace) counted adjacent TileEntityNuclearReactorElectric.
 * Упрощённый порт placeReactorComponent ниже — stub, возвращает false,
 * без логики world/StackUtil, но с корректными сигнатурами для компиляции на 1.21.1.</p>
 */
public class ItemHandlers {

    public static void init() {
        IC2.log.info(LogCategory.Item, "ItemHandlers init (stub) - registering item handlers (CF powder, resin, radioactive, etc. deferred)");
    }

    /**
     * Simplified port of reactorPlacer / reactorChamberPlace logic.
     * Original: проверка соседства с TileEntityNuclearReactorElectric (count == 1).
     * Fabric stub: без проверки мира, всегда false.
     *
     * @param stack компонент реактора
     * @return false - размещение не выполнено (stub)
     */
    public static boolean placeReactorComponent(ItemStack stack) {
        return false;
    }

    /**
     * Rolls the drop of a scrap box without taking the box; right click on the box is
     * {@link ItemScrapBox#use}.
     *
     * @return the drop, or {@link ItemStack#EMPTY} for a stack that is no scrap box
     */
    public static ItemStack scrapBoxUnpack(ItemStack scrapBox) {
        ItemStack drop = ic2.api.recipe.Recipes.scrapboxDrops == null ? null
                : ic2.api.recipe.Recipes.scrapboxDrops.getDrop(scrapBox, false);
        return drop == null ? ItemStack.EMPTY : drop;
    }

    /**
     * Simplified port of resin handler logic.
     * Original (1.12.2): IItemUseHandler for resin / rubber wood tapping.
     * Fabric stub: no world/Recipes interaction, always returns false.
     *
     * @param rubberWood древесина гевеи (BlockName.rubber_wood)
     * @param output выходной стек (смола, nullable)
     * @return false - обработка не выполнена (stub)
     */
    public static boolean handleResin(ItemStack rubberWood, ItemStack output) {
        return false;
    }
}
