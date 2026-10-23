package ic2.core.uu;

import ic2.api.recipe.IMachineRecipeManager;
import ic2.api.recipe.IRecipeInput;
import ic2.api.recipe.MachineRecipe;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import net.minecraft.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Port of {@code ic2.core.uu.MachineRecipeResolver}: the recipes of one IC2 processing machine
 * (macerator, extractor, compressor, centrifuge, block cutter, blast furnace, metal former, ore
 * washing plant), at 14 UU per operation.
 */
class MachineRecipeResolver implements IRecipeResolver {
    private static final Logger LOG = LoggerFactory.getLogger("IC2-UU");
    private static final double transformCost = 14.0;
    private final IMachineRecipeManager<IRecipeInput, Collection<ItemStack>, ?> manager;

    MachineRecipeResolver(IMachineRecipeManager<IRecipeInput, Collection<ItemStack>, ?> manager) {
        this.manager = manager;
    }

    @Override
    public List<RecipeTransformation> getTransformations() {
        if (manager == null || !manager.isIterable()) {
            return Collections.emptyList();
        }

        List<RecipeTransformation> ret = new ArrayList<>();

        for (MachineRecipe<IRecipeInput, Collection<ItemStack>> recipe : manager.getRecipes()) {
            try {
                List<List<LeanItemStack>> inputs = RecipeUtil.convertIngredients(recipe.getInput().getInputs());
                List<LeanItemStack> outputs = RecipeUtil.convertOutputs(recipe.getOutput());
                if (!outputs.isEmpty()) {
                    ret.add(new RecipeTransformation(transformCost, inputs, outputs));
                }
            } catch (IllegalArgumentException e) {
                LOG.warn("[IC2] invalid recipe {}", recipe.getInput(), e);
            }
        }

        return ret;
    }
}
