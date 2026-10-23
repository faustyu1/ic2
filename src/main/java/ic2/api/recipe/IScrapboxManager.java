package ic2.api.recipe;

import java.util.Map;
import net.minecraft.item.ItemStack;

public interface IScrapboxManager extends IBasicMachineRecipeManager {
    void addDrop(ItemStack drop, float chance);

    ItemStack getDrop(ItemStack scrapbox, boolean adjustInput);

    Map<ItemStack, Float> getDrops();
}
