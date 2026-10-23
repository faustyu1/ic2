package ic2.core.recipe;

import ic2.api.recipe.IBasicMachineRecipeManager;
import ic2.api.recipe.IMachineRecipeManager;
import ic2.api.recipe.IRecipeInput;
import ic2.api.recipe.MachineRecipe;
import ic2.api.recipe.MachineRecipeResult;
import ic2.api.recipe.RecipeOutput;
import ic2.api.recipe.Recipes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

/**
 * Fabric port of ic2.core.recipe.SmeltingRecipeManager.
 *
 * <p>The 1.12.2 original wrote straight into the global mutable {@code FurnaceRecipes} table. Since
 * 1.13 smelting recipes come from datapacks and the vanilla recipe manager is immutable at runtime,
 * so the IC2 smelting recipes from {@code config/furnace.ini} are kept in this manager instead. The
 * externally visible behaviour - input to output plus an {@code experience} metadata tag - is
 * unchanged.
 */
public class SmeltingRecipeManager implements IMachineRecipeManager<ItemStack, ItemStack, ItemStack> {
    private final Map<Item, MachineRecipe<ItemStack, ItemStack>> recipes = new LinkedHashMap<>();

    @Override
    public boolean addRecipe(ItemStack input, ItemStack output, NbtCompound metadata, boolean replace) {
        if (input == null || input.isEmpty() || output == null || output.isEmpty()) {
            return false;
        }
        if (recipes.containsKey(input.getItem()) && !replace) {
            return false;
        }

        float experience = metadata != null && metadata.contains("experience") ? metadata.getFloat("experience") : 0.0F;
        if (experience < 0.0F) {
            throw new IllegalArgumentException("Negative xp for " + input + " -> " + output);
        }

        NbtCompound meta = new NbtCompound();
        meta.putFloat("experience", experience);
        recipes.put(input.getItem(), new MachineRecipe<>(input, output, meta));
        return true;
    }

    @Override
    public MachineRecipeResult<ItemStack, ItemStack, ItemStack> apply(ItemStack input, boolean adjustInput) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        MachineRecipe<ItemStack, ItemStack> recipe = recipes.get(input.getItem());
        if (recipe == null) {
            recipe = vanillaRecipe(input);
            if (recipe == null) {
                return null;
            }
        }

        ItemStack adjusted = input.copy();
        if (adjustInput) {
            adjusted.decrement(1);
        }
        return OutputUnifier.unifySingle(recipe.getResult(adjusted));
    }

    /**
     * The vanilla furnace recipe for the item. In 1.12.2 IC2 put its own recipes into the global
     * FurnaceRecipes and its furnaces read that table, so they smelted everything a furnace
     * smelts; the port keeps its recipes here and falls back to the server's smelting recipes.
     */
    private static MachineRecipe<ItemStack, ItemStack> vanillaRecipe(ItemStack input) {
        net.minecraft.server.MinecraftServer server = ic2.core.IC2.server;
        if (server == null) {
            return null;
        }
        return server.getRecipeManager()
                .getFirstMatch(net.minecraft.recipe.RecipeType.SMELTING, new net.minecraft.recipe.input.SingleStackRecipeInput(input), server.getOverworld())
                .map(entry -> {
                    NbtCompound meta = new NbtCompound();
                    meta.putFloat("experience", entry.value().getExperience());
                    ItemStack output = entry.value().getResult(server.getRegistryManager()).copy();
                    return output.isEmpty() ? null : new MachineRecipe<>(input.copyWithCount(1), output, meta);
                })
                .orElse(null);
    }

    @Override
    public Iterable<? extends MachineRecipe<ItemStack, ItemStack>> getRecipes() {
        return recipes.values();
    }

    @Override
    public boolean isIterable() {
        return true;
    }

    /**
     * Adapter presenting {@link Recipes#furnace} through the interface the config loader and the
     * furnace machines use, exactly as the original enum singleton did.
     */
    public enum SmeltingBridge implements IBasicMachineRecipeManager {
        INSTANCE;

        @Override
        public boolean addRecipe(IRecipeInput input, Collection<ItemStack> output, NbtCompound metadata, boolean replace) {
            if (output.size() != 1) {
                throw new IllegalArgumentException("smelting takes exactly one output, got " + output.size());
            }
            ItemStack realOutput = output.iterator().next();
            boolean ret = false;

            for (ItemStack stack : input.getInputs()) {
                ret |= Recipes.furnace.addRecipe(stack, realOutput, metadata, replace);
            }

            return ret;
        }

        @Override
        public boolean addRecipe(IRecipeInput input, NbtCompound metadata, boolean replace, ItemStack... outputs) {
            return addRecipe(input, java.util.Arrays.asList(outputs), metadata, replace);
        }

        @Override
        public MachineRecipeResult<IRecipeInput, Collection<ItemStack>, ItemStack> apply(ItemStack input, boolean adjustInput) {
            MachineRecipeResult<ItemStack, ItemStack, ItemStack> normal = Recipes.furnace.apply(input, adjustInput);
            if (normal == null) {
                return null;
            }

            MachineRecipe<ItemStack, ItemStack> result = normal.getRecipe();
            IRecipeInput resultIn = Recipes.inputFactory.forStack(result.getInput());
            Collection<ItemStack> resultOut = Collections.singletonList(result.getOutput());
            return new MachineRecipe<>(resultIn, resultOut, result.getMetaData()).getResult(normal.getAdjustedInput());
        }

        @Override
        public Iterable<? extends MachineRecipe<IRecipeInput, Collection<ItemStack>>> getRecipes() {
            Collection<MachineRecipe<IRecipeInput, Collection<ItemStack>>> ret = new ArrayList<>();
            for (MachineRecipe<ItemStack, ItemStack> recipe : Recipes.furnace.getRecipes()) {
                ret.add(new MachineRecipe<>(
                        Recipes.inputFactory.forStack(recipe.getInput()),
                        Collections.singletonList(recipe.getOutput()),
                        recipe.getMetaData()));
            }
            return ret;
        }

        @Override
        public boolean isIterable() {
            return Recipes.furnace.isIterable();
        }

        @Override
        @Deprecated
        public RecipeOutput getOutputFor(ItemStack input, boolean adjustInput) {
            MachineRecipeResult<IRecipeInput, Collection<ItemStack>, ItemStack> result = apply(input, adjustInput);
            if (result == null) {
                return null;
            }
            return new RecipeOutput(result.getRecipe().getMetaData(), new ArrayList<>(result.getOutput()));
        }
    }
}
