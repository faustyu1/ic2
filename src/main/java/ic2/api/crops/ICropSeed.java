package ic2.api.crops;

import net.minecraft.item.ItemStack;

public interface ICropSeed {
    CropCard getCropFromStack(ItemStack stack);
    void setCropFromStack(ItemStack stack, CropCard crop);
    int getGrowthFromStack(ItemStack stack);
    void setGrowthFromStack(ItemStack stack, int growth);
    int getGainFromStack(ItemStack stack);
    void setGainFromStack(ItemStack stack, int gain);
    int getResistanceFromStack(ItemStack stack);
    void setResistanceFromStack(ItemStack stack, int resistance);
    int getScannedFromStack(ItemStack stack);
    void setScannedFromStack(ItemStack stack, int scanned);
    void incrementScannedFromStack(ItemStack stack);
}
