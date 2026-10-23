package ic2.core.crop.cards;

import ic2.api.crops.CropProperties;
import ic2.api.crops.ICropTile;
import ic2.core.crop.IC2CropCard;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/**
 * Fabric port of CropRedWheat - wheat that grows in the dark and carries redstone.
 *
 * <p>A fully grown plant lights up and emits a redstone signal itself. What it drops depends on
 * whether it is being powered: a powered plant always gives redstone, an unpowered one gives
 * wheat about half the time.
 */
public class CropRedWheat extends IC2CropCard {
    @Override public String getId() { return "redwheat"; }
    @Override public String getDiscoveredBy() { return "raa1337"; }
    @Override public CropProperties getProperties() { return new CropProperties(6, 3, 0, 0, 2, 0); }
    @Override public String[] getAttributes() { return new String[]{"Red", "Redstone", "Wheat"}; }
    @Override public int getMaxSize() { return 7; }

    @Override
    public boolean canGrow(ICropTile crop) {
        return crop.getCurrentSize() < 7 && crop.getLightLevel() <= 10 && crop.getLightLevel() >= 5;
    }

    @Override public boolean canBeHarvested(ICropTile crop) { return crop.getCurrentSize() == 7; }
    @Override public double dropGainChance() { return 0.5; }
    @Override public int getOptimalHarvestSize(ICropTile crop) { return 7; }

    @Override
    public ItemStack getGain(ICropTile crop) {
        var world = crop.getWorldObj();
        boolean unpowered = world.getReceivedRedstonePower(crop.getPosition()) <= 0;
        return unpowered && !world.random.nextBoolean()
                ? new ItemStack(Items.WHEAT) : new ItemStack(Items.REDSTONE);
    }

    @Override public boolean isRedstoneSignalEmitter(ICropTile crop) { return true; }
    @Override public int getEmittedRedstoneSignal(ICropTile crop) { return crop.getCurrentSize() == 7 ? 15 : 0; }
    @Override public int getEmittedLight(ICropTile crop) { return crop.getCurrentSize() == 7 ? 7 : 0; }
    @Override public int getGrowthDuration(ICropTile crop) { return 600; }
    @Override public int getSizeAfterHarvest(ICropTile crop) { return 2; }
}
