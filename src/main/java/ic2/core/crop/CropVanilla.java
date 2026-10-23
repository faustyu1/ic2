package ic2.core.crop;

import ic2.api.crops.ICropTile;
import net.minecraft.item.ItemStack;

/**
 * Fabric port of CropVanilla - a crop that mirrors a vanilla plant.
 *
 * <p>Its size is the vanilla plant's age, and an unbred plant drops the vanilla seed rather than
 * an IC2 seed bag, so a player can put the harvest straight back into a normal field.
 */
public abstract class CropVanilla extends IC2CropCard {
    protected final int maxAge;

    protected CropVanilla(int maxAge) {
        this.maxAge = maxAge;
    }

    @Override
    public String getDiscoveredBy() {
        return "Notch";
    }

    @Override
    public int getMaxSize() {
        return maxAge;
    }

    @Override
    public boolean canGrow(ICropTile crop) {
        return crop.getCurrentSize() < getMaxSize() && crop.getLightLevel() >= 9;
    }

    protected abstract ItemStack getSeeds();

    protected abstract ItemStack getProduct();

    @Override
    public ItemStack getGain(ICropTile crop) {
        return getProduct();
    }

    @Override
    public ItemStack getSeeds(ICropTile crop) {
        return crop.getStatGain() <= 1 && crop.getStatGrowth() <= 1 && crop.getStatResistance() <= 1
                ? getSeeds() : super.getSeeds(crop);
    }
}
