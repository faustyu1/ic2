package ic2.core.item.reactor;

import ic2.api.reactor.IReactor;
import net.minecraft.item.ItemStack;

/**
 * Port of ItemReactorMOX - a fuel rod that gets better the hotter the reactor runs, which is what
 * made it the fluid reactor's fuel.
 */
public class ItemReactorMOX extends ItemReactorUranium {

    public ItemReactorMOX(String name, int cells) {
        super(name, cells, 10000);
    }

    @Override
    protected int getFinalHeat(ItemStack stack, IReactor reactor, int x, int y, int heat) {
        if (reactor.isFluidCooled() && (float) reactor.getHeat() / reactor.getMaxHeat() > 0.5f) {
            heat *= 2;
        }
        return heat;
    }

    @Override
    protected ItemStack getDepletedStack(ItemStack stack, IReactor reactor) {
        return variant(switch (numberOfCells) {
            case 1 -> "depleted_mox";
            case 2 -> "depleted_dual_mox";
            case 4 -> "depleted_quad_mox";
            default -> throw new IllegalStateException("invalid cell count: " + numberOfCells);
        });
    }

    @Override
    public boolean acceptUraniumPulse(ItemStack stack, IReactor reactor, ItemStack pulsingStack,
                                      int youX, int youY, int pulseX, int pulseY, boolean heatrun) {
        if (!heatrun) {
            float effectiveness = (float) reactor.getHeat() / reactor.getMaxHeat();
            reactor.addOutput(4.0f * effectiveness + 1.0f);
        }
        return true;
    }
}
