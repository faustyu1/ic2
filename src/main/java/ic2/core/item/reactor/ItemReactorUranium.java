package ic2.core.item.reactor;

import ic2.api.reactor.IReactor;
import ic2.api.reactor.IReactorComponent;
import ic2.core.init.IC2ItemVariants;
import ic2.core.item.ItemRadioactive;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Queue;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * Port of ItemReactorUranium - the fuel rod, the only component that produces anything.
 *
 * <p>Each of its cells sends pulses to its four neighbours; the pulses that come back decide both
 * the energy output and, on the heat pass, the heat, which is the triangular number of the pulse
 * count times four. The heat goes into the neighbouring heat storages first and only what is left
 * reaches the hull.
 */
public class ItemReactorUranium extends ItemReactorComponent {
    /** How many uranium cells the rod holds: one, two or four. */
    public final int numberOfCells;

    public ItemReactorUranium(String name) {
        this(name, 1, 20000);
    }

    public ItemReactorUranium(String name, int cells) {
        this(name, cells, 20000);
    }

    protected ItemReactorUranium(String name, int cells, int duration) {
        super(name, duration);
        this.numberOfCells = cells;
    }

    @Override
    public void processChamber(ItemStack stack, IReactor reactor, int x, int y, boolean heatRun) {
        if (!reactor.produceEnergy()) {
            return;
        }

        int basePulses = 1 + numberOfCells / 2;

        for (int iteration = 0; iteration < numberOfCells; iteration++) {
            if (!heatRun) {
                for (int i = 0; i < basePulses; i++) {
                    acceptUraniumPulse(stack, reactor, stack, x, y, x, y, false);
                }

                checkPulseable(reactor, x - 1, y, stack, x, y, false);
                checkPulseable(reactor, x + 1, y, stack, x, y, false);
                checkPulseable(reactor, x, y - 1, stack, x, y, false);
                checkPulseable(reactor, x, y + 1, stack, x, y, false);
            } else {
                int pulses = basePulses
                        + checkPulseable(reactor, x - 1, y, stack, x, y, true)
                        + checkPulseable(reactor, x + 1, y, stack, x, y, true)
                        + checkPulseable(reactor, x, y - 1, stack, x, y, true)
                        + checkPulseable(reactor, x, y + 1, stack, x, y, true);

                int heat = getFinalHeat(stack, reactor, x, y, triangularNumber(pulses) * 4);

                Queue<Neighbour> heatAcceptors = new ArrayDeque<>();
                checkHeatAcceptor(reactor, x - 1, y, heatAcceptors);
                checkHeatAcceptor(reactor, x + 1, y, heatAcceptors);
                checkHeatAcceptor(reactor, x, y - 1, heatAcceptors);
                checkHeatAcceptor(reactor, x, y + 1, heatAcceptors);

                while (!heatAcceptors.isEmpty() && heat > 0) {
                    int share = heat / heatAcceptors.size();
                    heat -= share;

                    Neighbour acceptor = heatAcceptors.remove();
                    IReactorComponent component = (IReactorComponent) acceptor.stack.getItem();
                    heat += component.alterHeat(acceptor.stack, reactor, acceptor.x, acceptor.y, share);
                }

                if (heat > 0) {
                    reactor.addHeat(heat);
                }
            }
        }

        if (!heatRun) {
            if (getCustomDamage(stack) >= getMaxCustomDamage(stack) - 1) {
                reactor.setItemAt(x, y, getDepletedStack(stack, reactor));
            } else {
                applyCustomDamage(stack, 1, null);
            }
        }
    }

    /** Lets the MOX rod scale its heat with the reactor temperature. */
    protected int getFinalHeat(ItemStack stack, IReactor reactor, int x, int y, int heat) {
        return heat;
    }

    /** What the rod leaves behind once it is used up. */
    protected ItemStack getDepletedStack(ItemStack stack, IReactor reactor) {
        return variant(switch (numberOfCells) {
            case 1 -> "depleted_uranium";
            case 2 -> "depleted_dual_uranium";
            case 4 -> "depleted_quad_uranium";
            default -> throw new IllegalStateException("invalid cell count: " + numberOfCells);
        });
    }

    protected static ItemStack variant(String name) {
        Item item = IC2ItemVariants.get("nuclear", name);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    /** @return 1 when the neighbour took the pulse, which is what makes it count */
    protected static int checkPulseable(IReactor reactor, int x, int y, ItemStack stack,
                                        int meX, int meY, boolean heatrun) {
        ItemStack other = reactor.getItemAt(x, y);
        return other != null && other.getItem() instanceof IReactorComponent component
                && component.acceptUraniumPulse(other, reactor, stack, x, y, meX, meY, heatrun) ? 1 : 0;
    }

    protected static int triangularNumber(int x) {
        return (x * x + x) / 2;
    }

    protected void checkHeatAcceptor(IReactor reactor, int x, int y, Collection<Neighbour> acceptors) {
        ItemStack stack = reactor.getItemAt(x, y);
        if (stack != null && stack.getItem() instanceof IReactorComponent component
                && component.canStoreHeat(stack, reactor, x, y)) {
            acceptors.add(new Neighbour(stack, x, y));
        }
    }

    @Override
    public boolean acceptUraniumPulse(ItemStack stack, IReactor reactor, ItemStack pulsingStack,
                                      int youX, int youY, int pulseX, int pulseY, boolean heatrun) {
        if (!heatrun) {
            reactor.addOutput(1.0f);
        }
        return true;
    }

    @Override
    public float influenceExplosion(ItemStack stack, IReactor reactor) {
        return 2.0f * numberOfCells;
    }

    /**
     * A bare rod irradiates whoever carries it, the same {@code onUpdate} the nuclear materials
     * had in 1.12.2 - a flat 10 seconds at amplifier 100, whatever the rod's wear. The MOX rod
     * inherits this, as it did in 1.12.2.
     */
    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        ItemRadioactive.irradiate(world, entity, 200, 100);
    }

    protected record Neighbour(ItemStack stack, int x, int y) {}
}
