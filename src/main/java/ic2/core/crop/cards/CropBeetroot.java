package ic2.core.crop.cards;

import ic2.api.crops.CropProperties;
import ic2.core.crop.CropVanilla;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/** Fabric port of CropBeetroot - the vanilla beetroot, four ages tall. */
public class CropBeetroot extends CropVanilla {
    public CropBeetroot() {
        super(3);
    }

    @Override public String getId() { return "beetroots"; }
    @Override public CropProperties getProperties() { return new CropProperties(1, 0, 4, 0, 1, 2); }
    @Override public String[] getAttributes() { return new String[]{"Red", "Food", "Beetroot"}; }
    @Override protected ItemStack getProduct() { return new ItemStack(Items.BEETROOT); }
    @Override protected ItemStack getSeeds() { return new ItemStack(Items.BEETROOT_SEEDS); }
}
