package ic2.api.crops;

import ic2.api.info.ILocatable;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface ICropTile extends ILocatable {
    CropCard getCrop();
    void setCrop(CropCard crop);
    int getCurrentSize();
    void setCurrentSize(int size);
    int getStatGrowth();
    void setStatGrowth(int growth);
    int getStatGain();
    void setStatGain(int gain);
    int getStatResistance();
    void setStatResistance(int resistance);
    int getStorageNutrients();
    void setStorageNutrients(int nutrients);
    int getStorageWater();
    void setStorageWater(int water);
    int getStorageWeedEX();
    void setStorageWeedEX(int weedEx);
    int getScanLevel();
    void setScanLevel(int level);
    int getGrowthPoints();
    void setGrowthPoints(int points);
    boolean isCrossingBase();
    void setCrossingBase(boolean crossingBase);
    NbtCompound getCustomData();
    int getTerrainHumidity();
    int getTerrainNutrients();
    int getTerrainAirQuality();
    @Deprecated World getWorld();
    @Deprecated BlockPos getLocation();
    int getLightLevel();
    boolean pick();
    boolean performManualHarvest();
    List<ItemStack> performHarvest();
    void reset();
    void updateState();
    boolean isBlockBelow(Block block);
    boolean isBlockBelow(String blockName);
    ItemStack generateSeeds(CropCard crop, int growth, int gain, int resistance, int scanLevel);
}
