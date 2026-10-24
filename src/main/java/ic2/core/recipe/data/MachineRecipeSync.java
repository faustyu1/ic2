package ic2.core.recipe.data;

import ic2.api.recipe.IBasicMachineRecipeManager;
import ic2.api.recipe.IRecipeInput;
import ic2.api.recipe.MachineRecipe;
import ic2.core.init.Rezepte;
import ic2.core.recipe.BasicMachineRecipeManager;
import ic2.core.recipe.RecipeInputIngredient;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hands the datapack machine recipes to the machines' recipe managers whenever the recipes are
 * (re)loaded: on the server as it starts and after {@code /reload}, on a client of a remote server
 * as the server sends its recipes. What other mods added through the API stays as it is.
 */
public final class MachineRecipeSync {
    private static final Logger LOG = LoggerFactory.getLogger("IC2-Recipes");

    private MachineRecipeSync() {}

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> apply(server.getRecipeManager()));
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> apply(server.getRecipeManager()));
    }

    public static synchronized void apply(RecipeManager recipes) {
        int total = 0;
        for (MachineRecipeKind kind : MachineRecipeKind.values()) {
            IBasicMachineRecipeManager manager = kind.manager();
            if (manager == null) {
                continue;
            }
            List<MachineRecipe<IRecipeInput, Collection<ItemStack>>> converted = new ArrayList<>();
            List<RecipeEntry<IC2MachineRecipe>> entries = new ArrayList<>(recipes.listAllOfType(kind.type()));
            entries.sort(Comparator.comparing(entry -> entry.id().toString()));
            for (RecipeEntry<IC2MachineRecipe> entry : entries) {
                IC2MachineRecipe recipe = entry.value();
                if (recipe.results.stream().anyMatch(Rezepte::isDisabledOutput)) {
                    continue;
                }
                List<ItemStack> outputs = recipe.results.stream().map(ItemStack::copy).toList();
                converted.add(new MachineRecipe<>(new RecipeInputIngredient(recipe.ingredient, recipe.count), outputs,
                        recipe.metadata.isEmpty() ? null : recipe.metadata.copy()));
            }

            if (manager instanceof BasicMachineRecipeManager basic) {
                basic.setDataRecipes(converted);
            } else {
                // a manager another mod put in place: it only learns of additions
                for (MachineRecipe<IRecipeInput, Collection<ItemStack>> recipe : converted) {
                    manager.addRecipe(recipe.getInput(), recipe.getOutput(), recipe.getMetaData(), false);
                }
            }
            total += converted.size();
        }
        LOG.info("[IC2] {} machine recipes from datapacks", total);
    }
}
