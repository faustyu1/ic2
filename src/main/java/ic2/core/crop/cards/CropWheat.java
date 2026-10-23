package ic2.core.crop.cards;

import ic2.api.crops.CropProperties;
import ic2.api.crops.ICropTile;
import ic2.core.crop.CropVanilla;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/** Fabric port of CropWheat - vanilla wheat, which an unbred plant gives back as wheat seeds. */
public class CropWheat extends CropVanilla {
    public CropWheat() {
        super(7);
    }

    @Override public String getId() { return "wheat"; }
    @Override public CropProperties getProperties() { return new CropProperties(1, 0, 4, 0, 0, 2); }
    @Override public String[] getAttributes() { return new String[]{"Yellow", "Food", "Wheat"}; }
    @Override protected ItemStack getProduct() { return new ItemStack(Items.WHEAT, 1); }
    @Override protected ItemStack getSeeds() { return new ItemStack(Items.WHEAT_SEEDS); }
    @Override public int getSizeAfterHarvest(ICropTile crop) { return 2; }
}
