package ic2.core.item.reactor;

import ic2.api.reactor.IReactor;
import ic2.api.reactor.IReactorComponent;
import ic2.core.item.ItemGradualInt;
import net.minecraft.item.ItemStack;

/**
 * Base of the items that go into a nuclear reactor.
 *
 * <p>It carries what every component has in common - the durability the original gave it, which is
 * its heat or lifetime budget - and answers the {@link IReactorComponent} hooks neutrally. The
 * behaviour of the individual components (venting, exchanging, reflecting, the uranium pulses) is
 * part of the reactor port and is filled in by the subclasses as that lands.
 */
public class ItemReactorComponent extends ItemGradualInt implements IReactorComponent {

    public ItemReactorComponent(String name) {
        this(name, 0);
    }

    /**
     * @param durability the component's budget in its own damage counter - the heat a vent or
     *                   storage holds, the lifetime of a reflector - or 0 when it never wears out.
     *                   The components stack while undamaged, as they did in 1.12.2.
     */
    public ItemReactorComponent(String name, int durability) {
        super(name, durability);
    }

    @Override
    public void processChamber(ItemStack stack, IReactor reactor, int x, int y, boolean heatrun) {
    }

    @Override
    public boolean acceptUraniumPulse(ItemStack stack, IReactor reactor, ItemStack pulsingStack,
                                      int x, int y, int pulseX, int pulseY, boolean heatrun) {
        return false;
    }

    @Override
    public boolean canStoreHeat(ItemStack stack, IReactor reactor, int x, int y) {
        return false;
    }

    @Override
    public int getMaxHeat(ItemStack stack, IReactor reactor, int x, int y) {
        return 0;
    }

    @Override
    public int getCurrentHeat(ItemStack stack, IReactor reactor, int x, int y) {
        return 0;
    }

    @Override
    public int alterHeat(ItemStack stack, IReactor reactor, int x, int y, int heat) {
        return heat;
    }

    @Override
    public float influenceExplosion(ItemStack stack, IReactor reactor) {
        return 1.0f;
    }

    @Override
    public boolean canBePlacedIn(ItemStack stack, IReactor reactor) {
        return true;
    }
}
