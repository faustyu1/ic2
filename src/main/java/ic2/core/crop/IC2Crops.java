package ic2.core.crop;

import ic2.api.crops.BaseSeed;
import ic2.api.crops.CropCard;
import ic2.api.crops.CropProperties;
import ic2.api.crops.Crops;
import ic2.core.crop.cards.*;
import ic2.core.init.IC2ItemVariants;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.Blocks;
import net.minecraft.client.texture.Sprite;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;

/**
 * Fabric port of ic2.core.crop.IC2Crops - the crop registry and the crops IC2 ships.
 *
 * <p>The registry itself is {@link CropManager}; this class is the {@link Crops} face of it that
 * the API and other mods use, and it holds the biome bonuses and the base seeds - the vanilla
 * items that can be planted straight into a crop stick.
 */
public class IC2Crops extends Crops {
    private final Map<BiomeDictionary.Type, Integer> humidityBiomeTypeBonus = new EnumMap<>(BiomeDictionary.Type.class);
    private final Map<BiomeDictionary.Type, Integer> nutrientBiomeTypeBonus = new EnumMap<>(BiomeDictionary.Type.class);
    private final Map<Item, BaseSeed> baseSeeds = new HashMap<>();
    private Map<Identifier, Sprite> textures = Map.of();

    // ---------------------------------------------------------------- the crops themselves

    public static final CropCard cropWheat = new CropWheat();
    public static final CropCard cropPumpkin = new CropPumpkin();
    public static final CropCard cropMelon = new CropMelon();
    public static final CropCard cropYellowFlower = new CropColorFlower("dandelion", new String[]{"Yellow", "Flower"}, DyeColor.YELLOW);
    public static final CropCard cropRedFlower = new CropColorFlower("rose", new String[]{"Red", "Flower", "Rose"}, DyeColor.RED);
    public static final CropCard cropBlackFlower = new CropColorFlower("blackthorn", new String[]{"Black", "Flower", "Rose"}, DyeColor.BLACK);
    public static final CropCard cropPurpleFlower = new CropColorFlower("tulip", new String[]{"Purple", "Flower", "Tulip"}, DyeColor.PURPLE);
    public static final CropCard cropBlueFlower = new CropColorFlower("cyazint", new String[]{"Blue", "Flower"}, DyeColor.LIGHT_BLUE);
    public static final CropCard cropVenomilia = new CropVenom();
    public static final CropCard cropReed = new CropReed();
    public static final CropCard cropStickReed = new CropStickReed();
    public static final CropCard cropCocoa = new CropCocoa();
    public static final CropCard cropFlax = new CropFlax();
    public static final CropCard cropRedMushroom = new CropBaseMushroom("red_mushroom",
            new String[]{"Red", "Food", "Mushroom"}, new ItemStack(Blocks.RED_MUSHROOM));
    public static final CropCard cropBrownMushroom = new CropBaseMushroom("brown_mushroom",
            new String[]{"Brown", "Food", "Mushroom"}, new ItemStack(Blocks.BROWN_MUSHROOM));
    public static final CropCard cropNetherWart = new CropNetherWart();
    public static final CropCard cropTerraWart = new CropTerraWart();
    public static final CropCard cropOakSapling = sapling("oak_sapling", Blocks.OAK_LEAVES, Blocks.OAK_SAPLING);
    public static final CropCard cropSpruceSapling = sapling("spruce_sapling", Blocks.SPRUCE_LEAVES, Blocks.SPRUCE_SAPLING);
    public static final CropCard cropBirchSapling = sapling("birch_sapling", Blocks.BIRCH_LEAVES, Blocks.BIRCH_SAPLING);
    public static final CropCard cropJungleSapling = sapling("jungle_sapling", Blocks.JUNGLE_LEAVES, Blocks.JUNGLE_SAPLING);
    public static final CropCard cropAcaciaSapling = sapling("acacia_sapling", Blocks.ACACIA_LEAVES, Blocks.ACACIA_SAPLING);
    public static final CropCard cropDarkOakSapling = sapling("dark_oak_sapling", Blocks.DARK_OAK_LEAVES, Blocks.DARK_OAK_SAPLING);
    public static final CropCard cropFerru = new CropBaseMetalCommon("ferru", new String[]{"Metal", "Iron", "Shiny"},
            new String[]{"oreIron", "blockIron"}, new ItemStack(Items.IRON_NUGGET));
    public static final CropCard cropCyprium = new CropBaseMetalCommon("cyprium", new String[]{"Metal", "Copper", "Shiny"},
            new String[]{"oreCopper", "blockCopper"}, ic2Item("dust", "copper"));
    public static final CropCard cropStagnium = new CropBaseMetalCommon("stagnium", new String[]{"Metal", "Tin", "Shiny"},
            new String[]{"oreTin", "blockTin"}, ic2Item("dust", "tin"));
    public static final CropCard cropPlumbiscus = new CropBaseMetalCommon("plumbiscus", new String[]{"Metal", "Lead", "Shiny"},
            new String[]{"oreLead", "blockLead"}, ic2Item("dust", "lead"));
    public static final CropCard cropAurelia = new CropBaseMetalUncommon("aurelia", new String[]{"Metal", "Gold", "Shiny"},
            new String[]{"oreGold", "blockGold"}, new ItemStack(Items.GOLD_NUGGET));
    public static final CropCard cropShining = new CropBaseMetalUncommon("shining", new String[]{"Metal", "Silver", "Shiny"},
            new String[]{"oreSilver", "blockSilver"}, ic2Item("dust", "silver"));
    public static final CropCard cropRedwheat = new CropRedWheat();
    public static final CropCard cropCoffee = new CropCoffee();
    public static final CropCard cropHops = new CropHops();
    public static final CropCard cropCarrots = new CropCarrots();
    public static final CropCard cropPotato = new CropPotato();
    public static final CropCard cropEatingPlant = new CropEating();
    public static final CropCard cropBeetroot = new CropBeetroot();

