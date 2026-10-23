package ic2.core.crop.cards;

import ic2.api.crops.ICropTile;
import ic2.api.crops.CropProperties;
import ic2.core.IC2;
import ic2.core.crop.CropVanillaStem;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/** Fabric port of CropMelon - usually a handful of slices, now and then the whole melon. */
public class CropMelon extends CropVanillaStem {
    public CropMelon() {
        super(4);
    }

    @Override public String getId() { return "melon"; }
    @Override public String getDiscoveredBy() { return "Chao"; }
    @Override public CropProperties getProperties() { return new CropProperties(2, 0, 4, 0, 2, 0); }
    @Override public String[] getAttributes() { return new String[]{"Green", "Food", "Stem"}; }

    @Override
    protected ItemStack getProduct() {
        return IC2.random.nextInt(3) == 0
                ? new ItemStack(Blocks.MELON)
                : new ItemStack(Items.MELON_SLICE, IC2.random.nextInt(4) + 2);
    }

    @Override
    protected ItemStack getSeeds() {
        return new ItemStack(Items.MELON_SEEDS, IC2.random.nextInt(2) + 1);
    }

    @Override public int getGrowthDuration(ICropTile crop) { return crop.getCurrentSize() == 3 ? 700 : 250; }
}
