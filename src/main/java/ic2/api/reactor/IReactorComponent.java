package ic2.api.reactor;

import net.minecraft.item.ItemStack;

public interface IReactorComponent extends IBaseReactorComponent {
    void processChamber(ItemStack stack, IReactor reactor, int x, int y, boolean heatrun);
    boolean acceptUraniumPulse(ItemStack stack, IReactor reactor, ItemStack pulsingStack, int x, int y, int pulseX, int pulseY, boolean heatrun);
    boolean canStoreHeat(ItemStack stack, IReactor reactor, int x, int y);
    int getMaxHeat(ItemStack stack, IReactor reactor, int x, int y);
    int getCurrentHeat(ItemStack stack, IReactor reactor, int x, int y);
    int alterHeat(ItemStack stack, IReactor reactor, int x, int y, int heat);
    float influenceExplosion(ItemStack stack, IReactor reactor);
}