    /** Registers the registry itself, the biome bonuses, every crop and the base seeds. */
    public static void init() {
        if (Crops.instance != null) {
            return;
        }

        Crops.instance = new IC2Crops();
        Crops.weed = new CropWeed();

        Crops.instance.addBiomehumidityBonus(BiomeDictionary.Type.WATER, 10);
        Crops.instance.addBiomehumidityBonus(BiomeDictionary.Type.WET, 10);
        Crops.instance.addBiomehumidityBonus(BiomeDictionary.Type.DRY, -10);
        Crops.instance.addBiomenutrientsBonus(BiomeDictionary.Type.JUNGLE, 10);
        Crops.instance.addBiomenutrientsBonus(BiomeDictionary.Type.SWAMP, 10);
        Crops.instance.addBiomenutrientsBonus(BiomeDictionary.Type.MUSHROOM, 5);
        Crops.instance.addBiomenutrientsBonus(BiomeDictionary.Type.FOREST, 5);
        Crops.instance.addBiomenutrientsBonus(BiomeDictionary.Type.RIVER, 2);
        Crops.instance.addBiomenutrientsBonus(BiomeDictionary.Type.PLAINS, 0);
        Crops.instance.addBiomenutrientsBonus(BiomeDictionary.Type.SAVANNA, -2);
        Crops.instance.addBiomenutrientsBonus(BiomeDictionary.Type.HILLS, -5);
        Crops.instance.addBiomenutrientsBonus(BiomeDictionary.Type.MOUNTAIN, -5);
        Crops.instance.addBiomenutrientsBonus(BiomeDictionary.Type.WASTELAND, -8);
        Crops.instance.addBiomenutrientsBonus(BiomeDictionary.Type.END, -10);
        Crops.instance.addBiomenutrientsBonus(BiomeDictionary.Type.NETHER, -10);
        Crops.instance.addBiomenutrientsBonus(BiomeDictionary.Type.DEAD, -10);

        registerCrops();
        registerBaseSeeds();
    }

