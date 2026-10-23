package ic2.core.crop;

import ic2.api.crops.ICropTile;

/**
 * Fabric port of CropVanillaStem - melon and pumpkin, which grow a fruit off a stem.
 *
 * <p>The stem stays behind after the harvest, which is why it drops back one size instead of one
 * and weighs humidity a little heavier than the other two conditions.
 */
public abstract class CropVanillaStem extends CropVanilla {
    protected CropVanillaStem(int maxAge) {
        super(maxAge);
    }

    @Override
    public int getWeightInfluences(ICropTile crop, int humidity, int nutrients, int air) {
        return (int) (humidity * 1.1 + nutrients * 0.9 + air);
    }

    @Override
    public int getSizeAfterHarvest(ICropTile crop) {
        return maxAge - 1;
    }
}
