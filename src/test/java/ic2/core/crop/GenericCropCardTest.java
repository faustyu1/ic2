package ic2.core.crop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import ic2.api.crops.BaseSeed;
import ic2.api.crops.CropCard;
import ic2.api.crops.CropProperties;
import ic2.api.crops.Crops;
import ic2.core.crop.cards.GenericCropCard;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import net.minecraft.client.texture.Sprite;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import org.junit.Before;
import org.junit.Test;

/**
 * The builder behind the crops that are nothing but numbers: what it fills in by itself and what
 * it refuses, so a crop with impossible numbers never reaches the registry.
 */
public class GenericCropCardTest {

    private RecordingCrops registry;

    @Before
    public void installRegistry() {
        registry = new RecordingCrops();
        Crops.instance = registry;
    }

    @Test
    public void aCropWithoutAHarvestSizeIsHarvestedWhenItIsFullyGrown() {
        GenericCropCard card = GenericCropCard.create("blazereed")
                .setDiscoveredBy("Mr. Brain")
                .setProperties(new CropProperties(6, 0, 4, 1, 0, 0))
                .setAttributes(new String[]{"Fire"})
                .setMaxSize(4)
                .setAfterHarvestSize(1)
                .register();

        assertEquals(1, registry.registered.size());
        assertEquals("blazereed", card.getId());
        assertEquals("ic2", card.getOwner());
        assertEquals("ic2.crop.blazereed", card.getUnlocalizedName());
        assertEquals(4, card.getMaxSize());
    }

    @Test
    public void everySizeGetsItsOwnTexture() {
        GenericCropCard card = GenericCropCard.create("withereed")
                .setDiscoveredBy("CovertJaguar")
                .setProperties(new CropProperties(8, 2, 0, 4, 1, 3))
                .setMaxSize(4)
                .setAfterHarvestSize(1)
                .register();

        List<Identifier> textures = card.getTexturesLocation();
        assertEquals(4, textures.size());
        assertEquals(Identifier.of("ic2", "blocks/crop/withereed_1"), textures.get(0));
        assertEquals(Identifier.of("ic2", "blocks/crop/withereed_4"), textures.get(3));
    }

    @Test
    public void aCropWithoutADiscovererIsRefused() {
        try {
            GenericCropCard.create("nameless")
                    .setProperties(new CropProperties(1, 0, 0, 0, 0, 0))
                    .setMaxSize(4)
                    .setAfterHarvestSize(1)
                    .register();
            fail("expected the missing discoverer to be refused");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("discoveredBy"));
        }

        assertTrue(registry.registered.isEmpty());
    }

    @Test
    public void aCropShorterThanThreeIsRefused() {
        try {
            GenericCropCard.create("stunted")
                    .setDiscoveredBy("nobody")
                    .setProperties(new CropProperties(1, 0, 0, 0, 0, 0))
                    .setMaxSize(2)
                    .setAfterHarvestSize(1)
                    .register();
            fail("expected the too small maxSize to be refused");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("maxSize"));
        }
    }

    @Test
    public void aCropThatWouldVanishOnHarvestIsRefused() {
        try {
            GenericCropCard.create("vanishing")
                    .setDiscoveredBy("nobody")
                    .setProperties(new CropProperties(1, 0, 0, 0, 0, 0))
                    .setMaxSize(4)
                    .register();
            fail("expected the missing afterHarvestSize to be refused");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("afterHarvestSize"));
        }
    }

    @Test
    public void theBuilderKeepsTheGrowthSpeedItWasGiven() {
        GenericCropCard slow = GenericCropCard.create("meat_rose")
                .setDiscoveredBy("VintageBeef")
                .setProperties(new CropProperties(7, 0, 4, 1, 3, 0))
                .setMaxSize(4)
                .setGrowthSpeed(1500)
                .setAfterHarvestSize(1)
                .register();

        assertNotNull(slow.getProperties());
        // tier 7 * 1500, because the speed is above the 200 the default would use
        assertEquals(7 * 1500, slow.getGrowthDuration(null));
    }

    /** A registry that only records, so the test does not need the game's own. */
    private static final class RecordingCrops extends Crops {
        private final List<CropCard> registered = new ArrayList<>();

        @Override public void addBiomenutrientsBonus(BiomeDictionary.Type type, int bonus) {}
        @Override public void addBiomehumidityBonus(BiomeDictionary.Type type, int bonus) {}
        @Override public int getHumidityBiomeBonus(RegistryEntry<Biome> biome) { return 0; }
        @Override public int getNutrientBiomeBonus(RegistryEntry<Biome> biome) { return 0; }
        @Override public CropCard getCropCard(String owner, String id) { return null; }
        @Override public CropCard getCropCard(ItemStack stack) { return null; }
        @Override public Collection<CropCard> getCrops() { return registered; }
        @Override public void registerCrop(CropCard card) { registered.add(card); }
        @Override public void registerCropTextures(Map<Identifier, Sprite> textures) {}
        @Override public boolean registerBaseSeed(ItemStack stack, CropCard crop, int size, int growth, int gain, int resistance) { return true; }
        @Override public BaseSeed getBaseSeed(ItemStack stack) { return null; }
    }
}
