package ic2.core.crop.cards;

import ic2.api.crops.CropProperties;
import ic2.core.crop.CropVanilla;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/** Fabric port of CropCarrots - the carrot, which is its own seed. */
public class CropCarrots extends CropVanilla {
    public CropCarrots() {
        super(3);
    }

    @Override public String getId() { return "carrots"; }
    @Override public CropProperties getProperties() { return new CropProperties(2, 0, 4, 0, 0, 2); }
    @Override public String[] getAttributes() { return new String[]{"Orange", "Food", "Carrots"}; }
    @Override protected ItemStack getProduct() { return new ItemStack(Items.CARROT); }
    @Override protected ItemStack getSeeds() { return new ItemStack(Items.CARROT); }
}
