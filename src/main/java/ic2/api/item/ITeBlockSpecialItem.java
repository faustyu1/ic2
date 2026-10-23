package ic2.api.item;

import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public interface ITeBlockSpecialItem {
    boolean doesOverrideDefault(ItemStack stack);
    Identifier getModelLocation(ItemStack stack);
}
