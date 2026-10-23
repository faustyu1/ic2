package ic2.core.crop.cards;

import ic2.api.crops.CropProperties;
import ic2.api.crops.ICropTile;
import ic2.core.crop.IC2CropCard;
import net.minecraft.item.ItemStack;

/** Fabric port of CropBaseMushroom - the two vanilla mushrooms, which need water to grow at all. */
public class CropBaseMushroom extends IC2CropCard {
    protected final String cropId;
    protected final String[] cropAttributes;
    protected final ItemStack cropDrop;

    public CropBaseMushroom(String cropId, String[] cropAttributes, ItemStack cropDrop) {
        this.cropId = cropId;
        this.cropAttributes = cropAttributes;
        this.cropDrop = cropDrop;
    }

    @Override public String getId() { return cropId; }
    @Override public CropProperties getProperties() { return new CropProperties(2, 0, 4, 0, 0, 4); }
    @Override public String[] getAttributes() { return cropAttributes; }
    @Override public int getMaxSize() { return 3; }

    @Override
    public boolean canGrow(ICropTile crop) {
        return crop.getCurrentSize() < getMaxSize() && crop.getStorageWater() > 0;
    }

    @Override public ItemStack getGain(ICropTile crop) { return cropDrop.copy(); }
    @Override public int getGrowthDuration(ICropTile crop) { return 200; }
}
