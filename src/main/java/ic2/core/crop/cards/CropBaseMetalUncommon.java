package ic2.core.crop.cards;

import ic2.api.crops.CropProperties;
import ic2.api.crops.ICropTile;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

/**
 * Fabric port of CropBaseMetalUncommon - the rarer metal crops, aurelia and shining.
 *
 * <p>One size taller than the common ones and correspondingly slower, but it keeps the full drop
 * chance of its tier instead of the halved one.
 */
public class CropBaseMetalUncommon extends CropBaseMetalCommon {
    public CropBaseMetalUncommon(String cropName, String[] cropAttributes, Block[] cropRootsRequirement, ItemStack cropDrop) {
        super(cropName, cropAttributes, cropRootsRequirement, cropDrop);
    }

    public CropBaseMetalUncommon(String cropName, String[] cropAttributes, String[] cropRootsRequirement, ItemStack cropDrop) {
        super(cropName, cropAttributes, cropRootsRequirement, cropDrop);
    }

    @Override public int getMaxSize() { return 5; }
    @Override public CropProperties getProperties() { return new CropProperties(6, 2, 0, 0, 2, 0); }

    @Override
    public boolean canGrow(ICropTile crop) {
        if (crop.getCurrentSize() < 4) {
            return true;
        }
        return crop.getCurrentSize() == 4 && hasRoots(crop);
    }

    @Override public boolean canBeHarvested(ICropTile crop) { return crop.getCurrentSize() == 5; }
    @Override public int getOptimalHarvestSize(ICropTile crop) { return 5; }
    @Override public double dropGainChance() { return Math.pow(0.95, getProperties().getTier()); }
    @Override public int getGrowthDuration(ICropTile crop) { return crop.getCurrentSize() == 4 ? 2200 : 750; }
}