    public static void registerCrops() {
        Crops.instance.registerCrop(Crops.weed);
        Crops.instance.registerCrop(cropWheat);
        Crops.instance.registerCrop(cropPumpkin);
        Crops.instance.registerCrop(cropMelon);
        Crops.instance.registerCrop(cropYellowFlower);
        Crops.instance.registerCrop(cropRedFlower);
        Crops.instance.registerCrop(cropBlackFlower);
        Crops.instance.registerCrop(cropPurpleFlower);
        Crops.instance.registerCrop(cropBlueFlower);
        Crops.instance.registerCrop(cropVenomilia);
        Crops.instance.registerCrop(cropReed);
        Crops.instance.registerCrop(cropStickReed);
        Crops.instance.registerCrop(cropCocoa);
        Crops.instance.registerCrop(cropFlax);
        Crops.instance.registerCrop(cropFerru);
        Crops.instance.registerCrop(cropAurelia);
        Crops.instance.registerCrop(cropRedwheat);
        Crops.instance.registerCrop(cropNetherWart);
        Crops.instance.registerCrop(cropTerraWart);
        Crops.instance.registerCrop(cropCoffee);
        Crops.instance.registerCrop(cropHops);
        Crops.instance.registerCrop(cropCarrots);
        Crops.instance.registerCrop(cropPotato);
        Crops.instance.registerCrop(cropRedMushroom);
        Crops.instance.registerCrop(cropBrownMushroom);
        Crops.instance.registerCrop(cropEatingPlant);
        Crops.instance.registerCrop(cropCyprium);
        Crops.instance.registerCrop(cropStagnium);
        Crops.instance.registerCrop(cropPlumbiscus);
        Crops.instance.registerCrop(cropShining);
        Crops.instance.registerCrop(cropBeetroot);
        Crops.instance.registerCrop(cropOakSapling);
        Crops.instance.registerCrop(cropSpruceSapling);
        Crops.instance.registerCrop(cropBirchSapling);
        Crops.instance.registerCrop(cropJungleSapling);
        Crops.instance.registerCrop(cropAcaciaSapling);
        Crops.instance.registerCrop(cropDarkOakSapling);

        registerGenericCrops();
    }

