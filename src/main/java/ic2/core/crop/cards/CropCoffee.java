package ic2.core.crop.cards;

import ic2.api.crops.CropCard;
import ic2.api.crops.CropProperties;
import ic2.api.crops.ICropTile;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class CropCoffee extends CropCard {
    @Override public String getId() { return "coffee"; }
    @Override public String getOwner() { return "ic2"; }
    @Override public String getDiscoveredBy() { return "Snoochy"; }
    @Override public CropProperties getProperties() { return new CropProperties(3, 1, 2, 0, 0, 1); }
    @Override public String[] getAttributes() { return new String[]{"Leaves", "Beans", "Ingredient"}; }
    @Override public int getMaxSize() { return 6; }
    @Override public boolean canGrow(ICropTile c) { return c.getCurrentSize() < getMaxSize() && c.getLightLevel() >= 9; }
    @Override public int getOptimalHarvestSize(ICropTile c) { return getMaxSize(); }
    @Override public ItemStack getGain(ICropTile c) { return new ItemStack(Items.COCOA_BEANS); }
    @Override public int getSizeAfterHarvest(ICropTile c) { return 3; }
    @Override public List<Identifier> getTexturesLocation() { List<Identifier> r=new ArrayList<>(getMaxSize()); for(int i=1;i<=getMaxSize();i++) r.add(Identifier.of("ic2","blocks/crop/coffee_"+i)); return r; }
}
