package ic2.core.crop.cards;

import ic2.api.crops.CropCard;
import ic2.api.crops.CropProperties;
import ic2.api.crops.ICropTile;
import ic2.core.init.IC2Items;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class CropStickReed extends CropCard {

    @Override public String getId() { return "stickreed"; }
    @Override public String getOwner() { return "ic2"; }
    @Override public String getDiscoveredBy() { return "raa1337"; }
    @Override public CropProperties getProperties() { return new CropProperties(4, 2, 0, 1, 0, 1); }
    @Override public String[] getAttributes() { return new String[]{"Reed", "Resin"}; }
    @Override public int getMaxSize() { return 4; }

    @Override public boolean canGrow(ICropTile crop) { return crop.getCurrentSize() < 4; }

    @Override public int getWeightInfluences(ICropTile crop, int humidity, int nutrients, int air) {
        return (int) ((double) humidity * 1.2 + (double) nutrients + (double) air * 0.8);
    }

    @Override public boolean canBeHarvested(ICropTile crop) { return crop.getCurrentSize() > 1; }
    @Override public int getOptimalHarvestSize(ICropTile crop) { return 4; }

    @Override public ItemStack getGain(ICropTile crop) {
        if (crop.getCurrentSize() <= 3) {
            int count = Math.max(1, crop.getCurrentSize() - 1);
            return new ItemStack(Items.SUGAR_CANE, count);
        }
        if (IC2Items.RESIN != null) return new ItemStack(IC2Items.RESIN, 1);
        return new ItemStack(Items.SLIME_BALL, 1);
    }

    @Override public int getSizeAfterHarvest(ICropTile crop) {
        if (crop.getCurrentSize() == 4) {
            int rnd = 0;
            try {
                if (crop.getWorldObj() != null) rnd = crop.getWorldObj().getRandom().nextInt(3);
                else rnd = new java.util.Random().nextInt(3);
            } catch (Exception e) { rnd = new java.util.Random().nextInt(3); }
            return 3 - rnd;
        }
        return 1;
    }

    @Override public boolean onEntityCollision(ICropTile crop, Entity entity) { return false; }

    @Override public int getGrowthDuration(ICropTile crop) {
        return crop.getCurrentSize() == 4 ? 400 : 100;
    }

    @Override public List<Identifier> getTexturesLocation() {
        List<Identifier> ret = new ArrayList<>(getMaxSize());
        for (int s = 1; s <= getMaxSize(); s++) ret.add(Identifier.of("ic2", "blocks/crop/stickreed_" + s));
        return ret;
    }
}
