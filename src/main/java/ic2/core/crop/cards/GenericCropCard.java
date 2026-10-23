package ic2.core.crop.cards;

import ic2.api.crops.CropCard;
import ic2.api.crops.CropProperties;
import ic2.api.crops.Crops;
import ic2.api.crops.ICropTile;
import ic2.core.IC2;
import ic2.core.crop.IC2CropCard;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

/**
 * Fabric port of GenericCropCard - a crop described entirely by numbers and drops.
 *
 * <p>Most of the crops IC2 added over the years differ only in what they drop and how tall they
 * grow, so they are built here with a chain of setters instead of a class each. {@link #register}
 * checks that the numbers make sense before the crop reaches the registry.
 */
public class GenericCropCard extends IC2CropCard {
    protected final String id;
    protected String owner = "ic2";
    protected String discoveredBy;
    protected CropProperties properties;
    protected String[] attributes;
    protected int maxSize;
    protected ItemStack[] drops;
    protected ItemStack[] specialDrops;
    protected int growthSpeed = 0;
    protected int harvestSize;
    protected int optimalHarvestSize;
    protected int afterHarvestSize;
    protected Object[] rootRequirements;
    protected final List<BaseSeed> baseSeeds = new ArrayList<>(0);

    protected GenericCropCard(String id) {
        this.id = id;
    }

    public static GenericCropCard create(String id) {
        return new GenericCropCard(id);
    }

    @Override public String getId() { return id; }
    @Override public String getOwner() { return owner; }
    @Override public String getUnlocalizedName() { return owner + ".crop." + id; }
    @Override public String getDiscoveredBy() { return discoveredBy; }
    @Override public int getRootsLength(ICropTile cropTile) { return 5; }
    @Override public CropProperties getProperties() { return properties; }
    @Override public String[] getAttributes() { return attributes; }
    @Override public int getMaxSize() { return maxSize; }

    /**
     * The harvest: the ordinary drops, plus one of the special drops now and then. The roulette
     * has twice as many empty places as special drops, so the rare one stays rare however many
     * of them a crop has.
     */
    @Override
    public ItemStack[] getGains(ICropTile crop) {
        if (drops == null || drops.length == 0) {
            return new ItemStack[0];
        }

        ItemStack[] gains = copyOf(drops);
        if (specialDrops != null && specialDrops.length > 0) {
            int roulette = IC2.random.nextInt(specialDrops.length * 2 + 2);
            if (roulette < specialDrops.length && !specialDrops[roulette].isEmpty()) {
                gains = Arrays.copyOf(gains, gains.length + 1);
                gains[gains.length - 1] = specialDrops[roulette].copy();
            }
        }
        return gains;
    }

    @Override
    public int getGrowthDuration(ICropTile cropTile) {
        return growthSpeed < 200 ? properties.getTier() * 200 : properties.getTier() * growthSpeed;
    }

    @Override
    public boolean canCross(ICropTile cropTile) {
        return cropTile.getCurrentSize() + 2 > getMaxSize();
    }

    @Override
    public boolean canGrow(ICropTile crop) {
        if (rootRequirements != null && rootRequirements.length > 0 && crop.getCurrentSize() == maxSize - 1) {
            for (Object requirement : rootRequirements) {
                if (requirement instanceof String name && crop.isBlockBelow(name)) {
                    return true;
                }
                if (requirement instanceof Block block && crop.isBlockBelow(block)) {
                    return true;
                }
            }
            return false;
        }
        return crop.getCurrentSize() < maxSize;
    }

    @Override public boolean canBeHarvested(ICropTile cropTile) { return cropTile.getCurrentSize() >= harvestSize; }
    @Override public int getOptimalHarvestSize(ICropTile cropTile) { return optimalHarvestSize; }
    @Override public int getSizeAfterHarvest(ICropTile cropTile) { return afterHarvestSize; }

    @Override
    public List<Identifier> getTexturesLocation() {
        List<Identifier> ret = new ArrayList<>(getMaxSize());
        for (int size = 1; size <= getMaxSize(); size++) {
            ret.add(Identifier.of(owner, "blocks/crop/" + id + "_" + size));
        }
        return ret;
    }

    @Override
    public boolean onRightClick(ICropTile cropTile, PlayerEntity player) {
        return canBeHarvested(cropTile) && cropTile.performManualHarvest();
    }

