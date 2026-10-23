package ic2.core.recipe;

import ic2.api.recipe.IRecipeInput;
import ic2.api.recipe.IScrapboxManager;
import ic2.api.recipe.MachineRecipe;
import ic2.api.recipe.MachineRecipeResult;
import ic2.api.recipe.RecipeOutput;
import ic2.api.recipe.Recipes;
import ic2.core.IC2;
import ic2.core.init.IC2ItemVariants;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/**
 * Fabric port of ScrapboxRecipeManager - what comes out of a scrap box.
 *
 * <p>Every drop has a weight; a box gives one drop, picked with a chance of its weight over the
 * sum of all weights. The table is the 1.12.2 one: mostly dirt, sticks, wooden tools and food,
 * now and then dusts, ores or a battery, and rarely a diamond (0.1) or an emerald (0.05).
 */
public final class ScrapboxRecipeManager implements IScrapboxManager {
    private final List<Drop> drops = new ArrayList<>();
    /** Sum of all weights so far, the upper bound of the last drop. */
    private float topChance;

    private ScrapboxRecipeManager() {
    }

    /** Installs the manager with the built-in table; it needs the items registered. */
    public static void init() {
        if (Recipes.scrapboxDrops == null) {
            ScrapboxRecipeManager manager = new ScrapboxRecipeManager();
            manager.addBuiltinDrops();
            Recipes.scrapboxDrops = manager;
        }
    }

    private static ItemStack scrapBox() {
        Item box = IC2ItemVariants.get("crafting", "scrap_box");
        return box == null ? ItemStack.EMPTY : new ItemStack(box);
    }

    private static boolean isScrapBox(ItemStack stack) {
        Item box = IC2ItemVariants.get("crafting", "scrap_box");
        return stack != null && !stack.isEmpty() && box != null && stack.isOf(box);
    }

    @Override
    public boolean addRecipe(IRecipeInput input, Collection<ItemStack> output, NbtCompound metadata, boolean replace) {
        if (!input.matches(scrapBox())) {
            throw new IllegalArgumentException("currently only scrap boxes are supported");
        }
        if (metadata == null || !metadata.contains("weight")) {
            throw new IllegalArgumentException("no weight metadata");
        }
        if (output.size() != 1) {
            throw new IllegalArgumentException("currently only a single drop stack is supported");
        }
        float weight = metadata.getFloat("weight");
        if (weight <= 0.0F || Float.isInfinite(weight) || Float.isNaN(weight)) {
            throw new IllegalArgumentException("invalid weight");
        }
        addDrop(output.iterator().next(), weight);
        return true;
    }

    @Override
    public boolean addRecipe(IRecipeInput input, NbtCompound metadata, boolean replace, ItemStack... outputs) {
        return addRecipe(input, Arrays.asList(outputs), metadata, replace);
    }

    @Override
    public MachineRecipeResult<IRecipeInput, Collection<ItemStack>, ItemStack> apply(ItemStack input, boolean acceptTest) {
        if (!isScrapBox(input) || drops.isEmpty()) {
            return null;
        }

        // binary search for the first drop whose upper bound lies above the roll
        float chance = IC2.random.nextFloat() * topChance;
        int low = 0;
        int high = drops.size() - 1;
        while (low < high) {
            int mid = (high + low) / 2;
            if (chance < drops.get(mid).upperChanceBound) {
                high = mid;
            } else {
                low = mid + 1;
            }
        }

        ItemStack drop = drops.get(low).item.copy();
        return new MachineRecipe<IRecipeInput, Collection<ItemStack>>(Recipes.inputFactory.forStack(scrapBox()),
                Collections.singletonList(drop)).getResult(input.copyWithCount(input.getCount() - 1));
    }

    @Override
    public RecipeOutput getOutputFor(ItemStack input, boolean adjustInput) {
        MachineRecipeResult<IRecipeInput, Collection<ItemStack>, ItemStack> result = apply(input, false);
        return result != null && !result.getOutput().isEmpty() ? new RecipeOutput(null, new ArrayList<>(result.getOutput())) : null;
    }

    @Override
    public Iterable<? extends MachineRecipe<IRecipeInput, Collection<ItemStack>>> getRecipes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isIterable() {
        return false;
    }

    @Override
    public void addDrop(ItemStack drop, float rawChance) {
        topChance += rawChance;
        drops.add(new Drop(drop, rawChance, topChance));
    }