    /** The crops that are nothing but numbers and drops; the original built them the same way. */
    private static void registerGenericCrops() {
        GenericCropCard.create("blazereed")
                .setDiscoveredBy("Mr. Brain")
                .setProperties(new CropProperties(6, 0, 4, 1, 0, 0))
                .setAttributes(new String[]{"Fire", "Blaze", "Reed", "Sulfur"})
                .setMaxSize(4)
                .setDrops(new ItemStack(Items.BLAZE_POWDER))
                .setSpecialDrops(new ItemStack[]{new ItemStack(Items.BLAZE_ROD), ic2Item("dust", "sulfur")})
                .setAfterHarvestSize(1)
                .register();
        GenericCropCard.create("bobs_yer_uncle_ranks_berries")
                .setDiscoveredBy("GenerikB")
                .setProperties(new CropProperties(11, 4, 0, 8, 2, 9))
                .setAttributes(new String[]{"Shiny", "Vine", "Emerald", "Berylium", "Crystal"})
                .setMaxSize(4)
                .setDrops(ic2Item("crop_res", "bobs_yer_uncle_ranks_berry"))
                .setSpecialDrops(new ItemStack[]{new ItemStack(Items.EMERALD)})
                .setAfterHarvestSize(1)
                .register();
        GenericCropCard.create("corium")
                .setDiscoveredBy("Gregorius Techneticies")
                .setProperties(new CropProperties(6, 0, 2, 3, 1, 0))
                .setAttributes(new String[]{"Cow", "Silk", "Vine"})
                .setMaxSize(4)
                .setDrops(new ItemStack(Items.LEATHER))
                .setAfterHarvestSize(1)
                .register();
        GenericCropCard.create("corpse_plant")
                .setDiscoveredBy("Mr. Kenny")
                .setProperties(new CropProperties(5, 0, 2, 1, 0, 3))
                .setAttributes(new String[]{"Toxic", "Undead", "Vine", "Edible", "Rotten"})
                .setMaxSize(4)
                .setDrops(new ItemStack(Items.ROTTEN_FLESH))
                .setSpecialDrops(new ItemStack[]{new ItemStack(Items.BONE),
                        new ItemStack(Items.BLACK_DYE), new ItemStack(Items.BLACK_DYE)})
                .setAfterHarvestSize(1)
                .register();
        GenericCropCard.create("creeper_weed")
                .setDiscoveredBy("General Spaz")
                .setProperties(new CropProperties(7, 3, 0, 5, 1, 3))
                .setAttributes(new String[]{"Creeper", "Vine", "Explosive", "Fire", "Sulfur", "Saltpeter", "Coal"})
                .setMaxSize(4)
                .setDrops(new ItemStack(Items.GUNPOWDER))
                .setAfterHarvestSize(1)
                .register();
        GenericCropCard.create("diareed")
                .setDiscoveredBy("Diareed")
                .setProperties(new CropProperties(12, 5, 0, 10, 2, 10))
                .setAttributes(new String[]{"Fire", "Shiny", "Reed", "Coal", "Diamond", "Crystal"})
                .setMaxSize(4)
                .setDrops(ic2Item("dust", "small_diamond"))
                .setSpecialDrops(new ItemStack[]{new ItemStack(Items.DIAMOND)})
                .setAfterHarvestSize(1)
                .register();
        GenericCropCard.create("egg_plant")
                .setDiscoveredBy("Link")
                .setProperties(new CropProperties(6, 0, 4, 1, 0, 0))
                .setAttributes(new String[]{"Chicken", "Egg", "Edible", "Feather", "Flower", "Addictive"})
                .setMaxSize(3)
                .setDrops(new ItemStack(Items.EGG))
                .setSpecialDrops(new ItemStack[]{new ItemStack(Items.CHICKEN), new ItemStack(Items.FEATHER),
                        new ItemStack(Items.FEATHER), new ItemStack(Items.FEATHER)})
                .setGrowthSpeed(900)
                .setAfterHarvestSize(2)
                .register();
        GenericCropCard.create("ender_blossom")
                .setDiscoveredBy("RichardG")
                .setProperties(new CropProperties(10, 5, 0, 2, 1, 6))
                .setAttributes(new String[]{"Ender", "Flower", "Shiny"})
                .setMaxSize(4)
                .setDrops(ic2Item("dust", "ender_pearl"))
                .setSpecialDrops(new ItemStack[]{new ItemStack(Items.ENDER_PEARL),
                        new ItemStack(Items.ENDER_PEARL), new ItemStack(Items.ENDER_EYE)})
                .setAfterHarvestSize(1)
                .register();
        GenericCropCard.create("meat_rose")
                .setDiscoveredBy("VintageBeef")
                .setProperties(new CropProperties(7, 0, 4, 1, 3, 0))
                .setAttributes(new String[]{"Edible", "Flower", "Cow", "Chicken", "Pig", "Sheep"})
                .setMaxSize(4)
                .setDrops(new ItemStack(Items.PINK_DYE))
                .setSpecialDrops(new ItemStack[]{new ItemStack(Items.BEEF), new ItemStack(Items.PORKCHOP),
                        new ItemStack(Items.CHICKEN), new ItemStack(Items.MUTTON)})
                .setGrowthSpeed(1500)
                .setAfterHarvestSize(1)
                .register();
        GenericCropCard.create("milk_wart")
                .setDiscoveredBy("Mr. Brain")
                .setProperties(new CropProperties(6, 0, 3, 0, 1, 0))
                .setAttributes(new String[]{"Edible", "Milk", "Cow"})
                .setMaxSize(3)
                .setDrops(ic2Item("crop_res", "milk_wart"))
                .setGrowthSpeed(900)
                .setAfterHarvestSize(1)
                .addBaseSeed(ic2Item("crop_res", "milk_wart"))
                .register();
        GenericCropCard.create("oil_berries")
                .setDiscoveredBy("Spacetoad")
                .setProperties(new CropProperties(9, 6, 1, 2, 1, 12))
                .setAttributes(new String[]{"Fire", "Dark", "Reed", "Rotten", "Coal", "Oil"})
                .setMaxSize(3)
                .setDrops(ic2Item("crop_res", "oil_berry"))
                .setAfterHarvestSize(1)
                .register();
        GenericCropCard.create("slime_plant")
                .setDiscoveredBy("Neowulf")
                .setProperties(new CropProperties(6, 3, 0, 0, 0, 2))
                .setAttributes(new String[]{"Slime", "Bouncy", "Sticky", "Bush"})
                .setMaxSize(4)
                .setDrops(new ItemStack(Items.SLIME_BALL))
                .setAfterHarvestSize(3)
                .register();
        GenericCropCard.create("spidernip")
                .setDiscoveredBy("Mr. Kenny")
                .setProperties(new CropProperties(4, 2, 1, 4, 1, 3))
                .setAttributes(new String[]{"Toxic", "Silk", "Spider", "Flower", "Ingredient", "Addictive"})
                .setMaxSize(4)
                .setDrops(new ItemStack(Items.STRING))
                .setSpecialDrops(new ItemStack[]{new ItemStack(Items.SPIDER_EYE), new ItemStack(Blocks.COBWEB)})
                .setGrowthSpeed(600)
                .setAfterHarvestSize(1)
                .register();
        GenericCropCard.create("tearstalks")
                .setDiscoveredBy("Neowulf")
                .setProperties(new CropProperties(8, 1, 2, 0, 0, 0))
                .setAttributes(new String[]{"Healing", "Nether", "Ingredient", "Reed", "Ghast"})
                .setMaxSize(4)
                .setDrops(new ItemStack(Items.GHAST_TEAR))
                .setAfterHarvestSize(1)
                .register();
        GenericCropCard.create("withereed")
                .setDiscoveredBy("CovertJaguar")
                .setProperties(new CropProperties(8, 2, 0, 4, 1, 3))
                .setAttributes(new String[]{"Fire", "Undead", "Reed", "Coal", "Rotten", "Wither"})
                .setMaxSize(4)
                .setDrops(ic2Item("dust", "coal"))
                .setSpecialDrops(new ItemStack[]{new ItemStack(Items.COAL), new ItemStack(Items.COAL)})
                .setAfterHarvestSize(1)
                .register();
    }

