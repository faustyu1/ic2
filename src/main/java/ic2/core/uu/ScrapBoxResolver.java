package ic2.core.uu;

import ic2.api.recipe.Recipes;
import ic2.core.init.IC2ItemVariants;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Port of {@code ic2.core.uu.ScrapBoxResolver}: each scrap box drop costs as many scrap boxes as
 * it takes on average to get it (one over its chance, rounded), plus 1 UU.
 */
class ScrapBoxResolver implements IRecipeResolver {
    private static final double transformCost = 1.0;

    @Override
    public List<RecipeTransformation> getTransformations() {
        List<RecipeTransformation> ret = new ArrayList<>();
        Item scrapBox = IC2ItemVariants.get("crafting", "scrap_box");
        if (Recipes.scrapboxDrops == null || scrapBox == null) {
            return ret;
        }

        for (Map.Entry<ItemStack, Float> drop : Recipes.scrapboxDrops.getDrops().entrySet()) {
            if (drop.getKey() == null || drop.getKey().isEmpty()) {
                continue;
            }

            int amount = Math.max(1, Math.round(1.0F / drop.getValue()));
            List<LeanItemStack> input = Collections.singletonList(new LeanItemStack(scrapBox, amount));
            ret.add(new RecipeTransformation(transformCost, Collections.singletonList(input), new LeanItemStack(drop.getKey())));
        }

        return ret;
    }
}
