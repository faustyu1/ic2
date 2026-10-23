package ic2.core.recipe;

import ic2.api.recipe.IRecipeInput;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;

/**
 * Replacement for the 1.12.2 {@code RecipeInputOreDict}.
 *
 * <p>Forge's OreDictionary has no Fabric counterpart; the equivalent mechanism is an item tag, so
 * every {@code OreDict:<name>} entry in the config files is mapped to a tag by
 * {@link OreDictionaryCompat} and matched here.
 */
public class RecipeInputTag implements IRecipeInput {
    public final String oreDictName;
    public final TagKey<Item> tag;
    public final int amount;

    public RecipeInputTag(String oreDictName, TagKey<Item> tag, int amount) {
        this.oreDictName = oreDictName;
        this.tag = tag;
        this.amount = amount;
    }

    @Override
    public boolean matches(ItemStack subject) {
        return subject != null && !subject.isEmpty() && subject.isIn(tag);
    }

    @Override
    public int getAmount() {
        return amount;
    }

    @Override
    public List<ItemStack> getInputs() {
        List<ItemStack> ret = new ArrayList<>();
        Registries.ITEM.getEntryList(tag).ifPresent(entries -> {
            for (RegistryEntry<Item> entry : entries) {
                ret.add(new ItemStack(entry.value(), amount));
            }
        });
        return ret;
    }

    @Override
    public String toString() {
        return "tag(" + tag.id() + " x" + amount + ")";
    }
}
