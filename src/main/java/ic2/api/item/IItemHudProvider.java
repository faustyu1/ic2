package ic2.api.item;

import net.minecraft.item.ItemStack;

public interface IItemHudProvider {
    boolean doesProvideHUD(ItemStack stack);
    HudMode getHudMode(ItemStack stack);
    interface IItemHudBarProvider {
        int getBarPercent(ItemStack stack);
    }
}
