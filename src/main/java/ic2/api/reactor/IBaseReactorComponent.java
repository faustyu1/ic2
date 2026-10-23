package ic2.api.reactor;

import net.minecraft.item.ItemStack;

public interface IBaseReactorComponent {
    boolean canBePlacedIn(ItemStack stack, IReactor reactor);
}
