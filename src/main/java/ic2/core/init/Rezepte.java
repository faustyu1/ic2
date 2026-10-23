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
 * Fabric port of ic2.core.init.Rezepte - loads the machine recipes from
 * {@code assets/ic2/config/*.ini}.
 *
 * <p>The file format is untouched, so the recipe values stay identical to 1.12.2:
 * <pre>
 *   &lt;input&gt; = &lt;output 1&gt;[ &lt;output n&gt;...] [&#64;attribute:value...]
 * </pre>
 *
 * <p>Which attributes are accepted depends on the machine, exactly as in the original: the block
 * cutter reads {@code @hardness}, the thermal centrifuge {@code @heat} (stored as {@code minHeat}),
 * the ore washing plant {@code @fluid} (stored as {@code amount}), the blast furnace
 * {@code @fluid} and {@code @duration}, and the furnace {@code @xp} (stored as
 * {@code experience}). {@code @ignoreSameInputOutput} is accepted for every machine.
 *
 * <p>Unlike the original there is no deferred second pass for unresolvable entries: on Fabric all
 * items exist by the time recipes load, so an unresolvable line is reported once and skipped.
 */
public final class Rezepte {
    private static final Logger LOG = LoggerFactory.getLogger("IC2-Recipes");

    /** Machine kinds, each with the recipe attributes it understands. */
    public enum MachineType {
        Normal,
        Furnace,
        BlockCutter,
        ThermalCentrifuge,
        OreWashingPlant,
        BlastFurnace
    }

    /** Outputs the config disables, from {@code recipes/disable} in general.ini. */
    private static List<IRecipeInput> disabledOutputs = List.of();

    private Rezepte() {}

    /**
     * Loads every machine recipe config and feeds it into the matching manager in
     * {@link Recipes}. Managers that have not been created yet are skipped with a warning.
     */
    public static void loadRecipes() {
        loadDisabledOutputs();
        loadRecyclerLists();
        load("macerator", Recipes.macerator, MachineType.Normal);
        load("compressor", Recipes.compressor, MachineType.Normal);
        load("extractor", Recipes.extractor, MachineType.Normal);
        load("furnace", SmeltingRecipeManager.SmeltingBridge.INSTANCE, MachineType.Furnace);
        load("ore_washer", Recipes.oreWashing, MachineType.OreWashingPlant);
        load("thermal_centrifuge", Recipes.centrifuge, MachineType.ThermalCentrifuge);
        load("blast_furnace", Recipes.blastfurnace, MachineType.BlastFurnace);
        load("block_cutter", Recipes.blockcutter, MachineType.BlockCutter);
        load("block_cutter_late", Recipes.blockcutter, MachineType.BlockCutter);
        load("metal_former_cutting", Recipes.metalformerCutting, MachineType.Normal);
        load("metal_former_extruding", Recipes.metalformerExtruding, MachineType.Normal);
        load("metal_former_rolling", Recipes.metalformerRolling, MachineType.Normal);
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

    /**
     * Loads one recipe config.
     *
     * @param name    config base name, e.g. {@code macerator} for {@code config/macerator.ini}
     * @param machine target recipe manager, may be null if that machine is not wired up yet
     * @param type    machine kind, deciding which recipe attributes are accepted
     * @return number of recipes added
     */
    public static int load(String name, IMachineRecipeManager<IRecipeInput, Collection<ItemStack>, ?> machine, MachineType type) {
        if (machine == null) {
            LOG.warn("[IC2] No recipe manager for {}, skipping its recipes", name);
            return 0;
        }

        List<String> lines;
        try {
            lines = readConfig(name);
        } catch (IOException e) {
            LOG.error("[IC2] Failed to read config/{}.ini", name, e);
            return 0;
        }

        int total = 0;
        int loaded = 0;
        for (String line : lines) {
            total++;
            if (loadRecipe(name, line, machine, type)) {
                loaded++;
            }
        }

        LOG.info("[IC2] Loaded {} of {} recipes for {}", loaded, total, name);
        return loaded;
    }

    private static boolean loadRecipe(String config, String line,
                                      IMachineRecipeManager<IRecipeInput, Collection<ItemStack>, ?> machine,
                                      MachineType type) {
        int sep = line.indexOf('=');
        if (sep < 0) {
            LOG.warn("[IC2] {}: no '=' in recipe line: {}", config, line);
            return false;
        }

        String key = line.substring(0, sep).trim();
        String value = line.substring(sep + 1).trim();

        IRecipeInput input;
        try {
            input = ConfigUtil.asRecipeInputWithAmount(key);
        } catch (Exception e) {
            LOG.warn("[IC2] {}: unparsable input {}: {}", config, key, e.getMessage());
            return false;
        }
        if (input == null) {
            LOG.debug("[IC2] {}: skipping recipe with unresolvable input {}", config, key);
            return false;
        }

        List<ItemStack> outputs = new ArrayList<>();
        NbtCompound metadata = new NbtCompound();

        for (String part : splitWhitespace(value)) {
            if (!part.startsWith("@")) {
                ItemStack output;
                try {
                    output = ConfigUtil.asStackWithAmount(part);
                } catch (Exception e) {
                    LOG.warn("[IC2] {}: unparsable output {}: {}", config, part, e.getMessage());
                    return false;
                }
                if (output == null) {
                    LOG.debug("[IC2] {}: skipping recipe {} with unresolvable output {}", config, key, part);
                    return false;
                }
                outputs.add(output);
            } else if (!readAttribute(config, key, part, metadata, type)) {
                return false;
            }
        }

        if (outputs.isEmpty()) {
            LOG.warn("[IC2] {}: recipe {} has no outputs", config, key);
            return false;
        }

        for (IRecipeInput disabled : disabledOutputs) {
            for (ItemStack output : outputs) {
                if (disabled.matches(output)) {
                    return false;
                }
            }
        }

        return machine.addRecipe(input, outputs, metadata.isEmpty() ? null : metadata, false);
    }

    /** Parses one {@code @attribute:value} token into the recipe metadata. */
    private static boolean readAttribute(String config, String key, String part, NbtCompound metadata, MachineType type) {
        try {
            if (part.startsWith("@ignoreSameInputOutput")) {
                metadata.putBoolean("ignoreSameInputOutput", true);
                return true;
            }
            if (part.startsWith("@xp:") && type == MachineType.Furnace) {
                metadata.putFloat("experience", Float.parseFloat(part.substring(4)));
                return true;
            }
            if (part.startsWith("@hardness:") && type == MachineType.BlockCutter) {
                metadata.putInt("hardness", Integer.parseInt(part.substring(10)));
                return true;
            }
            if (part.startsWith("@heat:") && type == MachineType.ThermalCentrifuge) {
                metadata.putInt("minHeat", Integer.parseInt(part.substring(6)));
                return true;
            }
            if (part.startsWith("@fluid:") && type == MachineType.OreWashingPlant) {
                metadata.putInt("amount", Integer.parseInt(part.substring(7)));
                return true;
            }
            if (part.startsWith("@fluid:") && type == MachineType.BlastFurnace) {
                metadata.putInt("fluid", Integer.parseInt(part.substring(7)));
                return true;
            }
            if (part.startsWith("@duration:") && type == MachineType.BlastFurnace) {
                metadata.putInt("duration", Integer.parseInt(part.substring(10)));
                return true;
            }
        } catch (NumberFormatException e) {
            LOG.warn("[IC2] {}: malformed attribute {} on recipe {}", config, part, key);
            return false;
        }

        LOG.warn("[IC2] {}: invalid attribute {} for {} recipe {}", config, part, type, key);
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
