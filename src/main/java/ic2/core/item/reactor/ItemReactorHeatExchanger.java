package ic2.core.item.reactor;

import ic2.api.reactor.IReactor;
import ic2.api.reactor.IReactorComponent;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.ItemStack;

/**
 * Port of ItemReactorHeatSwitch - the exchangers, which level the heat between themselves, their
 * neighbours and the reactor hull.
 *
 * <p>The rate depends on how much hotter the other side is, in the stepped way the original
 * computed it, so the numbers of a 1.12.2 reactor design still hold.
 */
public class ItemReactorHeatExchanger extends ItemReactorHeatStorage {
    public final int switchSide;
    public final int switchReactor;

    public ItemReactorHeatExchanger(String name) {
        this(name, 0, 0, 0);
    }

    public ItemReactorHeatExchanger(String name, int heatStorage, int switchSide, int switchReactor) {
        super(name, heatStorage);
        this.switchSide = switchSide;
        this.switchReactor = switchReactor;
    }

    @Override
    public void processChamber(ItemStack stack, IReactor reactor, int x, int y, boolean heatrun) {
        if (!heatrun) {
            return;
        }

        int myHeat = 0;

        if (switchSide > 0) {
            List<Neighbour> acceptors = new ArrayList<>();
            checkHeatAcceptor(reactor, x - 1, y, acceptors);
            checkHeatAcceptor(reactor, x + 1, y, acceptors);
            checkHeatAcceptor(reactor, x, y - 1, acceptors);
            checkHeatAcceptor(reactor, x, y + 1, acceptors);

            for (Neighbour neighbour : acceptors) {
                IReactorComponent heatable = (IReactorComponent) neighbour.stack.getItem();
                double mine = (double) getCurrentHeat(stack, reactor, x, y) * 100.0 / getMaxHeat(stack, reactor, x, y);
                double theirMax = heatable.getMaxHeat(neighbour.stack, reactor, neighbour.x, neighbour.y);
                double theirs = heatable.getCurrentHeat(neighbour.stack, reactor, neighbour.x, neighbour.y) * 100.0 / theirMax;

                int add = amount(theirs, mine, theirMax / 100.0 * (theirs + mine / 2.0), switchSide);

                myHeat -= add;
                myHeat += heatable.alterHeat(neighbour.stack, reactor, neighbour.x, neighbour.y, add);
            }
        }

        if (switchReactor > 0) {
            double mine = (double) getCurrentHeat(stack, reactor, x, y) * 100.0 / getMaxHeat(stack, reactor, x, y);
            double hull = (double) reactor.getHeat() * 100.0 / reactor.getMaxHeat();

            int add = amount(hull, mine, Math.round(reactor.getMaxHeat() / 100.0 * (hull + mine / 2.0)), switchReactor);

            myHeat -= add;
            reactor.setHeat(reactor.getHeat() + add);
        }

        alterHeat(stack, reactor, x, y, myHeat);
    }

    /**
     * How much heat moves towards the other side: the raw share, capped by the switch rate, cut
     * down in steps while both sides are nearly cold, and reversed when the other side is hotter.
     */
    private int amount(double theirs, double mine, double raw, int limit) {
        int add = (int) raw;
        if (add > limit) {
            add = limit;
        }

        double combined = theirs + mine / 2.0;
        if (combined < 1.0) {
            add = switchSide / 2;
        }
        if (combined < 0.75) {
            add = switchSide / 4;
        }
        if (combined < 0.5) {
            add = switchSide / 8;
        }
        if (combined < 0.25) {
            add = 1;
        }

        double theirsRounded = Math.round(theirs * 10.0) / 10.0;
        double mineRounded = Math.round(mine * 10.0) / 10.0;
        if (theirsRounded > mineRounded) {
            add -= 2 * add;
        } else if (theirsRounded == mineRounded) {
            add = 0;
        }

        return add;
    }

    private void checkHeatAcceptor(IReactor reactor, int x, int y, List<Neighbour> acceptors) {
        ItemStack stack = reactor.getItemAt(x, y);
        if (stack != null && stack.getItem() instanceof IReactorComponent component
                && component.canStoreHeat(stack, reactor, x, y)) {
            acceptors.add(new Neighbour(stack, x, y));
        }
    }

    private record Neighbour(ItemStack stack, int x, int y) {}
}
