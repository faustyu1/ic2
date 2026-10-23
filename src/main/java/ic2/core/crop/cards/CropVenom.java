package ic2.core.crop.cards;

import ic2.api.crops.CropCard;
import ic2.api.crops.CropProperties;
import ic2.api.crops.ICropTile;
import ic2.core.IC2;
import ic2.core.init.IC2ItemVariants;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Fabric port of CropVenomilia - the poison tulip.
 *
 * <p>Ripe at size 4 it gives purple dye; left to reach 5 it gives grin powder, but touching it
 * then poisons whoever did unless they sneak, and knocks it back to size 4.
 */
public class CropVenom extends CropCard {

    @Override public String getId() { return "venomilia"; }
    @Override public String getOwner() { return "ic2"; }
    @Override public String getDiscoveredBy() { return "raGan"; }
    @Override public CropProperties getProperties() { return new CropProperties(3, 3, 1, 3, 3, 3); }
    @Override public String[] getAttributes() { return new String[]{"Purple", "Flower", "Tulip", "Poison"}; }
    @Override public int getMaxSize() { return 6; }

    @Override public boolean canGrow(ICropTile crop) {
        return crop.getCurrentSize() <= 4 && crop.getLightLevel() >= 12 || crop.getCurrentSize() == 5;
    }

    @Override public boolean canBeHarvested(ICropTile crop) { return crop.getCurrentSize() >= 4; }
    @Override public int getOptimalHarvestSize(ICropTile crop) { return 4; }

    @Override public ItemStack getGain(ICropTile crop) {
        if (crop.getCurrentSize() == 5) {
            Item grinPowder = IC2ItemVariants.get("crop_res", "grin_powder");
            return grinPowder == null ? ItemStack.EMPTY : new ItemStack(grinPowder);
        }
        return crop.getCurrentSize() >= 4 ? new ItemStack(Items.PURPLE_DYE, 1) : ItemStack.EMPTY;
    }

    @Override public int getSizeAfterHarvest(ICropTile crop) { return 3; }

    @Override public int getGrowthDuration(ICropTile crop) {
        return crop.getCurrentSize() >= 3 ? 600 : 400;
    }

    @Override public boolean onRightClick(ICropTile crop, PlayerEntity player) {
        if (!player.isSneaking()) onEntityCollision(crop, player);
        return crop.performManualHarvest();
    }

    @Override public boolean onLeftClick(ICropTile crop, PlayerEntity player) {
        if (!player.isSneaking()) onEntityCollision(crop, player);
        return crop.pick();
    }

    @Override public boolean onEntityCollision(ICropTile crop, Entity entity) {
        if (crop.getCurrentSize() == 5 && entity instanceof LivingEntity living) {
            if (entity instanceof PlayerEntity p && p.isSneaking() && IC2.random.nextInt(50) != 0) {
                return super.onEntityCollision(crop, entity);
            }
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, (IC2.random.nextInt(10) + 5) * 20, 0));
            crop.setCurrentSize(4);
            crop.updateState();
        }
        return super.onEntityCollision(crop, entity);
    }

    @Override public boolean isWeed(ICropTile crop) {
        return crop.getCurrentSize() == 5 && crop.getStatGrowth() >= 8;
    }

    @Override public List<Identifier> getTexturesLocation() {
        List<Identifier> ret = new ArrayList<>(getMaxSize());
        for (int s = 1; s <= getMaxSize(); s++) ret.add(Identifier.of("ic2", "blocks/crop/venomilia_" + s));
        return ret;
    }
}
