package ic2.core.init;

import ic2.api.recipe.IMachineRecipeManager;
import ic2.api.recipe.IRecipeInput;
import ic2.api.recipe.Recipes;
import ic2.core.block.machine.tileentity.TileEntityAssemblyBench;
import ic2.core.recipe.BasicListRecipeManager;
import ic2.core.recipe.SmeltingRecipeManager;
import ic2.core.recipe.UuRecipe;
import ic2.core.util.Config;
import ic2.core.util.ConfigUtil;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric port of ic2.core.init.Rezepte - reads the recipe settings of general.ini (the disabled
 * outputs, the recycler lists) and the UU-matter configs.
 *
 * <p>The machine recipes it loaded from {@code config/*.ini} in 1.12.2 are datapack recipes now,
 * see {@link ic2.core.recipe.data.IC2MachineRecipe}; tools/gen_machine_recipes.py made them from
 * those configs.
 */
public final class Rezepte {
    private static final Logger LOG = LoggerFactory.getLogger("IC2-Recipes");

    /** Outputs the config disables, from {@code recipes/disable} in general.ini. */
    private static List<IRecipeInput> disabledOutputs = List.of();

    private Rezepte() {}

    /** Reads the recipe settings and the UU-matter recipes. */
    public static void loadRecipes() {
        loadDisabledOutputs();
        loadRecyclerLists();
        loadUuRecipes();
    }

    /**
     * Loads {@code uu_recipes.ini} into {@link TileEntityAssemblyBench#RECIPES}.
     *
     * <p>Format is the shaped-crafting one: {@code "<pattern>" <symbol>:<ingredient>... = <output>},
     * with UU-matter as the usual - and, for a pure UU recipe, only - ingredient.
     *
     * @return number of recipes loaded
     */
    public static int loadUuRecipes() {
        List<String> lines;
        try {
            lines = readConfig("uu_recipes");
        } catch (IOException e) {
            LOG.error("[IC2] Failed to read config/uu_recipes.ini", e);
            return 0;
        }

        int loaded = 0;
        for (String line : lines) {
            int sep = line.indexOf('=');
            if (sep < 0) {
                LOG.warn("[IC2] uu_recipes: no '=' in line: {}", line);
                continue;
            }

            String key = line.substring(0, sep).trim();
            String value = line.substring(sep + 1).trim();

            try {
                ItemStack output = ConfigUtil.asStackWithAmount(value);
                if (output == null) {
                    LOG.debug("[IC2] uu_recipes: skipping recipe with unresolvable output {}", value);
                    continue;
                }

                UuRecipe recipe = parseUuRecipe(key, output);
                if (recipe == null) {
                    continue;
                }
                TileEntityAssemblyBench.RECIPES.add(recipe);
                loaded++;
            } catch (Exception e) {
                LOG.warn("[IC2] uu_recipes: skipping {}: {}", line, e.getMessage());
            }
        }

        LOG.info("[IC2] Loaded {} of {} UU pattern recipes", loaded, lines.size());
        return loaded;
    }

    private static UuRecipe parseUuRecipe(String key, ItemStack output) throws ParseException {
        List<String> tokens = splitWhitespace(key);
        if (tokens.isEmpty()) {
            return null;
        }

        String patternToken = tokens.get(0);
        if (patternToken.startsWith("\"") && patternToken.endsWith("\"")) {
            patternToken = patternToken.substring(1, patternToken.length() - 1);
        }

        List<String> rows = new ArrayList<>(Arrays.asList(patternToken.split("\\|", -1)));
        int width = 0;
        for (String row : rows) {
            width = Math.max(width, row.length());
        }
        for (int i = 0; i < rows.size(); i++) {
            rows.set(i, padRight(rows.get(i), width));
        }

        Map<Character, IRecipeInput> inputs = new LinkedHashMap<>();
        boolean uuOnly = true;
        for (String token : tokens.subList(1, tokens.size())) {
            if (token.length() < 3 || token.charAt(1) != ':') {
                LOG.warn("[IC2] uu_recipes: no pattern index character in {}", token);
                return null;
            }

            IRecipeInput input = ConfigUtil.asRecipeInput(token.substring(2));
            if (input == null) {
                LOG.debug("[IC2] uu_recipes: skipping recipe with unresolvable ingredient {}", token);
                return null;
            }
            if (!isUuMatter(input)) {
                uuOnly = false;
            }
            inputs.put(token.charAt(0), input);
        }

        for (String row : rows) {
            for (int i = 0; i < row.length(); i++) {
                char c = row.charAt(i);
                if (c != ' ' && !inputs.containsKey(c)) {
                    LOG.warn("[IC2] uu_recipes: pattern uses undefined symbol {}", c);
                    return null;
                }
            }
        }

        return new UuRecipe(rows, inputs, output, uuOnly);
    }

    private static boolean isUuMatter(IRecipeInput input) {
        Item matter = IC2ItemVariants.get("misc_resource", "matter");
        for (ItemStack stack : input.getInputs()) {
            if (stack.getItem() == matter) {
                return true;
            }
        }
        return false;
    }

    private static String padRight(String str, int width) {
        StringBuilder ret = new StringBuilder(str);
        while (ret.length() < width) {
            ret.append(' ');
        }
        return ret.toString();
    }

    /**
     * Reads the outputs the config disables. The original also removed them from the crafting
     * recipes; those are datapack files now, so a disabled crafting output has to be removed with a
     * datapack instead.
     */
    private static void loadDisabledOutputs() {
        Config config = MainConfig.get();
        List<IRecipeInput> disabled = new ArrayList<>(ConfigUtil.asRecipeInputList(config, "recipes/disable"));

        if (!config.getBool("recipes/allowCoinCrafting", true)) {
            Item coin = IC2ItemVariants.get("crafting", "coin");
            if (coin != null) {
                disabled.add(Recipes.inputFactory.forStack(new ItemStack(coin)));
            }
        }

        disabledOutputs = disabled;
        if (!disabled.isEmpty()) {
            LOG.info("[IC2] {} recipe outputs disabled by config", disabled.size());
        }
    }

    /** Fills the recycler black/whitelist from the balance section of general.ini. */
    private static void loadRecyclerLists() {
        Config config = MainConfig.get();

        if (Recipes.recyclerBlacklist == null) {
            Recipes.recyclerBlacklist = new BasicListRecipeManager();
        }
        if (Recipes.recyclerWhitelist == null) {
            Recipes.recyclerWhitelist = new BasicListRecipeManager();
        }

        for (IRecipeInput input : ConfigUtil.asRecipeInputList(config, "balance/recyclerBlacklist")) {
            Recipes.recyclerBlacklist.add(input);
        }
        for (IRecipeInput input : ConfigUtil.asRecipeInputList(config, "balance/recyclerWhitelist")) {
            Recipes.recyclerWhitelist.add(input);
        }
    }

    /** Whether the config disables recipes making this item, from {@code recipes/disable}. */
    public static boolean isDisabledOutput(ItemStack output) {
        for (IRecipeInput disabled : disabledOutputs) {
            if (disabled.matches(output)) {
                return true;
            }
        }
        return false;
    }

    /** Reads a config file, dropping comments, section headers and blank lines. */
    private static List<String> readConfig(String name) throws IOException {
        String path = "/assets/ic2/config/" + name + ".ini";
        List<String> ret = new ArrayList<>();

        try (InputStream is = Rezepte.class.getResourceAsStream(path)) {
            if (is == null) {
                throw new IOException("Resource not found: " + path);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    int comment = line.indexOf(';');
                    if (comment >= 0) {
                        line = line.substring(0, comment);
                    }
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("[")) {
                        continue;
                    }
                    ret.add(line);
                }
            }
        }

        return ret;
    }

    /** Splits on whitespace outside of double quotes, as the original crafting patterns require. */
    public static List<String> splitWhitespace(String str) {
        List<String> ret = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '"') {
                quoted = !quoted;
            }

            if (!quoted && Character.isWhitespace(c)) {
                if (current.length() > 0) {
                    ret.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            ret.add(current.toString());
        }

        return ret;
    }

    /**
     * Loads {@code uu_scan_values.ini}, the mass-fabricator scan values.
     *
     * @return item reference to UU-matter cost in UU units, keyed by the raw config name
     */
    public static Map<String, Double> loadScanValues() {
        Map<String, Double> ret = new HashMap<>();
        List<String> lines;
        try {
            lines = readConfig("uu_scan_values");
        } catch (IOException e) {
            LOG.error("[IC2] Failed to read config/uu_scan_values.ini", e);
            return ret;
        }

        for (String line : lines) {
            int sep = line.indexOf('=');
            if (sep < 0) {
                LOG.warn("[IC2] uu_scan_values: no '=' in line: {}", line);
                continue;
            }
            String key = line.substring(0, sep).trim();
            try {
                ret.put(key, Double.parseDouble(line.substring(sep + 1).trim()));
            } catch (NumberFormatException e) {
                LOG.warn("[IC2] uu_scan_values: malformed value in line: {}", line);
            }
        }

        LOG.info("[IC2] Loaded {} UU scan values", ret.size());
        return ret;
    }
}
