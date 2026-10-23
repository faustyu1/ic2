package ic2.api.upgrade;

import java.util.Collection;
import java.util.Set;
import net.minecraft.item.ItemStack;

public interface IUpgradeItem {
    boolean isSuitableFor(ItemStack stack, Set<UpgradableProperty> properties);
    boolean onTick(ItemStack stack, IUpgradableBlock parent);
    Collection<ItemStack> onProcessEnd(ItemStack stack, IUpgradableBlock parent, Collection<ItemStack> output);
}
