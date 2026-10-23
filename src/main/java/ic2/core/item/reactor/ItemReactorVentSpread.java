package ic2.core.item.reactor;

import ic2.api.reactor.IReactor;
import ic2.api.reactor.IReactorComponent;
import net.minecraft.item.ItemStack;

/**
 * Port of ItemReactorVentSpread - the component vent, which cools its four neighbours rather than
 * holding heat itself.
 */
public class ItemReactorVentSpread extends ItemReactorComponent {
    public final int sideVent;

    public ItemReactorVentSpread(String name, int sideVent) {
        super(name);
        this.sideVent = sideVent;
    }

    @Override
    public void processChamber(ItemStack stack, IReactor reactor, int x, int y, boolean heatrun) {
        if (!heatrun) {
            return;
        }

        cool(reactor, x - 1, y);
        cool(reactor, x + 1, y);
        cool(reactor, x, y - 1);
        cool(reactor, x, y + 1);
    }

    private void cool(IReactor reactor, int x, int y) {
        ItemStack stack = reactor.getItemAt(x, y);
        if (stack == null || !(stack.getItem() instanceof IReactorComponent component)
                || !component.canStoreHeat(stack, reactor, x, y)) {
            return;
        }

        int self = component.alterHeat(stack, reactor, x, y, -sideVent);
        if (self <= 0) {
            reactor.addEmitHeat(self + sideVent);
        }
    }
}
