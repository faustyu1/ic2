package ic2.core.crop.cards;

import ic2.api.crops.CropProperties;
import ic2.api.crops.ICropTile;
import ic2.core.crop.IC2CropCard;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

/**
 * Fabric port of CropBaseMetalCommon - a crop that grows a metal, like ferru or cyprium.
 *
 * <p>It only reaches its last size over the right block: the roots reach five blocks down and
 * have to find the ore they draw the metal from, which is what makes these crops a way to farm a
 * metal rather than a way to duplicate one.
 */
public class CropBaseMetalCommon extends IC2CropCard {
    protected final String cropName;
    protected final String[] cropAttributes;
    protected final Object[] cropRootsRequirement;
    protected final ItemStack cropDrop;

    public CropBaseMetalCommon(String cropName, String[] cropAttributes, Block[] cropRootsRequirement, ItemStack cropDrop) {
        this(cropName, cropAttributes, (Object[]) cropRootsRequirement, cropDrop);
    }

    public CropBaseMetalCommon(String cropName, String[] cropAttributes, String[] cropRootsRequirement, ItemStack cropDrop) {
        this(cropName, cropAttributes, (Object[]) cropRootsRequirement, cropDrop);
    }

    private CropBaseMetalCommon(String cropName, String[] cropAttributes, Object[] cropRootsRequirement, ItemStack cropDrop) {
        this.cropName = cropName;
        this.cropAttributes = cropAttributes;
        this.cropRootsRequirement = cropRootsRequirement;
        this.cropDrop = cropDrop;
    }

    @Override public String getId() { return cropName; }
    @Override public CropProperties getProperties() { return new CropProperties(6, 2, 0, 0, 1, 0); }
    @Override public String[] getAttributes() { return cropAttributes; }
    @Override public int getMaxSize() { return 4; }

    @Override
    public boolean canGrow(ICropTile crop) {
        if (crop.getCurrentSize() < 3) {
            return true;
        }
        return crop.getCurrentSize() == 3 && hasRoots(crop);
    }

    /** Whether one of the blocks this crop needs is within reach of its roots. */
    protected boolean hasRoots(ICropTile crop) {
        if (cropRootsRequirement == null || cropRootsRequirement.length == 0) {
            return true;
        }

        for (Object requirement : cropRootsRequirement) {
            if (requirement instanceof String name && crop.isBlockBelow(name)) {
                return true;
            }
            if (requirement instanceof Block block && crop.isBlockBelow(block)) {
                return true;
            }
        }
        return false;
    }

    @Override public int getRootsLength(ICropTile crop) { return 5; }
    @Override public boolean canBeHarvested(ICropTile crop) { return crop.getCurrentSize() == 4; }
    @Override public int getOptimalHarvestSize(ICropTile crop) { return 4; }
    @Override public ItemStack getGain(ICropTile crop) { return cropDrop.copy(); }
    @Override public double dropGainChance() { return super.dropGainChance() / 2.0; }
    @Override public int getGrowthDuration(ICropTile crop) { return crop.getCurrentSize() == 3 ? 2000 : 800; }
    @Override public int getSizeAfterHarvest(ICropTile crop) { return 2; }
}
