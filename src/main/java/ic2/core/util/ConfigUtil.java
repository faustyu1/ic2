package ic2.core.util;

import ic2.api.recipe.IRecipeInput;
import ic2.api.recipe.Recipes;
import ic2.core.init.IC2ItemVariants;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/**
 * Fabric port of ic2.core.util.ConfigUtil - parses the item references used in
 * {@code assets/ic2/config/*.ini}.
 *
 * <p>Reference grammar, unchanged from 1.12.2 so the shipped config files can be used verbatim:
 * <pre>
 *   [OreDict:|Fluid:]&lt;name&gt;[#&lt;variant&gt;][@&lt;metadata&gt;][*&lt;amount&gt;]
 * </pre>
 *
 * <p>Two things changed meaning in the port: {@code #variant} used to select a metadata value of a
 * multi-item and now selects the separately registered item created by the flattening (see
 * {@link IC2ItemVariants}), and a bare {@code @metadata} no longer has anything to address, so it
 * is accepted and ignored.
 */
public class ConfigUtil {
    /**
     * Groups whose members are blocks rather than items, mapping the config's {@code group#variant}
     * to the flattened block id. A null value means "the variant name is the block id".
     */
    private static final Map<String, String> BLOCK_GROUP_SUFFIX = new HashMap<>();

    static {
        BLOCK_GROUP_SUFFIX.put("resource", null);
        BLOCK_GROUP_SUFFIX.put("te", null);
        BLOCK_GROUP_SUFFIX.put("glass", "_glass");
        BLOCK_GROUP_SUFFIX.put("fence", "_fence");
    }

    public static boolean getBool(Config config, String path) { return config.getBool(path, false); }
    public static int getInt(Config config, String path) { return config.getInt(path, 0); }
    public static double getDouble(Config config, String path) { return config.getDouble(path, 0.0); }
    public static String getString(Config config, String path) { return config.getString(path, ""); }

    /** Recipe inputs from a comma separated config value, skipping the ones that do not resolve. */
    public static List<IRecipeInput> asRecipeInputList(Config config, String path) {
        List<IRecipeInput> ret = new ArrayList<>();
        for (String part : config.getList(path)) {
            try {
                IRecipeInput input = asRecipeInput(part);
                if (input != null) {
                    ret.add(input);
                }
            } catch (ParseException e) {
                // an unparsable entry disables nothing, which is what the original did as well
            }
        }
        return ret;
    }

    /** Splits a comma separated config value. */
    public static List<String> asList(String str) {
        str = str.trim();
        return str.isEmpty() ? Collections.emptyList() : Arrays.asList(str.split("\\s*,\\s*"));
    }

    public static ItemStack asStack(String str) throws ParseException {
        return asStack(str, false);
    }

    public static ItemStack asStackWithAmount(String str) throws ParseException {
        return asStack(str, true);
    }

    public static IRecipeInput asRecipeInput(String str) throws ParseException {
        return asRecipeInput(str, false);
    }

    public static IRecipeInput asRecipeInputWithAmount(String str) throws ParseException {
        return asRecipeInput(str, true);
    }

    public static List<ItemStack> asStackList(String str) throws ParseException {
        List<String> parts = asList(str);
        List<ItemStack> ret = new ArrayList<>(parts.size());
        for (String part : parts) {
            ret.add(asStack(part));
        }
        return ret;
    }

    public static List<IRecipeInput> asRecipeInputList(String str) throws ParseException {
        return asRecipeInputList(str, false);
    }

    public static List<IRecipeInput> asRecipeInputList(String str, boolean allowNull) throws ParseException {
        List<String> parts = asList(str);
        List<IRecipeInput> ret = new ArrayList<>(parts.size());
        for (String part : parts) {
            IRecipeInput input = asRecipeInput(part);
            if (input == null && !allowNull) {
                throw new ParseException("There is no item matching " + part + ".", -1);
            }
            ret.add(input);
        }
        return ret;
    }

    private static ItemStack asStack(String str, boolean checkAmount) throws ParseException {
        Reference ref = Reference.parse(str, checkAmount);
        Item item = resolveItem(ref.name, ref.variant, ref.meta);
        if (item == null) {
            return null;
        }
        return new ItemStack(item, checkAmount ? ref.amount : 1);
    }

