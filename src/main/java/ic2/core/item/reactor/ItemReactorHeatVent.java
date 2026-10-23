package ic2.core.item.reactor;

import ic2.api.reactor.IReactor;
import net.minecraft.item.ItemStack;

/**
 * Port of ItemReactorVent - a vent that pulls heat out of the reactor hull and radiates its own
 * heat away.
 */
public class ItemReactorHeatVent extends ItemReactorHeatStorage {
    public final int selfVent;
    public final int reactorVent;

    public ItemReactorHeatVent(String name) {
        this(name, 0, 0, 0);
    }

    public ItemReactorHeatVent(String name, int heatStorage, int selfVent, int reactorVent) {
        super(name, heatStorage);
        this.selfVent = selfVent;
        this.reactorVent = reactorVent;
    }

    @Override
    public void processChamber(ItemStack stack, IReactor reactor, int x, int y, boolean heatrun) {
        if (!heatrun) {
            return;
        }

        if (reactorVent > 0) {
            int rheat = reactor.getHeat();
            int reactorDrain = Math.min(rheat, reactorVent);
            rheat -= reactorDrain;

            if (alterHeat(stack, reactor, x, y, reactorDrain) > 0) {
                return;
            }
            reactor.setHeat(rheat);
        }

        int self = alterHeat(stack, reactor, x, y, -selfVent);
        if (self <= 0) {
            reactor.addEmitHeat(self + selfVent);
        }
    }
}
