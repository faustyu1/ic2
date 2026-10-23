package ic2.core.uu;

import ic2.api.recipe.MachineRecipe;
import ic2.api.recipe.Recipes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.SmeltingRecipe;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Port of {@code ic2.core.uu.VanillaSmeltingResolver}: every furnace recipe, at 14 UU per smelt.
 *
 * <p>1.12.2 read the global furnace table, which held IC2's own smelting recipes as well. Those
 * live in {@link Recipes#furnace} now and the vanilla ones in the server's datapacks, so both are
 * read; IC2's recipe wins for an input both know, as the furnace machines do.
 */
class VanillaSmeltingResolver implements IRecipeResolver {
    private static final Logger LOG = LoggerFactory.getLogger("IC2-UU");
    private static final double transformCost = 14.0;
    private final MinecraftServer server;

    /** @param server the server whose smelting recipes count, or null for IC2's own only */
    VanillaSmeltingResolver(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public List<RecipeTransformation> getTransformations() {
        List<RecipeTransformation> ret = new ArrayList<>();
        Set<Item> ic2Inputs = new HashSet<>();

        if (Recipes.furnace != null) {
            for (MachineRecipe<ItemStack, ItemStack> recipe : Recipes.furnace.getRecipes()) {
                ItemStack input = recipe.getInput();
                ItemStack output = recipe.getOutput();
                if (input.isEmpty() || output.isEmpty()) {
                    LOG.warn("[IC2] invalid smelting recipe {} -> {}", input, output);
                    continue;
                }
                ic2Inputs.add(input.getItem());
                ret.add(new RecipeTransformation(transformCost,
                        RecipeUtil.convertIngredients(Collections.singletonList(input.copyWithCount(1))),
                        new LeanItemStack(output)));
            }
        }

        if (server != null) {
            for (RecipeEntry<SmeltingRecipe> entry : server.getRecipeManager().listAllOfType(RecipeType.SMELTING)) {
                SmeltingRecipe recipe = entry.value();
                ItemStack output = recipe.getResult(server.getRegistryManager());
                if (output.isEmpty() || recipe.getIngredients().isEmpty()) {
                    continue;
                }

                List<ItemStack> inputs = new ArrayList<>();
                for (ItemStack stack : recipe.getIngredients().get(0).getMatchingStacks()) {
                    if (!stack.isEmpty() && !ic2Inputs.contains(stack.getItem())) {
                        inputs.add(stack.copyWithCount(1));
                    }
                }
                if (!inputs.isEmpty()) {
                    ret.add(new RecipeTransformation(transformCost, RecipeUtil.convertIngredients(inputs), new LeanItemStack(output)));
                }
            }
        }

        return ret;
    }
}