    /** The items that can be put into a crop stick directly, without a seed bag. */
    public static void registerBaseSeeds() {
        Crops.instance.registerBaseSeed(new ItemStack(Items.WHEAT_SEEDS), cropWheat, 1, 1, 1, 1);
        Crops.instance.registerBaseSeed(new ItemStack(Items.PUMPKIN_SEEDS), cropPumpkin, 1, 1, 1, 1);
        Crops.instance.registerBaseSeed(new ItemStack(Items.MELON_SEEDS), cropMelon, 1, 1, 1, 1);
        Crops.instance.registerBaseSeed(new ItemStack(Items.NETHER_WART), cropNetherWart, 1, 1, 1, 1);
        Crops.instance.registerBaseSeed(new ItemStack(net.minecraft.registry.Registries.ITEM.get(Identifier.of("ic2", "terra_wart"))), cropTerraWart, 1, 1, 1, 1);
        Crops.instance.registerBaseSeed(ic2Item("crop_res", "coffee_beans"), cropCoffee, 1, 1, 1, 1);
        Crops.instance.registerBaseSeed(new ItemStack(Items.SUGAR_CANE), cropReed, 1, 3, 0, 2);
        Crops.instance.registerBaseSeed(new ItemStack(Items.COCOA_BEANS), cropCocoa, 1, 0, 0, 0);
        Crops.instance.registerBaseSeed(new ItemStack(Blocks.POPPY, 4), cropRedFlower, 4, 1, 1, 1);
        Crops.instance.registerBaseSeed(new ItemStack(Blocks.DANDELION, 4), cropYellowFlower, 4, 1, 1, 1);
        Crops.instance.registerBaseSeed(new ItemStack(Items.CARROT), cropCarrots, 1, 1, 1, 1);
        Crops.instance.registerBaseSeed(new ItemStack(Items.POTATO), cropPotato, 1, 1, 1, 1);
        Crops.instance.registerBaseSeed(new ItemStack(Blocks.BROWN_MUSHROOM, 4), cropBrownMushroom, 1, 1, 1, 1);
        Crops.instance.registerBaseSeed(new ItemStack(Blocks.RED_MUSHROOM, 4), cropRedMushroom, 1, 1, 1, 1);
        Crops.instance.registerBaseSeed(new ItemStack(Blocks.CACTUS), cropEatingPlant, 1, 1, 1, 1);
        Crops.instance.registerBaseSeed(new ItemStack(Items.BEETROOT_SEEDS), cropBeetroot, 1, 1, 1, 1);
        Crops.instance.registerBaseSeed(new ItemStack(Blocks.OAK_SAPLING), cropOakSapling, 1, 1, 1, 1);
        Crops.instance.registerBaseSeed(new ItemStack(Blocks.SPRUCE_SAPLING), cropSpruceSapling, 1, 1, 1, 1);
        Crops.instance.registerBaseSeed(new ItemStack(Blocks.BIRCH_SAPLING), cropBirchSapling, 1, 1, 1, 1);
        Crops.instance.registerBaseSeed(new ItemStack(Blocks.JUNGLE_SAPLING), cropJungleSapling, 1, 1, 1, 1);
        Crops.instance.registerBaseSeed(new ItemStack(Blocks.ACACIA_SAPLING), cropAcaciaSapling, 1, 1, 1, 1);
        Crops.instance.registerBaseSeed(new ItemStack(Blocks.DARK_OAK_SAPLING), cropDarkOakSapling, 1, 1, 1, 1);
    }

