package ic2.core.uu;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.server.MinecraftServer;

/**
 * Port of {@code ic2.core.uu.RecipeResolver}: every crafting table recipe of the server, vanilla
 * and IC2 alike, at 1 UU per craft. Recipes without a fixed result or ingredient list (dyeing,
 * the jetpack attachment and the other special recipes) are left out, as in 1.12.2.
 */
class RecipeResolver implements IRecipeResolver {
    private static final double transformCost = 1.0;
    private final MinecraftServer server;

    RecipeResolver(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public List<RecipeTransformation> getTransformations() {
        List<RecipeTransformation> ret = new ArrayList<>();

        for (RecipeEntry<CraftingRecipe> entry : server.getRecipeManager().listAllOfType(RecipeType.CRAFTING)) {
            CraftingRecipe recipe = entry.value();
            ItemStack output = recipe.getResult(server.getRegistryManager());
            if (output.isEmpty() || recipe.getIngredients().isEmpty()) {
                continue;
            }

            List<List<LeanItemStack>> inputs = RecipeUtil.convertCraftingIngredients(recipe.getIngredients());
            if (!inputs.isEmpty()) {
                ret.add(new RecipeTransformation(transformCost, inputs, new LeanItemStack(output)));
            }
        }

        return ret;
    }
}
