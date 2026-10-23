package ic2.core.item.reactor;

import ic2.api.reactor.IReactor;
import net.minecraft.item.ItemStack;

/**
 * Port of ItemReactorHeatStorage - a component that holds heat, i.e. the heat capacitors and the
 * base of the vents and exchangers.
 *
 * <p>The heat it currently holds is its custom damage, so a hot capacitor taken out of a reactor
 * stays hot, exactly as in 1.12.2.
 */
public class ItemReactorHeatStorage extends ItemReactorComponent {

    public ItemReactorHeatStorage(String name) {
        this(name, 0);
    }

    public ItemReactorHeatStorage(String name, int heatStorage) {
        super(name, heatStorage);
    }

    @Override
    public boolean canStoreHeat(ItemStack stack, IReactor reactor, int x, int y) {
        return true;
    }

    @Override
    public int getMaxHeat(ItemStack stack, IReactor reactor, int x, int y) {
        return getMaxCustomDamage(stack);
    }

    @Override
    public int getCurrentHeat(ItemStack stack, IReactor reactor, int x, int y) {
        return getCustomDamage(stack);
    }

    /**
     * Moves heat into or out of the component.
     *
     * @return the heat that did not fit, which the caller has to place elsewhere; the component
     *         melts down (is removed) when it overflows
     */
    @Override
    public int alterHeat(ItemStack stack, IReactor reactor, int x, int y, int heat) {
        int myHeat = getCurrentHeat(stack, reactor, x, y) + heat;
        int max = getMaxHeat(stack, reactor, x, y);

        if (myHeat > max) {
            reactor.setItemAt(x, y, ItemStack.EMPTY);
            return max - myHeat + 1;
        }

        if (myHeat < 0) {
            heat = myHeat;
            myHeat = 0;
        } else {
            heat = 0;
        }

        setCustomDamage(stack, myHeat);
        return heat;
    }
}
