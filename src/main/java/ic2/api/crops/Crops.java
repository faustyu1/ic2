package ic2.api.crops;

import java.util.Collection;
import java.util.Map;
import net.minecraft.client.texture.Sprite;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.fml.common.eventhandler.Event;

public abstract class Crops {
    public static Crops instance;
    public static CropCard weed;

    public abstract void addBiomenutrientsBonus(BiomeDictionary.Type type, int bonus);
    public abstract void addBiomehumidityBonus(BiomeDictionary.Type type, int bonus);
    public abstract int getHumidityBiomeBonus(RegistryEntry<Biome> biome);
    public abstract int getNutrientBiomeBonus(RegistryEntry<Biome> biome);
    public abstract CropCard getCropCard(String owner, String id);
    public abstract CropCard getCropCard(ItemStack stack);
    public abstract Collection<CropCard> getCrops();
    public abstract void registerCrop(CropCard card);
    public abstract void registerCropTextures(Map<Identifier, Sprite> textures);
    public abstract boolean registerBaseSeed(ItemStack stack, CropCard crop, int size, int growth, int gain, int resistance);
    public abstract BaseSeed getBaseSeed(ItemStack stack);

    public static class CropRegisterEvent extends Event {
        public void register(CropCard crop) { Crops.instance.registerCrop(crop); }
        public void register(CropCard... crops) { for (CropCard c : crops) register(c); }
    }
}
