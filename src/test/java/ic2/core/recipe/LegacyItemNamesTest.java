package ic2.core.recipe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import ic2.core.util.LegacyItemNames;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;

/**
 * Covers the 1.12.2 to 1.21.1 vanilla item mapping the config files rely on.
 */
public class LegacyItemNamesTest {
    private static final String[] CONFIGS = {
        "macerator", "compressor", "extractor", "furnace", "ore_washer", "thermal_centrifuge",
        "blast_furnace", "block_cutter", "block_cutter_late",
        "metal_former_cutting", "metal_former_extruding", "metal_former_rolling",
        "shaped_recipes", "shapeless_recipes", "uu_recipes", "uu_scan_values"
    };

    private static final Pattern VANILLA = Pattern.compile("\\bminecraft:([a-z0-9_]+)@(\\d+)");

    @Test
    public void mapsFlattenedNames() {
        assertEquals("stone_bricks", LegacyItemNames.resolve("stonebrick", null));
        assertEquals("chiseled_stone_bricks", LegacyItemNames.resolve("stonebrick", "3"));
        assertEquals("oak_log", LegacyItemNames.resolve("log", "0"));
        assertEquals("dark_oak_log", LegacyItemNames.resolve("log2", "1"));
        assertEquals("lapis_lazuli", LegacyItemNames.resolve("dye", "4"));
        assertEquals("red_terracotta", LegacyItemNames.resolve("stained_hardened_clay", "14"));
    }

    @Test
    public void mapsRenamedNamesWithoutMetadata() {
        assertEquals("sugar_cane", LegacyItemNames.resolve("reeds", null));
        assertEquals("lily_pad", LegacyItemNames.resolve("waterlily", null));
        assertEquals("grass_block", LegacyItemNames.resolve("grass", null));
        assertEquals("snow", LegacyItemNames.resolve("snow_layer", null));
        assertEquals("snow_block", LegacyItemNames.resolve("snow", null));
    }

    @Test
    public void keepsUnchangedNames() {
        assertEquals("cobblestone", LegacyItemNames.resolve("cobblestone", null));
        assertEquals("diamond", LegacyItemNames.resolve("diamond", "0"));
    }

    @Test
    public void anyMetadataFallsBackToTheDefaultVariant() {
        // "@*" reaches resolve() with a null metadata, which must still map the renamed name
        assertEquals("oak_log", LegacyItemNames.resolve("log", null));
        assertEquals("white_wool", LegacyItemNames.resolve("wool", null));
    }

    @Test
    public void everyMetadataReferenceInTheConfigsIsMapped() throws IOException {
        Set<String> unmapped = new TreeSet<>();

        for (String config : CONFIGS) {
            for (String line : readConfig(config)) {
                Matcher m = VANILLA.matcher(line);
                while (m.find()) {
                    String name = m.group(1);
                    String meta = m.group(2);
                    // a metadata value always needs an explicit entry: without one the reference
                    // would resolve to the unrelated base item
                    if (LegacyItemNames.resolve(name, meta).equals(LegacyItemNames.resolve(name, null))
                            && !meta.equals("0")) {
                        unmapped.add("minecraft:" + name + "@" + meta + " (" + config + ")");
                    }
                }
            }
        }

        assertTrue("vanilla references without a metadata mapping: " + unmapped, unmapped.isEmpty());
    }

    private static Iterable<String> readConfig(String name) throws IOException {
        String path = "/assets/ic2/config/" + name + ".ini";
        java.util.List<String> ret = new java.util.ArrayList<>();

        try (InputStream is = LegacyItemNamesTest.class.getResourceAsStream(path)) {
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
