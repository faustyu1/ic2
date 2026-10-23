package ic2.core.crop;

import static org.junit.Assert.assertEquals;

import ic2.api.crops.CropCard;
import ic2.api.crops.CropProperties;
import java.util.List;
import java.util.Random;
import net.minecraft.util.Identifier;
import org.junit.Test;

/**
 * The crop tile's arithmetic against numbers worked out by hand from the 1.12.2 formulas: the
 * climate bonus, how fast a crop grows, which crop a crossing picks and how much a harvest gives.
 */
public class CropMathTest {

    @Test
    public void theClimateBonusIsRainScaledByAParabolaOfTheTemperature() {
        // a wet, warm biome: rain gives 12, capped at 10, the parabola is 1 at 1.0
        assertEquals(20, CropMath.biomeHumidityBonus(1.0F, 1.0F));
        // half rain is the neutral point, whatever the temperature
        assertEquals(0, CropMath.biomeHumidityBonus(0.5F, 0.5F));
        // a desert: no rain at 2.0 is -10 twice over
        assertEquals(-20, CropMath.biomeHumidityBonus(0.0F, 2.0F));
        // little rain at a mild temperature: -6, and the parabola at 0.5 halves its size back
        assertEquals(-3, CropMath.biomeHumidityBonus(0.25F, 0.5F));
    }

    @Test
    public void betterCropsAskForABetterEnvironment() {
        assertEquals(0, CropMath.minimumQuality(1, 0, 0, 0));
        assertEquals(26, CropMath.minimumQuality(6, 1, 2, 3));
        // tier 0 is weed, and the requirement never goes below zero
        assertEquals(0, CropMath.minimumQuality(0, 0, 0, 0));
    }

    @Test
    public void aSurplusSpeedsGrowthUpByOnePercentAPoint() {
        assertEquals(12, CropMath.growthPoints(10, 0, 20, 0, new Random(0)));
    }

    @Test
    public void aShortfallSlowsGrowthDownByFourPercentAPoint() {
        assertEquals(6, CropMath.growthPoints(10, 20, 10, 0, new Random(0)));
    }

    @Test
    public void aFarTooPoorEnvironmentKillsTheCropUnlessItResists() {
        // 120% short: a crop without resistance always dies
        assertEquals(-1, CropMath.growthPoints(10, 40, 10, -1, new Random(0)));
        // with the highest resistance the draw out of 32 can never beat it, it only stops growing
        assertEquals(0, CropMath.growthPoints(10, 40, 10, 31, new Random(0)));
    }

    @Test
    public void crossingFavoursTheSameCropAndCropsThatShareTraits() {
        CropCard wheat = card(1, new int[] {0, 4, 0, 0, 2}, "Yellow", "Food", "Wheat");
        CropCard carrots = card(2, new int[] {0, 4, 0, 0, 2}, "Orange", "Food", "Carrots");
        CropCard metal = card(6, new int[] {2, 0, 0, 1, 0}, "Metal", "Iron", "Shiny");
        CropCard tallFood = card(5, new int[] {0, 4, 0, 0, 2}, "Food");

        assertEquals(500, CropMath.crossingRatio(wheat, wheat));
        // equal properties give 2 each, the shared "Food" 5, one tier up costs nothing
        assertEquals(15, CropMath.crossingRatio(carrots, wheat));
        // five tiers up costs 10, more than the little the properties give
        assertEquals(0, CropMath.crossingRatio(metal, wheat));
        // four tiers down costs 4
        assertEquals(11, CropMath.crossingRatio(wheat, tallFood));
    }

    @Test
    public void fastGrowersSpreadMoreReadilyAndVeryResistantOnesLess() {
        assertEquals(4, CropMath.spreadBase(0, 0));
        assertEquals(5, CropMath.spreadBase(16, 0));
        assertEquals(6, CropMath.spreadBase(30, 0));
        assertEquals(3, CropMath.spreadBase(0, 28));
        assertEquals(2, CropMath.spreadBase(31, 31));
    }

    @Test
    public void aWeightedDrawSkipsCropsWithoutWeight() {
        int[] ratios = {0, 0, 5, 5, 12};
        assertEquals(2, CropMath.pickWeighted(ratios, 0));
        assertEquals(2, CropMath.pickWeighted(ratios, 4));
        assertEquals(4, CropMath.pickWeighted(ratios, 5));
        assertEquals(4, CropMath.pickWeighted(ratios, 11));
    }

    @Test
    public void theHarvestScattersAroundTheDropChance() {
        assertEquals(1, CropMath.harvestCount(1.0, 0.0));
        assertEquals(0, CropMath.harvestCount(1.0, -2.0));
        assertEquals(3, CropMath.harvestCount(2.0, 1.0));
    }

    private static CropCard card(int tier, int[] properties, String... attributes) {
        CropProperties props = new CropProperties(tier, properties[0], properties[1], properties[2], properties[3], properties[4]);
        return new CropCard() {
            @Override public String getId() { return "test"; }
            @Override public String getOwner() { return "test"; }
            @Override public CropProperties getProperties() { return props; }
            @Override public String[] getAttributes() { return attributes; }
            @Override public int getMaxSize() { return 4; }
            @Override public List<Identifier> getTexturesLocation() { return List.of(); }
        };
    }
}
