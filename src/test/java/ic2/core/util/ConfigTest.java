package ic2.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import ic2.core.init.MainConfig;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.Test;

/** Covers the ini reader and the values the port takes from general.ini. */
public class ConfigTest {
    @Test
    public void normalizesSectionSpacing() throws IOException {
        Config config = new Config("test");
        config.load(new ByteArrayInputStream((
                "[balance / energy / generator]\n"
                + "solar = 1.5\n"
                + "[balance/fermenter]\n"
                + "hU_per_run =  4000\n"
                + "[balance /SteamKineticGenerator]\n"
                + "rotorlivetime = 86400\n").getBytes(StandardCharsets.UTF_8)));

        assertEquals(1.5, config.getDouble("balance/energy/generator/solar", 0.0), 1e-9);
        assertEquals(4000, config.getInt("balance/fermenter/hU_per_run", 0));
        assertEquals(86400, config.getInt("balance/SteamKineticGenerator/rotorlivetime", 0));
    }

    @Test
    public void dropsCommentsAndBlankLines() throws IOException {
        Config config = new Config("test");
        config.load(new ByteArrayInputStream((
                "; a comment\n"
                + "\n"
                + "[misc]\n"
                + "roundEnetLoss = true ; trailing comment\n").getBytes(StandardCharsets.UTF_8)));

        assertEquals(1, config.size());
        assertTrue(config.getBool("misc/roundEnetLoss", false));
    }

    @Test
    public void readsTheShippedGeneralIni() {
        Config config = MainConfig.get();

        assertTrue("general.ini did not load", config.size() > 100);
        assertTrue(config.getBool("worldgen/rubberTree", false));
        assertEquals(20, config.getInt("worldgen/uranium/count", 0));
        assertEquals(60, config.getInt("protection/nukeExplosionPowerLimit", 0));
        assertEquals(3, config.getInt("balance/massFabricatorTier", 0));
        assertTrue(config.getBool("recipes/allowCoinCrafting", false));
        assertTrue(config.getBool("misc/roundEnetLoss", false));

        List<String> blacklist = config.getList("balance/recyclerBlacklist");
        assertTrue("recycler blacklist should be populated", blacklist.size() >= 5);
        assertTrue(blacklist.contains("minecraft:stick"));
    }

    @Test
    public void exposesPredefinedUuValues() {
        Config config = MainConfig.get();

        assertEquals(12000.0,
                config.getDouble("balance/uu-values/predefined/IC2:misc_resource#iridium_ore", 0.0), 1e-9);
        assertEquals(1333.0,
                config.getDouble("balance/uu-values/predefined/IC2:misc_resource#iridium_shard", 0.0), 1e-9);
    }

    /** The user file's values win, the keys it lacks keep their defaults. */
    @Test
    public void mergeOverridesOnlyWhatItHas() throws IOException {
        Config config = new Config("test");
        config.load(new ByteArrayInputStream("[recipes]\npreferredOutputMods = ic2\ndisable =\n".getBytes(StandardCharsets.UTF_8)));
        config.merge(new ByteArrayInputStream("[recipes]\npreferredOutputMods = techreborn, ic2\n".getBytes(StandardCharsets.UTF_8)));

        assertEquals("techreborn, ic2", config.getString("recipes/preferredOutputMods", null));
        assertEquals("", config.getString("recipes/disable", null));
        assertEquals(2, config.size());
    }
}