    private static IRecipeInput asRecipeInput(String str, boolean checkAmount) throws ParseException {
        Reference ref = Reference.parse(str, checkAmount);

        if (ref.name.startsWith("OreDict:")) {
            String oreDict = ref.name.substring("OreDict:".length());
            return Recipes.inputFactory.forOreDict(oreDict, ref.amount);
        }
        if (ref.name.startsWith("Fluid:")) {
            String fluid = ref.name.substring("Fluid:".length());
            // fluid entries default to a full bucket/cell rather than a single item
            int amount = ref.amountSpecified ? ref.amount : 1000;
            return Recipes.inputFactory.forFluidContainer(LiquidUtil.getFluid(fluid), amount);
        }

        ItemStack stack = asStack(str, checkAmount);
        return stack == null ? null : Recipes.inputFactory.forStack(stack);
    }

    /**
     * Resolves a config item name to a registered item.
     *
     * @param name    namespaced name, e.g. {@code ic2:dust} or {@code minecraft:cobblestone}
     * @param variant the {@code #variant} suffix, or null
     * @return the item, or null if nothing is registered under any of the candidate ids
     */
    public static Item resolveItem(String name, String variant) {
        return resolveItem(name, variant, null);
    }

    /**
     * Resolves a config item name to a registered item.
     *
     * @param name    namespaced name, e.g. {@code ic2:dust} or {@code minecraft:stonebrick}
     * @param variant the {@code #variant} suffix, or null
     * @param meta    the {@code @metadata} value, or null for none/any; only vanilla names use it
     * @return the item, or null if nothing is registered under any of the candidate ids
     */
    public static Item resolveItem(String name, String variant, String meta) {
        if (name == null || name.isEmpty() || !name.contains(":")) {
            return null;
        }

        Identifier id;
        try {
            // 1.12.2 accepted the mod id in any case ("IC2:scaffold"); ids are lower case now
            id = Identifier.of(name.toLowerCase(java.util.Locale.ROOT));
        } catch (Exception e) {
            return null;
        }

        if (variant == null) {
            if (!"ic2".equals(id.getNamespace())) {
                // pre-flattening vanilla names still carry their 1.12.2 spelling and metadata
                Identifier flattened = Identifier.of(id.getNamespace(),
                        LegacyItemNames.resolve(id.getPath(), meta));
                return lookup(flattened);
            }
            return lookup(id);
        }

        if ("ic2".equals(id.getNamespace())) {
            String group = id.getPath();

            Item item = IC2ItemVariants.get(group, variant);
            if (item != null) {
                return item;
            }

            // blocks are registered under their flattened name, not under a group item
            if (BLOCK_GROUP_SUFFIX.containsKey(group)) {
                String suffix = BLOCK_GROUP_SUFFIX.get(group);
                Item block = lookup(Identifier.of("ic2", suffix == null ? variant : variant + suffix));
                if (block != null) {
                    return block;
                }
            }

            return lookup(Identifier.of("ic2", group + "_" + variant));
        }

        return lookup(Identifier.of(id.getNamespace(), id.getPath() + "_" + variant));
    }

    private static Item lookup(Identifier id) {
        return Registries.ITEM.containsId(id) ? Registries.ITEM.get(id) : null;
    }

    /** A parsed {@code name[#variant][@meta][*amount]} reference. */
    private static final class Reference {
        final String name;
        final String variant;
        final String meta;
        final int amount;
        final boolean amountSpecified;

        private Reference(String name, String variant, String meta, int amount, boolean amountSpecified) {
            this.name = name;
            this.variant = variant;
            this.meta = meta;
            this.amount = amount;
            this.amountSpecified = amountSpecified;
        }

        static Reference parse(String str, boolean checkAmount) throws ParseException {
            String[] parts = str.trim().split("(?=[@#*])");
            String name = parts[0];
            String variant = null;
            String meta = null;
            int amount = 1;
            boolean amountSpecified = false;

            for (int i = 1; i < parts.length; i++) {
                String part = parts[i];
                switch (part.charAt(0)) {
                    case '#' -> variant = part.substring(1);
                    case '@' -> {
                        // "@*" (any metadata) splits into a bare "@" plus a "*" token; it selects
                        // the default variant of a legacy name rather than a specific one
                        if (part.length() == 1 && i + 1 < parts.length && parts[i + 1].startsWith("*")) {
                            i++;
                        } else if (part.length() > 1) {
                            meta = part.substring(1);
                        }
                    }
                    case '*' -> {
                        if (!checkAmount) {
                            throw new ParseException("We do not support amount here: " + str, -1);
                        }
                        amount = Integer.parseInt(part.substring(1));
                        amountSpecified = true;
                    }
                    default -> throw new ParseException("unparsable reference: " + str, -1);
                }
            }

            return new Reference(name, variant, meta, amount, amountSpecified);
        }
    }
}