    /**
     * Rolls a drop for the box.
     *
     * @param adjustInput whether to take the box out of the stack
     * @return the drop, or null when the stack is no scrap box
     */
    @Override
    public ItemStack getDrop(ItemStack input, boolean adjustInput) {
        MachineRecipeResult<IRecipeInput, Collection<ItemStack>, ItemStack> result = apply(input, false);
        if (result == null || result.getOutput().isEmpty()) {
            return null;
        }
        if (adjustInput) {
            input.setCount(result.getAdjustedInput().getCount());
        }
        return result.getOutput().iterator().next();
    }

    /** Every drop with its chance, 0 to 1. */
    @Override
    public Map<ItemStack, Float> getDrops() {
        Map<ItemStack, Float> ret = new HashMap<>(drops.size());
        for (Drop drop : drops) {
            ret.put(drop.item, drop.originalChance / topChance);
        }
        return ret;
    }

    private void addBuiltinDrops() {
        addDrop(Items.WOODEN_HOE, 5.01F);
        addDrop(Items.DIRT, 5.0F);
        addDrop(Items.STICK, 4.0F);
        addDrop(Items.GRASS_BLOCK, 3.0F);
        addDrop(Items.GRAVEL, 3.0F);
        addDrop(Items.NETHERRACK, 2.0F);
        addDrop(Items.ROTTEN_FLESH, 2.0F);
        addDrop(Items.APPLE, 1.5F);
        addDrop(Items.BREAD, 1.5F);
        addDrop(ic2("filled_tin_can"), 1.5F);
        addDrop(Items.WOODEN_SWORD, 1.0F);
        addDrop(Items.WOODEN_SHOVEL, 1.0F);
        addDrop(Items.WOODEN_PICKAXE, 1.0F);
        addDrop(Items.SOUL_SAND, 1.0F);
        addDrop(Items.OAK_SIGN, 1.0F);
        addDrop(Items.LEATHER, 1.0F);
        addDrop(Items.FEATHER, 1.0F);
        addDrop(Items.BONE, 1.0F);
        addDrop(Items.COOKED_PORKCHOP, 0.9F);
        addDrop(Items.COOKED_BEEF, 0.9F);
        addDrop(Items.PUMPKIN, 0.9F);
        addDrop(Items.COOKED_CHICKEN, 0.9F);
        addDrop(Items.MINECART, 0.01F);
        addDrop(Items.REDSTONE, 0.9F);
        addDrop(IC2ItemVariants.get("crafting", "rubber"), 0.8F);
        addDrop(Items.GLOWSTONE_DUST, 0.8F);
        addDrop(IC2ItemVariants.get("dust", "coal"), 0.8F);
        addDrop(IC2ItemVariants.get("dust", "copper"), 0.8F);
        addDrop(IC2ItemVariants.get("dust", "tin"), 0.8F);
        addDrop(ic2("single_use_battery"), 0.7F);
        addDrop(IC2ItemVariants.get("dust", "iron"), 0.7F);
        addDrop(IC2ItemVariants.get("dust", "gold"), 0.7F);
        addDrop(Items.SLIME_BALL, 0.6F);
        addDrop(Items.IRON_ORE, 0.5F);
        addDrop(Items.GOLDEN_HELMET, 0.01F);
        addDrop(Items.GOLD_ORE, 0.5F);
        addDrop(Items.CAKE, 0.5F);
        addDrop(Items.DIAMOND, 0.1F);
        addDrop(Items.EMERALD, 0.05F);
        addDrop(Items.ENDER_PEARL, 0.08F);
        addDrop(Items.BLAZE_ROD, 0.04F);
        addDrop(Items.EGG, 0.8F);
        addDrop(Items.COPPER_ORE, 0.7F); // IC2 had its own copper ore, Minecraft has had one since 1.17
        addDrop(ic2("tin_ore"), 0.7F);
    }

    private static Item ic2(String name) {
        return Registries.ITEM.get(Identifier.of("ic2", name));
    }

    private void addDrop(ItemConvertible item, float rawChance) {
        // a missing item keeps its share of the odds as an empty roll rather than shifting the rest
        addDrop(item == null || item.asItem() == Items.AIR ? ItemStack.EMPTY : new ItemStack(item), rawChance);
    }

    private record Drop(ItemStack item, float originalChance, float upperChanceBound) {
    }
}
