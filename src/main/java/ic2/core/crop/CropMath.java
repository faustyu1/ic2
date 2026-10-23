package ic2.core.crop;

import ic2.api.crops.CropCard;
import java.util.Random;

/**
 * The arithmetic of {@link TileEntityCrop}, kept apart from the world so it can be checked on its
 * own. Every formula is the one 1.12.2 ran inline in the crop tile, including its quirks.
 */
public final class CropMath {
    private CropMath() {}

    /**
     * How much the biome's climate adds to a crop's humidity: rain helps or hurts by up to 10,
     * and the temperature scales that by a parabola that peaks at 1.0.
     *
     * @param downfall the biome's rainfall, 0..1 for the vanilla biomes
     * @param temperature the biome's temperature at the crop
     */
    public static int biomeHumidityBonus(float downfall, float temperature) {
        int rainfallBonus = (int) ((double) (25.0F * downfall) - 12.5);
        rainfallBonus = clamp(rainfallBonus, -10, 10);

        int coefficientBonus = (int) ((double) Math.abs(rainfallBonus)
                * (-2.0 * Math.pow(temperature, 2.0) + (double) (4.0F * temperature) - 1.0));
        coefficientBonus = clamp(coefficientBonus, -10, 10);

        return rainfallBonus + coefficientBonus;
    }

    /**
     * The quality the environment has to provide for a crop of that tier and those stats to grow
     * at full speed; better stats ask for more.
     */
    public static int minimumQuality(int tier, int statGrowth, int statGain, int statResistance) {
        return Math.max(0, (tier - 1) * 4 + statGrowth + statGain + statResistance);
    }

    /**
     * Growth points one growth tick adds.
     *
     * <p>A good environment speeds the crop up by one percent per point of surplus, a poor one
     * slows it down by four percent per missing point.
     *
     * @return the points, or -1 when the environment is so poor that the crop dies; the caller
     *         resets the crop then, exactly as the original did
     */
    public static int growthPoints(int baseGrowth, int minimumQuality, int providedQuality, int statResistance, Random random) {
        if (providedQuality >= minimumQuality) {
            return baseGrowth * (100 + (providedQuality - minimumQuality)) / 100;
        }

        int aux = (minimumQuality - providedQuality) * 4;
        if (aux > 100 && random.nextInt(32) > statResistance) {
            return -1;
        }

        return Math.max(0, baseGrowth * (100 - aux) / 100);
    }

    /**
     * How likely a crossing produces {@code newCrop} from a neighbour that is {@code oldCrop}:
     * the same crop is very likely, a crop with similar properties and shared attributes less so,
     * and one that is several tiers apart hardly at all.
     */
    public static int crossingRatio(CropCard newCrop, CropCard oldCrop) {
        if (newCrop == oldCrop) {
            return 500;
        }

        int value = 0;
        int[] propOld = oldCrop.getProperties().getAllProperties();
        int[] propNew = newCrop.getProperties().getAllProperties();

        for (int i = 0; i < 5; i++) {
            int delta = Math.abs(propOld[i] - propNew[i]);
            value += -delta + 2;
        }

        for (String attributeNew : newCrop.getAttributes()) {
            for (String attributeOld : oldCrop.getAttributes()) {
                if (attributeNew.equalsIgnoreCase(attributeOld)) {
                    value += 5;
                }
            }
        }

        int diff = newCrop.getProperties().getTier() - oldCrop.getProperties().getTier();
        if (diff > 1) {
            value -= 2 * diff;
        }
        if (diff < -3) {
            value -= -diff;
        }

        return Math.max(value, 0);
    }

    /**
     * The odds a neighbour takes part in crossing or spreads onto an empty stick, out of 16:
     * a fast grower is more eager, a very resistant one is not.
     */
    public static int spreadBase(int statGrowth, int statResistance) {
        int base = 4;
        if (statGrowth >= 16) {
            base++;
        }
        if (statGrowth >= 30) {
            base++;
        }
        if (statResistance >= 28) {
            base += 27 - statResistance;
        }
        return base;
    }

    /**
     * Index of the entry a draw of {@code search} lands on, in cumulative weights.
     *
     * @param ratios running totals, each at least the one before
     * @param search a value in {@code [0, ratios[last])}
     */
    public static int pickWeighted(int[] ratios, int search) {
        int min = 0;
        int max = ratios.length - 1;

        while (min < max) {
            int cur = (min + max) / 2;
            if (search < ratios[cur]) {
                max = cur;
            } else {
                min = cur + 1;
            }
        }

        return min;
    }

    /** How many items a harvest yields: a normal distribution around the drop chance. */
    public static int harvestCount(double chance, double gaussian) {
        return (int) Math.max(0L, Math.round(gaussian * chance * 0.6827 + chance));
    }

    public static int clamp(int value, int min, int max) {
        return value < min ? min : Math.min(value, max);
    }
}
