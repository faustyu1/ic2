package ic2.core.item.reactor;

import ic2.api.reactor.IReactor;
import net.minecraft.item.ItemStack;

/**
 * Port of ItemReactorCondensator - swallows heat until its coolant is used up, and never gives any
 * back.
 */
public class ItemReactorCondensator extends ItemReactorComponent {

    public ItemReactorCondensator(String name) {
        this(name, 0);
    }

    public ItemReactorCondensator(String name, int capacity) {
        super(name, capacity);
    }

    @Override
    public boolean canStoreHeat(ItemStack stack, IReactor reactor, int x, int y) {
        return getCustomDamage(stack) < getMaxCustomDamage(stack);
    }

    @Override
    public int getMaxHeat(ItemStack stack, IReactor reactor, int x, int y) {
        return getMaxCustomDamage(stack);
    }

    @Override
    public int getCurrentHeat(ItemStack stack, IReactor reactor, int x, int y) {
        return getCustomDamage(stack);
    }

    @Override
    public int alterHeat(ItemStack stack, IReactor reactor, int x, int y, int heat) {
        if (heat < 0) {
            return heat;
        }

        int current = getCustomDamage(stack);
        int amount = Math.min(heat, getMaxHeat(stack, reactor, x, y) - current);
        setCustomDamage(stack, current + amount);
        return heat - amount;
    }
}
