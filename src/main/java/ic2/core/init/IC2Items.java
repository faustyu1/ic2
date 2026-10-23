package ic2.core.init;

import ic2.core.item.ItemAdvancedBattery;
import ic2.core.item.ItemBatteryChargeHotbar;
import ic2.core.item.ItemBatterySU;
import ic2.core.item.ItemElectric;
import ic2.core.item.ItemGradualInt;
import ic2.core.item.reactor.ItemReactorCondensator;
import ic2.core.item.reactor.ItemReactorMOX;
import ic2.core.item.reactor.ItemReactorVentSpread;
import ic2.core.item.reactor.ItemReactorHeatExchanger;
import ic2.core.item.reactor.ItemReactorHeatStorage;
import ic2.core.item.reactor.ItemReactorHeatVent;
import ic2.core.item.reactor.ItemReactorPlating;
import ic2.core.item.reactor.ItemReactorReflector;
import ic2.core.item.reactor.ItemReactorUranium;
import ic2.core.item.tool.BronzeToolMaterial;
import ic2.core.item.ItemBattery;
import ic2.core.item.ItemCell;
import ic2.core.item.ItemBooze;
import ic2.core.item.ItemCoke;
import ic2.core.item.ItemCropSeed;
import ic2.core.item.ItemFoodIc2;
import ic2.core.item.ItemSpray;
import ic2.core.item.ItemTreetap;
import ic2.core.item.ItemIC2;
import ic2.core.item.handheld.HandHeldKind;
import ic2.core.item.handheld.ItemHandHeldContainer;
import ic2.core.item.ItemIodineTablet;
import ic2.core.item.ItemMug;
import ic2.core.item.ItemTerraWart;
import ic2.core.item.ItemMisc;
import ic2.core.item.ItemWrench;
import ic2.core.item.tool.ItemCropnalyzer;
import ic2.core.item.tool.ItemCutter;
import ic2.core.item.tool.ItemDrill;
import ic2.core.item.tool.ItemElectricHoe;
import ic2.core.item.tool.ItemNanoSaber;
import ic2.core.item.tool.ItemScanner;
import ic2.core.item.tool.ItemToolMeter;
import ic2.core.item.tool.ItemWindmeter;
import ic2.core.item.tool.ItemTreetapElectric;
import ic2.core.item.tool.ItemMiningLaser;
import ic2.core.item.armor.IC2ArmorMaterials;
import ic2.core.item.armor.ItemArmorCFPack;
import ic2.core.item.armor.ItemArmorHazmat;
import ic2.core.item.armor.ItemArmorIC2;
import ic2.core.item.armor.ItemArmorJetpack;
import ic2.core.item.armor.ItemArmorNanoSuit;
import ic2.core.item.armor.ItemArmorNightvisionGoggles;
import ic2.core.item.armor.ItemArmorPack;
import ic2.core.item.armor.ItemArmorQuantumSuit;
import ic2.core.item.armor.ItemArmorSolarHelmet;
import ic2.core.item.armor.ItemArmorStaticBoots;
import ic2.core.item.armor.ItemArmorUtility;
import ic2.core.item.tool.ItemChainsaw;
import ic2.core.item.tool.ItemElectricWrench;
import ic2.core.item.tool.ItemWrenchAdvanced;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.AxeItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.Item;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.SwordItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class IC2Items {
    public static Item WRENCH;
    public static Item RE_BATTERY;
    public static Item COPPER_INGOT;
    public static Item TIN_INGOT;
    public static Item BRONZE_INGOT;
    public static Item EMPTY_CELL;
    public static Item REFINED_IRON;
    public static Item DRILL;
    public static Item CHAINSAW;
    public static Item ELECTRIC_WRENCH;
    public static Item ADVANCED_WRENCH;
    public static Item ADV_BATTERY;
    public static Item NANO_HELMET;
    public static Item QUANTUM_HELMET;
    public static Item BATPACK;
    public static Item JETPACK;
    public static Item NANO_CHEST;
    public static Item NANO_LEGS;
    public static Item NANO_BOOTS;
    public static Item QUANTUM_CHEST;
    public static Item QUANTUM_LEGS;
    public static Item QUANTUM_BOOTS;
    public static Item LAPPACK;
    public static Item CF_PACK;
    // --- stubs: dusts / plates / misc ---
    public static Item DUST_COPPER;
    public static Item DUST_TIN;
    public static Item DUST_BRONZE;
    public static Item DUST_GOLD;
    public static Item DUST_IRON;
    public static Item PLATE_COPPER;
    public static Item PLATE_IRON;
    public static Item RUBBER;
    public static Item RESIN;
    public static Item SCRAP;
    public static Item SCRAP_BOX;
    public static Item UU_MATTER;
    public static Item CABLE_COPPER;
    public static Item CABLE_GOLD;
    public static Item CABLE_HV;
    public static Item FUEL_ROD;
    public static Item MOX_FUEL_ROD;
    public static Item CRAFTING_CIRCUIT;
    public static Item ADV_CIRCUIT;
    public static Item CARBON_PLATE;
    public static Item WATER_CELL;
    public static Item LAVA_CELL;
    public static Item BIOFUEL_CELL;
    public static Item CUTTER;
    public static Item MINING_LASER;
    public static Item DYNAMITE;
    public static Item DYNAMITE_STICKY;
    public static Item COFFEE;
    public static Item BOOZE;
    public static Item MUG;
    public static Item CROP_SEED;
    public static Item CROP_STICK;
    public static Item IODINE_TABLET;
    public static Item TERRA_WART;
    public static Item RUBBER_BOAT;
    public static Item CARBON_BOAT;
    public static Item ELECTRIC_BOAT;
    public static Item SPRAY;
    public static Item TREETAP;
    public static Item COKE;
    public static Item ADVANCED_RE_BATTERY;
    public static Item ENERGY_CRYSTAL;
    public static Item LAPOTRON_CRYSTAL;
    public static Item SINGLE_USE_BATTERY;

    private IC2Items(){}

    public static void registerAll(){
        WRENCH = reg("wrench", new ItemWrench());
        RE_BATTERY = reg("re_battery", new ItemBattery());
        EMPTY_CELL = reg("cell", new ItemCell("cell"));
        DRILL = reg("drill", ItemDrill.drill());
        CHAINSAW = reg("chainsaw", new ItemChainsaw());
        ELECTRIC_WRENCH = reg("electric_wrench", new ItemElectricWrench());
        ADVANCED_WRENCH = reg("wrench_advanced", new ItemWrenchAdvanced());
        ADV_BATTERY = reg("adv_battery", new ItemAdvancedBattery());
        CUTTER = reg("cutter", new ItemCutter());
        MINING_LASER = reg("mining_laser", new ItemMiningLaser());
        COFFEE = reg("coffee", new ItemFoodIc2("coffee", 2, 0.5f));
        BOOZE = reg("booze", new ItemBooze());
        MUG = reg("mug", new ItemMug());
        CROP_SEED = reg("crop_seed", new ItemCropSeed("wheat"));
        IODINE_TABLET = reg("iodine_tablet", new ItemIodineTablet());
        TERRA_WART = reg("terra_wart", new ItemTerraWart());
        SPRAY = reg("spray", new ItemSpray("spray"));
        TREETAP = reg("treetap", new ItemTreetap("treetap"));
        COKE = reg("coke", new ItemCoke());
        MOX_FUEL_ROD = reg("mox_fuel_rod", new ItemReactorMOX("mox_fuel_rod", 1));

        registerBatteries();
        registerReactorComponents();
        registerTools();
        registerArmor();


        // metadata variants that 1.13 flattening split into their own items live in
        // IC2ItemVariants; bind the convenience handles used across the port to them
        COPPER_INGOT = variant("ingot", "copper");
        TIN_INGOT = variant("ingot", "tin");
        BRONZE_INGOT = variant("ingot", "bronze");
        REFINED_IRON = variant("ingot", "refined_iron");
        DUST_COPPER = variant("dust", "copper");
        DUST_TIN = variant("dust", "tin");
        DUST_BRONZE = variant("dust", "bronze");
        DUST_GOLD = variant("dust", "gold");
        DUST_IRON = variant("dust", "iron");
        PLATE_COPPER = variant("plate", "copper");
        PLATE_IRON = variant("plate", "iron");
        RUBBER = variant("crafting", "rubber");
        RESIN = variant("misc_resource", "resin");
        SCRAP = variant("crafting", "scrap");
        SCRAP_BOX = variant("crafting", "scrap_box");
        UU_MATTER = variant("misc_resource", "matter");
        CRAFTING_CIRCUIT = variant("crafting", "circuit");
        ADV_CIRCUIT = variant("crafting", "advanced_circuit");
        CARBON_PLATE = variant("crafting", "carbon_plate");
        FUEL_ROD = variant("crafting", "fuel_rod");
        WATER_CELL = variant("fluid_cell", "water");
        LAVA_CELL = variant("fluid_cell", "lava");
        BIOFUEL_CELL = variant("fluid_cell", "ic2biofuel");
        RUBBER_BOAT = variant("boat", "rubber");
        CARBON_BOAT = variant("boat", "carbon");
        ELECTRIC_BOAT = variant("boat", "electric");
        CABLE_COPPER = variant("cable", "type:copper,insulation:1");
        CABLE_GOLD = variant("cable", "type:gold,insulation:2");
        CABLE_HV = variant("cable", "type:iron,insulation:3");
    }


    /**
     * The batteries, with the capacities and transfer limits BlocksItems gave them in 1.12.2.
     * The charging ones keep the hotbar topped up; the single use one empties itself into it.
     */
    private static void registerBatteries() {
        ADVANCED_RE_BATTERY = reg("advanced_re_battery", new ItemElectric("advanced_re_battery", 2, 100000, 256));
        ENERGY_CRYSTAL = reg("energy_crystal", new ItemElectric("energy_crystal", 3, 1000000, 2048));
        LAPOTRON_CRYSTAL = reg("lapotron_crystal", new ItemElectric("lapotron_crystal", 4, 10000000, 8092));
        SINGLE_USE_BATTERY = reg("single_use_battery", new ItemBatterySU("single_use_battery", 1200, 1));
        reg("charging_re_battery", new ItemBatteryChargeHotbar("charging_re_battery", 1, 40000, 128));
        reg("advanced_charging_re_battery", new ItemBatteryChargeHotbar("advanced_charging_re_battery", 2, 400000, 1024));
        reg("charging_energy_crystal", new ItemBatteryChargeHotbar("charging_energy_crystal", 3, 4000000, 8192));
        reg("charging_lapotron_crystal", new ItemBatteryChargeHotbar("charging_lapotron_crystal", 4, 40000000, 32768));
    }

    /**
     * The nuclear reactor components. The numbers are the heat or lifetime budgets of 1.12.2; what
     * each of them does with that budget inside a reactor is part of the reactor port.
     */
    private static void registerReactorComponents() {
        reg("heat_storage", new ItemReactorHeatStorage("heat_storage", 10000));
        reg("tri_heat_storage", new ItemReactorHeatStorage("tri_heat_storage", 30000));
        reg("hex_heat_storage", new ItemReactorHeatStorage("hex_heat_storage", 60000));

        reg("plating", new ItemReactorPlating("plating", 1000, 0.95f));
        reg("heat_plating", new ItemReactorPlating("heat_plating", 2000, 0.99f));
        reg("containment_plating", new ItemReactorPlating("containment_plating", 500, 0.9f));

        reg("heat_exchanger", new ItemReactorHeatExchanger("heat_exchanger", 2500, 12, 4));
        reg("reactor_heat_exchanger", new ItemReactorHeatExchanger("reactor_heat_exchanger", 5000, 0, 72));
        reg("component_heat_exchanger", new ItemReactorHeatExchanger("component_heat_exchanger", 5000, 36, 0));
        reg("advanced_heat_exchanger", new ItemReactorHeatExchanger("advanced_heat_exchanger", 10000, 24, 8));

        reg("heat_vent", new ItemReactorHeatVent("heat_vent", 1000, 6, 0));
        reg("reactor_heat_vent", new ItemReactorHeatVent("reactor_heat_vent", 1000, 5, 5));
        reg("overclocked_heat_vent", new ItemReactorHeatVent("overclocked_heat_vent", 1000, 20, 36));
        reg("advanced_heat_vent", new ItemReactorHeatVent("advanced_heat_vent", 1000, 12, 0));
        // the component vent cools its neighbours instead of holding heat itself
        reg("component_heat_vent", new ItemReactorVentSpread("component_heat_vent", 4));

        reg("neutron_reflector", new ItemReactorReflector("neutron_reflector", 30000));
        reg("thick_neutron_reflector", new ItemReactorReflector("thick_neutron_reflector", 120000));
        reg("iridium_reflector", new ItemReactorReflector("iridium_reflector"));

        reg("rsh_condensator", new ItemReactorCondensator("rsh_condensator", 20000));
        reg("lzh_condensator", new ItemReactorCondensator("lzh_condensator", 100000));

        reg("uranium_fuel_rod", new ItemReactorUranium("uranium_fuel_rod", 1));
        reg("dual_uranium_fuel_rod", new ItemReactorUranium("dual_uranium_fuel_rod", 2));
        reg("quad_uranium_fuel_rod", new ItemReactorUranium("quad_uranium_fuel_rod", 4));
        reg("dual_mox_fuel_rod", new ItemReactorMOX("dual_mox_fuel_rod", 2));
        reg("quad_mox_fuel_rod", new ItemReactorMOX("quad_mox_fuel_rod", 4));
    }

    /**
     * Tools. The electric ones carry the capacity and tier of the original; the mechanical ones
     * count their uses in the same custom damage counter 1.12.2 used.
     */
    private static void registerTools() {
        reg("bronze_pickaxe", new PickaxeItem(BronzeToolMaterial.INSTANCE,
                new Item.Settings().attributeModifiers(PickaxeItem.createAttributeModifiers(BronzeToolMaterial.INSTANCE, 1.0f, -2.8f))));
        reg("bronze_axe", new AxeItem(BronzeToolMaterial.INSTANCE,
                new Item.Settings().attributeModifiers(AxeItem.createAttributeModifiers(BronzeToolMaterial.INSTANCE, 6.0f, -3.2f))));
        reg("bronze_shovel", new ShovelItem(BronzeToolMaterial.INSTANCE,
                new Item.Settings().attributeModifiers(ShovelItem.createAttributeModifiers(BronzeToolMaterial.INSTANCE, 1.5f, -3.0f))));
        reg("bronze_hoe", new HoeItem(BronzeToolMaterial.INSTANCE,
                new Item.Settings().attributeModifiers(HoeItem.createAttributeModifiers(BronzeToolMaterial.INSTANCE, -2.0f, -1.0f))));
        reg("bronze_sword", new SwordItem(BronzeToolMaterial.INSTANCE,
                new Item.Settings().attributeModifiers(SwordItem.createAttributeModifiers(BronzeToolMaterial.INSTANCE, 3, -2.4f))));

        reg("forge_hammer", new ItemGradualInt("forge_hammer", 80));
        reg("foam_sprayer", new ic2.core.item.tool.ItemSprayer());
        reg("wrench_new", new ic2.core.item.tool.ItemToolWrenchNew());
        reg("crowbar", new ic2.core.item.tool.ItemToolCrowbar());
        reg("tool_box", new ItemHandHeldContainer(HandHeldKind.TOOLBOX));
        reg("containment_box", new ItemHandHeldContainer(HandHeldKind.CONTAINMENT_BOX));
        reg("frequency_transmitter", new ic2.core.item.tool.ItemFrequencyTransmitter("frequency_transmitter"));
        reg("meter", new ItemToolMeter());
        reg("remote", new ic2.core.item.tool.ItemRemote());
        reg("weeding_trowel", new ic2.core.item.tool.ItemWeedingTrowel());
        CROP_STICK = reg("crop_stick", new ic2.core.crop.ItemCrop());
        reg("filled_tin_can", new ic2.core.item.ItemTinCan());
        DYNAMITE = reg("dynamite", new ic2.core.item.block.ItemDynamite("dynamite", false));
        DYNAMITE_STICKY = reg("dynamite_sticky", new ic2.core.item.block.ItemDynamite("dynamite_sticky", true));

        // wind turbine rotors; the number is how long the rotor lasts
        reg("rotor_wood", new ic2.core.item.resources.ItemWindRotor("rotor_wood", 5, 10800, 0.25F, 10, 60));
        reg("rotor_bronze", new ic2.core.item.resources.ItemWindRotor("rotor_bronze", 7, 86400, 0.5F, 14, 75));
        reg("rotor_iron", new ic2.core.item.resources.ItemWindRotor("rotor_iron", 7, 86400, 0.5F, 14, 75));
        reg("rotor_steel", new ic2.core.item.resources.ItemWindRotor("rotor_steel", 9, 172800, 0.75F, 17, 90));
        reg("rotor_carbon", new ic2.core.item.resources.ItemWindRotor("rotor_carbon", 11, 604800, 1.0F, 20, 110));

        reg("diamond_drill", ItemDrill.diamondDrill());
        reg("iridium_drill", ItemDrill.iridiumDrill());
        reg("electric_hoe", new ItemElectricHoe());
        reg("electric_treetap", new ItemTreetapElectric());
        reg("nano_saber", new ItemNanoSaber());
        reg("obscurator", new ic2.core.item.tool.ItemObscurator());
        reg("advanced_scanner", ItemScanner.advanced());
        reg("scanner", new ItemScanner());
        reg("crystal_memory", new ic2.core.item.ItemCrystalMemory());
        reg("wind_meter", new ItemWindmeter());
        reg("cropnalyzer", new ItemCropnalyzer());
    }

    /**
     * Armour. Every piece is a real {@link net.minecraft.item.ArmorItem} that equips in its slot;
     * the suits that run on energy absorb through {@link ic2.api.item.ISpecialArmor}, which
     * {@code ic2.core.mixin.LivingEntityArmorMixin} applies. The capacities, transfer limits and
     * tiers are the ones from 1.12.2.
     */
    private static void registerArmor() {
        IC2ArmorMaterials.registerAll();

        // plain armour: protects with armour points, 15x and 50x the base durability as in 1.12.2
        reg("bronze_helmet", new ItemArmorIC2("bronze_helmet", IC2ArmorMaterials.BRONZE, ArmorItem.Type.HELMET, 15));
        reg("bronze_chestplate", new ItemArmorIC2("bronze_chestplate", IC2ArmorMaterials.BRONZE, ArmorItem.Type.CHESTPLATE, 15));
        reg("bronze_leggings", new ItemArmorIC2("bronze_leggings", IC2ArmorMaterials.BRONZE, ArmorItem.Type.LEGGINGS, 15));
        reg("bronze_boots", new ItemArmorIC2("bronze_boots", IC2ArmorMaterials.BRONZE, ArmorItem.Type.BOOTS, 15));
        reg("alloy_chestplate", new ItemArmorIC2("alloy_chestplate", IC2ArmorMaterials.ALLOY, ArmorItem.Type.CHESTPLATE, 50));

        // hazmat suit and rubber boots
        reg("hazmat_helmet", new ItemArmorHazmat("hazmat_helmet", IC2ArmorMaterials.HAZMAT, ArmorItem.Type.HELMET));
        reg("hazmat_chestplate", new ItemArmorHazmat("hazmat_chestplate", IC2ArmorMaterials.HAZMAT, ArmorItem.Type.CHESTPLATE));
        reg("hazmat_leggings", new ItemArmorHazmat("hazmat_leggings", IC2ArmorMaterials.HAZMAT, ArmorItem.Type.LEGGINGS));
        reg("rubber_boots", new ItemArmorHazmat("rubber_boots", IC2ArmorMaterials.RUBBER, ArmorItem.Type.BOOTS));

        // nano suit: 1M EU, 90% absorption at 5000 EU per damage point
        NANO_HELMET = reg("nano_helmet", new ItemArmorNanoSuit("nano_helmet", ArmorItem.Type.HELMET));
        NANO_CHEST = reg("nano_chestplate", new ItemArmorNanoSuit("nano_chestplate", ArmorItem.Type.CHESTPLATE));
        NANO_LEGS = reg("nano_leggings", new ItemArmorNanoSuit("nano_leggings", ArmorItem.Type.LEGGINGS));
        NANO_BOOTS = reg("nano_boots", new ItemArmorNanoSuit("nano_boots", ArmorItem.Type.BOOTS));

        // quantum suit: 10M EU, 20000 EU per damage point
        QUANTUM_HELMET = reg("quantum_helmet", new ItemArmorQuantumSuit("quantum_helmet", ArmorItem.Type.HELMET));
        QUANTUM_CHEST = reg("quantum_chestplate", new ItemArmorQuantumSuit("quantum_chestplate", ArmorItem.Type.CHESTPLATE));
        QUANTUM_LEGS = reg("quantum_leggings", new ItemArmorQuantumSuit("quantum_leggings", ArmorItem.Type.LEGGINGS));
        QUANTUM_BOOTS = reg("quantum_boots", new ItemArmorQuantumSuit("quantum_boots", ArmorItem.Type.BOOTS));

        // chest-slot battery packs
        BATPACK = reg("batpack", new ItemArmorPack("batpack", IC2ArmorMaterials.BATPACK, 60000.0, 100.0, 1));
        reg("advanced_batpack", new ItemArmorPack("advanced_batpack", IC2ArmorMaterials.ADVANCED_BATPACK, 600000.0, 1000.0, 2));
        reg("energy_pack", new ItemArmorPack("energy_pack", IC2ArmorMaterials.ENERGYPACK, 2000000.0, 1000.0, 3));
        LAPPACK = reg("lappack", new ItemArmorPack("lappack", IC2ArmorMaterials.LAPPACK, 2.0E7, 2500.0, 4));

        JETPACK = reg("jetpack", new ItemArmorJetpack());
        reg("jetpack_electric", new ic2.core.item.armor.ItemArmorJetpackElectric("jetpack_electric", 30000.0, 60.0, 1));

        // utility pieces
        CF_PACK = reg("cf_pack", new ItemArmorCFPack());
        reg("solar_helmet", new ItemArmorSolarHelmet());
        reg("static_boots", new ItemArmorStaticBoots());
        reg("nightvision_goggles", new ItemArmorNightvisionGoggles());
    }

    private static Item variant(String group, String name) {
        Item item = IC2ItemVariants.get(group, name);
        if (item == null) {
            throw new IllegalStateException("missing item variant " + group + "#" + name
                    + " - IC2ItemVariants.registerAll() must run before IC2Items.registerAll()");
        }
        return item;
    }

    private static Item reg(String name, Item item){
        return Registry.register(Registries.ITEM, Identifier.of("ic2", name), item);
    }
}
