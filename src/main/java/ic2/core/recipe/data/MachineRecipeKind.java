package ic2.core.recipe.data;

import ic2.api.recipe.IBasicMachineRecipeManager;
import ic2.api.recipe.Recipes;
import java.util.function.Supplier;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * The machines whose recipes come from datapacks, each with its own recipe type
 * ({@code ic2:macerator} and so on) and the recipe manager in {@link Recipes} it fills.
 */
public enum MachineRecipeKind {
    MACERATOR("macerator", () -> Recipes.macerator),
    COMPRESSOR("compressor", () -> Recipes.compressor),
    EXTRACTOR("extractor", () -> Recipes.extractor),
    ORE_WASHER("ore_washer", () -> Recipes.oreWashing),
    THERMAL_CENTRIFUGE("thermal_centrifuge", () -> Recipes.centrifuge),
    BLAST_FURNACE("blast_furnace", () -> Recipes.blastfurnace),
    BLOCK_CUTTER("block_cutter", () -> Recipes.blockcutter),
    METAL_FORMER_CUTTING("metal_former_cutting", () -> Recipes.metalformerCutting),
    METAL_FORMER_EXTRUDING("metal_former_extruding", () -> Recipes.metalformerExtruding),
    METAL_FORMER_ROLLING("metal_former_rolling", () -> Recipes.metalformerRolling);

    public final String name;
    private final Supplier<IBasicMachineRecipeManager> manager;
    private RecipeType<IC2MachineRecipe> type;
    private RecipeSerializer<IC2MachineRecipe> serializer;

    MachineRecipeKind(String name, Supplier<IBasicMachineRecipeManager> manager) {
        this.name = name;
        this.manager = manager;
    }

    public RecipeType<IC2MachineRecipe> type() {
        return type;
    }

    public RecipeSerializer<IC2MachineRecipe> serializer() {
        return serializer;
    }

    public IBasicMachineRecipeManager manager() {
        return manager.get();
    }

    public static void register() {
        for (MachineRecipeKind kind : values()) {
            Identifier id = Identifier.of("ic2", kind.name);
            kind.type = Registry.register(Registries.RECIPE_TYPE, id, new RecipeType<>() {
                @Override
                public String toString() {
                    return id.toString();
                }
            });
            kind.serializer = Registry.register(Registries.RECIPE_SERIALIZER, id, new IC2MachineRecipe.Serializer(kind));
        }
    }
}
