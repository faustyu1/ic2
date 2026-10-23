package ic2.core.crop.cards;

import ic2.api.crops.CropCard;
import ic2.api.crops.CropProperties;
import ic2.api.crops.ICropTile;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import java.util.ArrayList;
import java.util.List;

public class CropNetherWart extends CropCard {
    @Override public String getId() { return "nether_wart"; }
    @Override public String getOwner() { return "ic2"; }
    @Override public String getDiscoveredBy() { return "IC2"; }
    @Override public CropProperties getProperties() { return new CropProperties(2, 0, 2, 0, 0, 1); }
    @Override public String[] getAttributes() { return new String[]{"nether_wart"}; }
    @Override public int getMaxSize() { return 5; }
    @Override public boolean canGrow(ICropTile c) { return c.getCurrentSize() < getMaxSize(); }
    @Override public int getOptimalHarvestSize(ICropTile c) { return getMaxSize(); }
    @Override public ItemStack getGain(ICropTile c) { return new ItemStack(net.minecraft.item.Items.NETHER_WART); }
    @Override public int getSizeAfterHarvest(ICropTile c) { return 1; }
    @Override public List<Identifier> getTexturesLocation() { List<Identifier> r=new ArrayList<>(getMaxSize()); for(int i=1;i<=getMaxSize();i++) r.add(Identifier.of("ic2","blocks/crop/nether_wart_"+i)); return r; }
}
