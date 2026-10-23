package ic2.core.item.reactor;

import ic2.api.reactor.IReactor;
import ic2.api.reactor.IReactorComponent;
import net.minecraft.item.ItemStack;

/**
 * Port of ItemReactorReflector - bounces a uranium pulse back into the cell that sent it.
 *
 * <p>A reflector with no budget is the iridium one, which never wears out.
 */
public class ItemReactorReflector extends ItemReactorComponent {

    public ItemReactorReflector(String name) {
        this(name, 0);
    }

    public ItemReactorReflector(String name, int durability) {
        super(name, durability);
    }

    @Override
    public boolean acceptUraniumPulse(ItemStack stack, IReactor reactor, ItemStack pulsingStack,
                                      int youX, int youY, int pulseX, int pulseY, boolean heatrun) {
        if (!heatrun) {
            if (pulsingStack.getItem() instanceof IReactorComponent source) {
                source.acceptUraniumPulse(pulsingStack, reactor, stack, pulseX, pulseY, youX, youY, heatrun);
            }
        } else if (getMaxCustomDamage(stack) > 0) {
            if (getCustomDamage(stack) + 1 >= getMaxCustomDamage(stack)) {
                reactor.setItemAt(youX, youY, ItemStack.EMPTY);
            } else {
                setCustomDamage(stack, getCustomDamage(stack) + 1);
            }
        }

        return true;
    }

    @Override
    public float influenceExplosion(ItemStack stack, IReactor reactor) {
        return -1.0f;
    }
}