    // ---------------------------------------------------------------- Crops

    @Override
    public void addBiomenutrientsBonus(BiomeDictionary.Type type, int nutrientsBonus) {
        nutrientBiomeTypeBonus.put(type, nutrientsBonus);
    }

    @Override
    public void addBiomehumidityBonus(BiomeDictionary.Type type, int humidityBonus) {
        humidityBiomeTypeBonus.put(type, humidityBonus);
    }

    @Override
    public int getHumidityBiomeBonus(RegistryEntry<Biome> biome) {
        return bestBonus(humidityBiomeTypeBonus, biome);
    }

    @Override
    public int getNutrientBiomeBonus(RegistryEntry<Biome> biome) {
        return bestBonus(nutrientBiomeTypeBonus, biome);
    }

    /** The largest bonus among the kinds of place this biome counts as, as the original did. */
    private static int bestBonus(Map<BiomeDictionary.Type, Integer> bonuses, RegistryEntry<Biome> biome) {
        int ret = 0;
        for (BiomeDictionary.Type type : BiomeDictionary.getTypes(biome)) {
            Integer value = bonuses.get(type);
            if (value != null && value > ret) {
                ret = value;
            }
        }
        return ret;
    }

    @Override
    public CropCard getCropCard(String owner, String id) {
        return CropManager.getInstance().getCrop(owner, id);
    }

    @Override
    public CropCard getCropCard(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        if (stack.getItem() instanceof ic2.api.crops.ICropSeed seed) {
            return seed.getCropFromStack(stack);
        }
        BaseSeed base = getBaseSeed(stack);
        return base == null ? null : base.crop;
    }

    @Override
    public Collection<CropCard> getCrops() {
        return CropManager.getInstance().getCrops();
    }

    @Override
    public void registerCrop(CropCard card) {
        CropManager.getInstance().register(card);
    }

    @Override
    public void registerCropTextures(Map<Identifier, Sprite> textures) {
        this.textures = textures;
    }

    /** The sprites the crop renderer looks its crop up in; empty until the atlas is built. */
    public Map<Identifier, Sprite> getCropTextures() {
        return textures;
    }

    @Override
    public boolean registerBaseSeed(ItemStack stack, CropCard crop, int size, int growth, int gain, int resistance) {
        if (stack == null || stack.isEmpty() || crop == null) {
            return false;
        }
        return baseSeeds.putIfAbsent(stack.getItem(), new BaseSeed(crop, size, growth, gain, resistance)) == null;
    }

    @Override
    public BaseSeed getBaseSeed(ItemStack stack) {
        return stack == null || stack.isEmpty() ? null : baseSeeds.get(stack.getItem());
    }

    /** The base seeds as registered, for the tooltip and for the crop stick's own check. */
    public List<Item> getBaseSeedItems() {
        return new ArrayList<>(baseSeeds.keySet());
    }

    // ---------------------------------------------------------------- helpers

    private static CropCard sapling(String id, net.minecraft.block.Block leaves, net.minecraft.block.Block sapling) {
        return new CropBaseSapling(id, id, new ItemStack(leaves), new ItemStack(sapling));
    }

    /**
     * One of the flattened IC2 items, or nothing when it is not registered yet. A crop whose drop
     * is missing still registers - it simply drops nothing, which beats failing the whole load.
     */
    private static ItemStack ic2Item(String group, String variant) {
        Item item = IC2ItemVariants.get(group, variant);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }
}
