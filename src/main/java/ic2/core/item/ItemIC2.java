package ic2.core.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Fabric stub of ItemIC2 - base for all IC2 items, holds name and registration id.
 */
public class ItemIC2 extends Item {
    protected final String name;
    public ItemIC2(String name, Settings settings){ super(settings); this.name=name; }
    public ItemIC2(String name){ super(new Settings()); this.name=name; }
    public String getItemName(){ return name; }

    // An electric item shows how much EU it holds where a normal item shows its wear.

    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return ElectricItemBar.visible(stack) || super.isItemBarVisible(stack);
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        return ElectricItemBar.visible(stack) ? ElectricItemBar.step(stack) : super.getItemBarStep(stack);
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        return ElectricItemBar.visible(stack) ? ElectricItemBar.color(stack) : super.getItemBarColor(stack);
    }
}
