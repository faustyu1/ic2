package ic2.core.recipe;

import ic2.api.recipe.MachineRecipe;
import ic2.api.recipe.MachineRecipeResult;
import ic2.core.init.MainConfig;
import ic2.core.util.ConfigUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;

/**
 * Port addition: makes IC2's machines output another mod's item of the same material, the
 * {@code recipes/preferredOutputMods} setting.
 *
 * <p>Two items are the same material when they share a {@code c:} material tag such as
 * {@code c:ingots/tin} or {@code c:dusts/lead}. Only IC2's own items are swapped, only plain stacks
 * without data, and only for a mod ranked above {@code ic2} in the setting - so the default
 * {@code ic2} changes nothing. The choice per item is cached and worked out again whenever the
 * tags are (re)loaded.
 */
public final class OutputUnifier {
    /** The {@code c:<dir>/<material>} tags that name a material in one form. */
    private static final Set<String> MATERIAL_DIRS = Set.of(
            "ingots", "nuggets", "dusts", "plates", "gems", "storage_blocks", "ores", "raw_materials");
    private static final String IC2 = "ic2";

    private static final Map<Item, Item> CACHE = new ConcurrentHashMap<>();
    private static volatile List<String> preferred;

    private OutputUnifier() {
    }

    /** Forgets the choices when the tags change, which is also when other mods' items join them. */
    public static void register() {
        try {
            net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents.TAGS_LOADED.register((registries, client) -> CACHE.clear());
        } catch (Throwable t) {
            // unit tests run without a Fabric loader context
        }
    }

    /** The mods of the setting, highest priority first. */
    static List<String> preferred() {
        List<String> ret = preferred;
        if (ret == null) {
            ret = new ArrayList<>();
            for (String mod : ConfigUtil.asList(ConfigUtil.getString(MainConfig.get(), "recipes/preferredOutputMods"))) {
                ret.add(mod.toLowerCase(Locale.ROOT));
            }
            preferred = ret;
        }
        return ret;
    }

    /** Sets the mods, highest priority first; for tests, the setting is read once otherwise. */
    public static void setPreferred(List<String> mods) {
        preferred = List.copyOf(mods);
        CACHE.clear();
    }

    /** Whether any output can change: some mod is ranked above IC2. */
    public static boolean isActive() {
        List<String> mods = preferred();
        return !mods.isEmpty() && !IC2.equals(mods.get(0));
    }

    public static ItemStack unify(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !isActive() || !stack.getComponentChanges().isEmpty()) {
            return stack;
        }
        Item to = CACHE.computeIfAbsent(stack.getItem(), OutputUnifier::preferredFor);
        return to == stack.getItem() ? stack : new ItemStack(to, stack.getCount());
    }

    /** The outputs with every swappable stack swapped; the same collection when none is. */
    public static Collection<ItemStack> unify(Collection<ItemStack> stacks) {
        if (stacks == null || !isActive()) {
            return stacks;
        }
        List<ItemStack> ret = new ArrayList<>(stacks.size());
        boolean changed = false;
        for (ItemStack stack : stacks) {
            ItemStack unified = unify(stack);
            changed |= unified != stack;
            ret.add(unified);
        }
        return changed ? ret : stacks;
    }

    /** A recipe result with its outputs swapped, or the result itself when nothing changes. */
    public static <RI, I> MachineRecipeResult<RI, Collection<ItemStack>, I> unifyResult(MachineRecipeResult<RI, Collection<ItemStack>, I> result) {
        if (result == null || !isActive()) {
            return result;
        }
        MachineRecipe<RI, Collection<ItemStack>> recipe = result.getRecipe();
        Collection<ItemStack> outputs = unify(recipe.getOutput());
        return outputs == recipe.getOutput() ? result
                : new MachineRecipeResult<>(new MachineRecipe<>(recipe.getInput(), outputs, recipe.getMetaData()), result.getAdjustedInput());
    }

    /** Same for a single-output recipe, the furnaces'. */
    public static <RI, I> MachineRecipeResult<RI, ItemStack, I> unifySingle(MachineRecipeResult<RI, ItemStack, I> result) {
        if (result == null || !isActive()) {
            return result;
        }
        MachineRecipe<RI, ItemStack> recipe = result.getRecipe();
        ItemStack output = unify(recipe.getOutput());
        return output == recipe.getOutput() ? result
                : new MachineRecipeResult<>(new MachineRecipe<>(recipe.getInput(), output, recipe.getMetaData()), result.getAdjustedInput());
    }

    private static Item preferredFor(Item item) {
        if (!IC2.equals(Registries.ITEM.getId(item).getNamespace())) {
            return item;
        }

        RegistryEntry<Item> entry = Registries.ITEM.getEntry(item);
        for (TagKey<Item> tag : entry.streamTags().toList()) {
            if (!isMaterialTag(tag)) {
                continue;
            }
            Item best = item;
            int bestRank = rank(IC2);
            for (RegistryEntry<Item> other : Registries.ITEM.iterateEntries(tag)) {
                int rank = rank(Registries.ITEM.getId(other.value()).getNamespace());
                if (rank < bestRank) {
                    best = other.value();
                    bestRank = rank;
                }
            }
            if (best != item) {
                return best;
            }
        }
        return item;
    }

    private static boolean isMaterialTag(TagKey<Item> tag) {
        if (!"c".equals(tag.id().getNamespace())) {
            return false;
        }
        String path = tag.id().getPath();
        int slash = path.indexOf('/');
        return slash > 0 && path.indexOf('/', slash + 1) < 0 && MATERIAL_DIRS.contains(path.substring(0, slash));
    }

    /** The position in the setting; a mod not in it ranks below all that are. */
    private static int rank(String namespace) {
        int index = preferred().indexOf(namespace);
        return index < 0 ? Integer.MAX_VALUE : index;
    }
}
