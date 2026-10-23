package ic2.core.crop.cards;

import ic2.api.crops.CropCard;
import ic2.api.crops.CropProperties;
import ic2.api.crops.ICropTile;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal weed card - used as fallback when crop tile is empty.
 */
public class CropWeed extends CropCard {

    @Override public String getId() { return "weed"; }
    @Override public String getOwner() { return "ic2"; }
    @Override public CropProperties getProperties() { return new CropProperties(0, 0, 0, 0, 0, 5); }
    @Override public String[] getAttributes() { return new String[]{"Weed"}; }
    @Override public int getMaxSize() { return 4; }

    @Override public boolean isWeed(ICropTile cropTile) { return true; }

    @Override public ItemStack getGain(ICropTile crop) { return ItemStack.EMPTY; }

    @Override public List<Identifier> getTexturesLocation() {
        List<Identifier> ret = new ArrayList<>(getMaxSize());
        for (int s = 1; s <= getMaxSize(); s++) ret.add(Identifier.of("ic2", "blocks/crop/weed_" + s));
        return ret;
    }
}
