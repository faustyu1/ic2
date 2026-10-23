package ic2.core.uu;

import ic2.core.init.IC2ItemVariants;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/**
 * Port of {@code ic2.core.uu.ManualRecipeResolver}: transformations no recipe manager knows - a
 * fuel rod burns down to its depleted rod in the reactor, for free.
 */
class ManualRecipeResolver implements IRecipeResolver {
    private static final double transformCost = 0.0;

    @Override
    public List<RecipeTransformation> getTransformations() {
        List<RecipeTransformation> ret = new ArrayList<>();
        add(ret, "uranium_fuel_rod", "depleted_uranium");
        add(ret, "dual_uranium_fuel_rod", "depleted_dual_uranium");
        add(ret, "quad_uranium_fuel_rod", "depleted_quad_uranium");
        add(ret, "mox_fuel_rod", "depleted_mox");
        add(ret, "dual_mox_fuel_rod", "depleted_dual_mox");
        add(ret, "quad_mox_fuel_rod", "depleted_quad_mox");
        return ret;
    }

    private static void add(List<RecipeTransformation> ret, String rod, String depleted) {
        Identifier id = Identifier.of("ic2", rod);
        Item output = IC2ItemVariants.get("nuclear", depleted);
        if (!Registries.ITEM.containsId(id) || output == null) {
            return;
        }

        List<List<LeanItemStack>> inputs = Collections.singletonList(Collections.singletonList(new LeanItemStack(Registries.ITEM.get(id), 1)));
        ret.add(new RecipeTransformation(transformCost, inputs, new LeanItemStack(output, 1)));
    }
}
