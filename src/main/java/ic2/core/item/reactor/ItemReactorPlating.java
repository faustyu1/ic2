package ic2.core.item.reactor;

import ic2.api.reactor.IReactor;
import net.minecraft.item.ItemStack;

/**
 * Port of ItemReactorPlating - raises the hull heat capacity and damps what the heat does to the
 * surroundings.
 */
public class ItemReactorPlating extends ItemReactorComponent {
    public final int maxHeatAdd;
    public final float effectModifier;

    public ItemReactorPlating(String name) {
        this(name, 0, 1.0f);
    }

    public ItemReactorPlating(String name, int maxHeatAdd, float effectModifier) {
        super(name);
        this.maxHeatAdd = maxHeatAdd;
        this.effectModifier = effectModifier;
    }

    @Override
    public void processChamber(ItemStack stack, IReactor reactor, int x, int y, boolean heatrun) {
        if (heatrun) {
            reactor.setMaxHeat(reactor.getMaxHeat() + maxHeatAdd);
            reactor.setHeatEffectModifier(reactor.getHeatEffectModifier() * effectModifier);
        }
    }

    @Override
    public float influenceExplosion(ItemStack stack, IReactor reactor) {
        return effectModifier >= 1.0f ? 0.0f : effectModifier;
    }
}