    /** What the crop's tooltip says about the block it needs under it, if any. */
    public List<String> getInformation() {
        if (rootRequirements == null || rootRequirements.length == 0) {
            return new ArrayList<>();
        }

        List<String> info = new ArrayList<>();
        info.add("");
        info.add("Requires roots");
        info.add(" Requires a specific block underneath to reach full growth");
        info.add(" Roots length: " + getRootsLength(null));
        info.add(" Roots accepted: ");

        for (Object requirement : rootRequirements) {
            if (requirement instanceof String name) {
                info.add("  " + name);
            } else if (requirement instanceof ItemStack stack) {
                info.add("  " + stack.getName().getString());
            } else if (requirement instanceof Block block) {
                info.add("  " + block.getName().getString());
            }
        }
        return info;
    }

    public GenericCropCard register() {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("The id must not be null or empty!");
        }
        if (owner == null || owner.isBlank()) {
            throw new IllegalArgumentException("The owner must not be null or empty!");
        }
        if (discoveredBy == null || discoveredBy.isBlank()) {
            throw new IllegalArgumentException("The discoveredBy must not be null or empty!");
        }
        if (properties == null) {
            throw new IllegalArgumentException("The properties must not be null!");
        }
        if (maxSize < 3) {
            throw new IllegalArgumentException("The maxSize must be at least 3!");
        }

        if (harvestSize < 2) {
            harvestSize = maxSize;
        }
        if (optimalHarvestSize < 2) {
            optimalHarvestSize = harvestSize;
        }
        if (afterHarvestSize < 1) {
            throw new IllegalArgumentException("The afterHarvestSize must be at least 1!");
        }

        Crops.instance.registerCrop(this);
        for (BaseSeed seed : baseSeeds) {
            Crops.instance.registerBaseSeed(seed.seed, this, seed.size, seed.growth, seed.gain, seed.resistance);
        }
        return this;
    }

    public GenericCropCard addBaseSeed(ItemStack seed) {
        baseSeeds.add(new BaseSeed(seed));
        return this;
    }

    public GenericCropCard addBaseSeed(ItemStack seed, int size, int growth, int gain, int resistance) {
        baseSeeds.add(new BaseSeed(seed, size, growth, gain, resistance));
        return this;
    }

    public GenericCropCard setOwner(String owner) { this.owner = owner; return this; }
    public GenericCropCard setDiscoveredBy(String discoveredBy) { this.discoveredBy = discoveredBy; return this; }
    public GenericCropCard setProperties(CropProperties properties) { this.properties = properties; return this; }
    public GenericCropCard setAttributes(String[] attributes) { this.attributes = attributes; return this; }
    public GenericCropCard setMaxSize(int maxSize) { this.maxSize = maxSize; return this; }
    public GenericCropCard setDrops(ItemStack drop) { this.drops = new ItemStack[]{drop.copy()}; return this; }
    public GenericCropCard setDrops(ItemStack[] drops) { this.drops = copyOf(drops); return this; }
    public GenericCropCard setSpecialDrops(ItemStack specialDrop) { this.specialDrops = new ItemStack[]{specialDrop.copy()}; return this; }
    public GenericCropCard setSpecialDrops(ItemStack[] specialDrops) { this.specialDrops = copyOf(specialDrops); return this; }
    public GenericCropCard setGrowthSpeed(int growthSpeed) { this.growthSpeed = growthSpeed; return this; }
    public GenericCropCard setHarvestSize(int harvestSize) { this.harvestSize = harvestSize; return this; }
    public GenericCropCard setOptimalHarvestSize(int size) { this.optimalHarvestSize = size; return this; }
    public GenericCropCard setAfterHarvestSize(int size) { this.afterHarvestSize = size; return this; }
    public GenericCropCard setRootRequirements(Object[] rootRequirements) { this.rootRequirements = rootRequirements; return this; }

    /** Copies the drops and drops the empty entries, which a missing item leaves behind. */
    private static ItemStack[] copyOf(ItemStack[] array) {
        List<ItemStack> ret = new ArrayList<>(array.length);
        for (ItemStack stack : array) {
            if (stack != null && !stack.isEmpty()) {
                ret.add(stack.copy());
            }
        }
        return ret.toArray(new ItemStack[0]);
    }

    private static final class BaseSeed {
        private final ItemStack seed;
        private final int size;
        private final int growth;
        private final int gain;
        private final int resistance;

        BaseSeed(ItemStack seed) {
            this(seed, 1, 1, 1, 1);
        }

        BaseSeed(ItemStack seed, int size, int growth, int gain, int resistance) {
            this.seed = seed;
            this.size = size;
            this.growth = growth;
            this.gain = gain;
            this.resistance = resistance;
        }
    }
}
