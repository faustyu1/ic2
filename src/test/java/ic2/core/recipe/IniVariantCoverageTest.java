package ic2.core.recipe;

import static org.junit.Assert.assertTrue;

import ic2.core.block.BlockScaffold;
import ic2.core.block.BlockSheet;
import ic2.core.block.transport.items.PipeSize;
import ic2.core.block.transport.items.PipeType;
import ic2.core.block.wiring.CableType;
import ic2.core.item.logistics.PumpCoverType;
import ic2.core.item.type.BlockCuttingBladeType;
import ic2.core.item.type.BoatType;
import ic2.core.item.type.CasingResourceType;
import ic2.core.item.type.CraftingItemType;
import ic2.core.item.type.CropResItemType;
import ic2.core.item.type.DustResourceType;
import ic2.core.item.type.IngotResourceType;
import ic2.core.item.type.MiscResourceType;
import ic2.core.item.type.MugType;
import ic2.core.item.type.NuclearResourceType;
import ic2.core.item.type.OreResourceType;
import ic2.core.item.type.PlateResourceType;
import ic2.core.item.type.TfbpType;
import ic2.core.item.type.UpdateKitType;
import ic2.core.item.type.UpgradeType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;

/**
 * Guards the flattening tables: every {@code ic2:<group>#<variant>} the shipped config files
 * reference must be covered by an item variant group, otherwise those recipes would silently
 * drop out on load.
 *
 * <p>Groups whose members are blocks ({@code te}, {@code resource}, ...) are skipped: those are
 * resolved through the block registry rather than through the item variant tables.
 */
public class IniVariantCoverageTest {
    private static final String[] CONFIGS = {
        "macerator", "compressor", "extractor", "furnace", "ore_washer", "thermal_centrifuge",
        "blast_furnace", "block_cutter", "block_cutter_late",
        "metal_former_cutting", "metal_former_extruding", "metal_former_rolling",
        "shaped_recipes", "shapeless_recipes", "uu_recipes"
    };

    /** Groups backed by blocks, which the recipe loader resolves through the block registry. */
    private static final Set<String> BLOCK_GROUPS = Set.of("te", "resource", "glass", "fence");

    private static final Pattern REFERENCE = Pattern.compile("\\bic2:([a-z0-9_]+)#([a-z0-9_:,]+)");

    @Test
    public void everyReferencedVariantIsCovered() throws IOException {
        Map<String, Set<String>> groups = knownGroups();
        Set<String> missing = new TreeSet<>();

        for (String config : CONFIGS) {
            for (String line : readConfig(config)) {
                Matcher m = REFERENCE.matcher(line);
                while (m.find()) {
                    String group = m.group(1);
                    String variant = m.group(2);
                    if (BLOCK_GROUPS.contains(group)) {
                        continue;
                    }
                    Set<String> variants = groups.get(group);
                    if (variants == null || !variants.contains(variant)) {
                        missing.add(group + "#" + variant + " (" + config + ")");
                    }
                }
            }
        }

        assertTrue("config references without a registered item variant: " + missing, missing.isEmpty());
    }

    private static Map<String, Set<String>> knownGroups() {
        Map<String, Set<String>> ret = new HashMap<>();
        ret.put("crushed", names(OreResourceType.values()));
        ret.put("purified", names(OreResourceType.values()));
        ret.put("dust", names(DustResourceType.values()));
        ret.put("ingot", names(IngotResourceType.values()));
        ret.put("plate", names(PlateResourceType.values()));
        ret.put("casing", names(CasingResourceType.values()));
        ret.put("nuclear", names(NuclearResourceType.values()));
        ret.put("misc_resource", names(MiscResourceType.values()));
        ret.put("crafting", names(CraftingItemType.values()));
        ret.put("crop_res", names(CropResItemType.values()));
        ret.put("upgrade", names(UpgradeType.values()));
        ret.put("upgrade_kit", names(UpdateKitType.values()));
        ret.put("block_cutting_blade", names(BlockCuttingBladeType.values()));
        ret.put("tfbp", names(TfbpType.values()));
        ret.put("boat", names(BoatType.values()));
        ret.put("mug", names(MugType.values()));
        ret.put("cover", names(PumpCoverType.values()));
        ret.put("sheet", names(BlockSheet.SheetType.values()));
        ret.put("scaffold", names(BlockScaffold.ScaffoldType.values()));
        ret.put("mining_pipe", Set.of("pipe"));
        ret.put("painter", ic2Colors());
        ret.put("fluid_cell", fluidCellVariants());
        ret.put("cable", cableVariants());
        ret.put("pipe", pipeVariants());
        return ret;
    }

    private static Set<String> cableVariants() {
        Set<String> ret = new HashSet<>();
        for (CableType type : CableType.values()) {
            for (int insulation = 0; insulation <= type.maxInsulation; insulation++) {
                ret.add("type:" + type.getName() + ",insulation:" + insulation);
            }
        }
        return ret;
    }

    private static Set<String> pipeVariants() {
        Set<String> ret = new HashSet<>();
        for (PipeType type : PipeType.values()) {
            for (PipeSize size : PipeSize.values()) {
                ret.add("type:" + type.getName() + ",size:" + size.getName());
            }
        }
        return ret;
    }

    private static Set<String> ic2Colors() {
        Set<String> ret = new HashSet<>();
        for (ic2.core.util.Ic2Color color : ic2.core.util.Ic2Color.values()) {
            ret.add(color.getName());
        }
        return ret;
    }

    private static Set<String> fluidCellVariants() {
        return Set.of("water", "lava", "ic2air", "ic2biogas", "ic2biomass", "ic2biofuel",
                "ic2coolant", "ic2construction_foam", "ic2distilled_water", "ic2hot_coolant",
                "ic2hot_water", "ic2pahoehoe_lava", "ic2steam", "ic2superheated_steam",
                "ic2uu_matter", "ic2weed_ex");
    }

    private static Set<String> names(Enum<?>[] values) {
        Set<String> ret = new HashSet<>();
        for (Enum<?> value : values) {
            ret.add(value.name());
        }
        return ret;
    }

    private static List<String> readConfig(String name) throws IOException {
        String path = "/assets/ic2/config/" + name + ".ini";
        List<String> ret = new ArrayList<>();

        try (InputStream is = IniVariantCoverageTest.class.getResourceAsStream(path)) {
            if (is == null) {
                throw new IOException("missing config " + path);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    int comment = line.indexOf(';');
                    if (comment >= 0) {
                        line = line.substring(0, comment);
                    }
                    line = line.trim();
                    if (!line.isEmpty()) {
                        ret.add(line);
                    }
                }
            }
        }

        return ret;
    }
}
