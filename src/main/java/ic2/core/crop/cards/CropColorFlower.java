package ic2.core.crop.cards;

import ic2.api.crops.CropProperties;
import ic2.api.crops.ICropTile;
import ic2.core.crop.IC2CropCard;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.DyeColor;

/**
 * Fabric port of CropColorFlower - the five flowers that are farmed for their dye.
 *
 * <p>In 1.12.2 the dye was one item with a metadata; after flattening each colour is its own
 * item, so the card carries the colour rather than the metadata number.
 */
public class CropColorFlower extends IC2CropCard {
    private final String name;
    private final String[] attributes;
    private final DyeColor color;

    public CropColorFlower(String name, String[] attributes, DyeColor color) {
        this.name = name;
        this.attributes = attributes;
        this.color = color;
    }

    @Override
    public String getDiscoveredBy() {
        return !name.equals("dandelion") && !name.equals("rose") ? "Alblaka" : "Notch";
    }

    @Override public String getId() { return name; }
    @Override public CropProperties getProperties() { return new CropProperties(2, 1, 1, 0, 5, 1); }
    @Override public String[] getAttributes() { return attributes; }
    @Override public int getMaxSize() { return 4; }

    @Override
    public boolean canGrow(ICropTile crop) {
        return crop.getCurrentSize() <= 3 && crop.getLightLevel() >= 12;
    }

    @Override public boolean canBeHarvested(ICropTile crop) { return crop.getCurrentSize() == 4; }
    @Override public int getOptimalHarvestSize(ICropTile crop) { return 4; }

    @Override
    public ItemStack getGain(ICropTile crop) {
        return new ItemStack(dye(color));
    }

    @Override public int getSizeAfterHarvest(ICropTile crop) { return 3; }
    @Override public int getGrowthDuration(ICropTile crop) { return crop.getCurrentSize() == 3 ? 600 : 400; }

    private static net.minecraft.item.Item dye(DyeColor color) {
        return switch (color) {
            case WHITE -> Items.WHITE_DYE;
            case ORANGE -> Items.ORANGE_DYE;
            case MAGENTA -> Items.MAGENTA_DYE;
            case LIGHT_BLUE -> Items.LIGHT_BLUE_DYE;
            case YELLOW -> Items.YELLOW_DYE;
            case LIME -> Items.LIME_DYE;
            case PINK -> Items.PINK_DYE;
            case GRAY -> Items.GRAY_DYE;
            case LIGHT_GRAY -> Items.LIGHT_GRAY_DYE;
            case CYAN -> Items.CYAN_DYE;
            case PURPLE -> Items.PURPLE_DYE;
            case BLUE -> Items.BLUE_DYE;
            case BROWN -> Items.BROWN_DYE;
            case GREEN -> Items.GREEN_DYE;
            case RED -> Items.RED_DYE;
            case BLACK -> Items.BLACK_DYE;
        };
    }
}
