package ic2.core.init;

import ic2.core.block.BlockTileEntity;
import ic2.core.block.TileEntityBlock;
import ic2.core.block.wiring.CableType;
import ic2.core.block.personal.TileEntityEnergyOMat;
import ic2.core.block.personal.TileEntityPersonalChest;
import ic2.core.block.personal.TileEntityTradeOMat;
import ic2.core.block.reactor.tileentity.TileEntityNuclearReactorElectric;
import ic2.core.block.reactor.tileentity.TileEntityReactorAccessHatch;
import ic2.core.block.reactor.tileentity.TileEntityReactorChamberElectric;
import ic2.core.block.reactor.tileentity.TileEntityReactorFluidPort;
import ic2.core.block.reactor.tileentity.TileEntityReactorRedstonePort;
import ic2.core.block.reactor.tileentity.TileEntityReactorVessel;
import ic2.core.block.machine.tileentity.TileEntityAdvMiner;
import ic2.core.block.machine.tileentity.TileEntityBatchCrafter;
import ic2.core.block.machine.tileentity.TileEntityChunkloader;
import ic2.core.block.machine.tileentity.TileEntityCondenser;
import ic2.core.block.machine.tileentity.TileEntityCreativeGenerator;
import ic2.core.block.machine.tileentity.TileEntityCropHarvester;
import ic2.core.block.machine.tileentity.TileEntityCropmatron;
import ic2.core.block.machine.tileentity.TileEntityElectrolyzer;
import ic2.core.block.machine.tileentity.TileEntityFermenter;
import ic2.core.block.machine.tileentity.TileEntityFluidDistributor;
import ic2.core.block.machine.tileentity.TileEntityFluidRegulator;
import ic2.core.block.machine.tileentity.TileEntityIndustrialWorkbench;
import ic2.core.block.machine.tileentity.TileEntityIronFurnace;
import ic2.core.block.machine.tileentity.TileEntityLiquidHeatExchanger;
import ic2.core.block.machine.tileentity.TileEntityMagnetizer;
import ic2.core.block.machine.tileentity.TileEntityMiner;
import ic2.core.block.machine.tileentity.TileEntityPump;
import ic2.core.block.machine.tileentity.TileEntitySolarDistiller;
import ic2.core.block.machine.tileentity.TileEntitySortingMachine;
import ic2.core.block.machine.tileentity.TileEntitySteamRepressurizer;
import ic2.core.block.machine.tileentity.TileEntityTank;
import ic2.core.block.machine.tileentity.TileEntityTeleporter;
import ic2.core.block.machine.tileentity.TileEntityTerraformer;
import ic2.core.block.machine.tileentity.TileEntityTesla;
import ic2.core.block.machine.tileentity.TileEntityWeightedFluidDistributor;
import ic2.core.block.machine.tileentity.TileEntityWeightedItemDistributor;
import ic2.core.block.reactor.tileentity.TileEntityRCI_LZH;
import ic2.core.block.reactor.tileentity.TileEntityRCI_RSH;
import ic2.core.block.steam.TileEntityCokeKilnGrate;
import ic2.core.block.steam.TileEntityCokeKilnHatch;
import ic2.core.block.storage.box.TileEntityBronzeStorageBox;
import ic2.core.block.storage.box.TileEntityIridiumStorageBox;
import ic2.core.block.storage.box.TileEntityIronStorageBox;
import ic2.core.block.storage.box.TileEntitySteelStorageBox;
import ic2.core.block.storage.box.TileEntityWoodenStorageBox;
import ic2.core.block.transport.TileEntityFluidPipe;
import ic2.core.block.steam.TileEntityBurningBox;
import ic2.core.block.steam.TileEntityCokeKiln;
import ic2.core.block.steam.TileEntitySteamGenerator;
import ic2.core.block.storage.tank.TileEntityBronzeTank;
import ic2.core.block.storage.tank.TileEntityIronTank;
import ic2.core.block.storage.tank.TileEntityIridiumTank;
import ic2.core.block.storage.tank.TileEntitySteelTank;
import ic2.core.crop.TileEntityCrop;
import ic2.core.block.TileEntityBarrel;
import ic2.core.block.TileEntityWall;
import ic2.core.block.machine.tileentity.TileEntityAssemblyBench;
import ic2.core.block.machine.tileentity.TileEntityItemBuffer;
import ic2.core.block.machine.tileentity.TileEntityNuke;
import ic2.core.block.machine.tileentity.TileEntityPatternStorage;
import ic2.core.block.machine.tileentity.TileEntityReplicator;
import ic2.core.block.machine.tileentity.TileEntityScanner;
import ic2.core.block.personal.TileEntityTradingTerminal;
import ic2.core.block.wiring.TileEntityCable;
import ic2.core.block.wiring.TileEntityElectricBatBox;
import ic2.core.block.wiring.TileEntityElectricCESU;
import ic2.core.block.wiring.TileEntityElectricMFE;
import ic2.core.block.wiring.TileEntityElectricMFSU;
import ic2.core.block.generator.tileentity.TileEntityConversionGenerator;
import ic2.core.block.generator.tileentity.TileEntityGenerator;
import ic2.core.block.generator.tileentity.TileEntityGeoGenerator;
import ic2.core.block.generator.tileentity.TileEntityKineticGenerator;
import ic2.core.block.generator.tileentity.TileEntityRTGenerator;
import ic2.core.block.generator.tileentity.TileEntitySemifluidGenerator;
import ic2.core.block.generator.tileentity.TileEntitySolarGenerator;
import ic2.core.block.generator.tileentity.TileEntityStirlingGenerator;
import ic2.core.block.generator.tileentity.TileEntityWaterGenerator;
import ic2.core.block.generator.tileentity.TileEntityWindGenerator;
import ic2.core.block.heatgenerator.tileentity.TileEntityElectricHeatGenerator;
import ic2.core.block.heatgenerator.tileentity.TileEntityFluidHeatGenerator;
import ic2.core.block.heatgenerator.tileentity.TileEntityRTHeatGenerator;
import ic2.core.block.heatgenerator.tileentity.TileEntitySolidHeatGenerator;
import ic2.core.block.kineticgenerator.tileentity.TileEntityElectricKineticGenerator;
import ic2.core.block.kineticgenerator.tileentity.TileEntityManualKineticGenerator;
import ic2.core.block.kineticgenerator.tileentity.TileEntitySteamKineticGenerator;
import ic2.core.block.kineticgenerator.tileentity.TileEntityStirlingKineticGenerator;
import ic2.core.block.kineticgenerator.tileentity.TileEntityWaterKineticGenerator;
import ic2.core.block.kineticgenerator.tileentity.TileEntityWindKineticGenerator;
import ic2.core.block.machine.tileentity.TileEntityBlastFurnace;
import ic2.core.block.machine.tileentity.TileEntityBlockCutter;
import ic2.core.block.machine.tileentity.TileEntityCanner;
import ic2.core.block.machine.tileentity.TileEntityCentrifuge;
import ic2.core.block.machine.tileentity.TileEntityCompressor;
import ic2.core.block.machine.tileentity.TileEntityExtractor;
import ic2.core.block.machine.tileentity.TileEntityFluidBottler;
import ic2.core.block.machine.tileentity.TileEntityInduction;
import ic2.core.block.machine.tileentity.TileEntityMacerator;
import ic2.core.block.machine.tileentity.TileEntityMatter;
import ic2.core.block.machine.tileentity.TileEntityMassFabricator;
import ic2.core.block.machine.tileentity.TileEntityMetalFormer;
import ic2.core.block.machine.tileentity.TileEntityOreWashing;
import ic2.core.block.machine.tileentity.TileEntityRecycler;
import ic2.core.block.machine.tileentity.TileEntitySolidCanner;
import ic2.core.block.machine.tileentity.TileEntityElectricFurnace;
import ic2.core.block.wiring.TileEntityTransformerEV;
import ic2.core.block.wiring.TileEntityTransformerHV;
import ic2.core.block.wiring.TileEntityTransformerLV;
import ic2.core.block.wiring.TileEntityTransformerMV;
import ic2.core.ref.TeBlock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.block.AbstractBlock;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

/**
 * Fabric registry for IC2 blocks and block entities.
 * Original used Forge GameRegistry and TeBlockRegistry.buildBlocks().
 * This version registers a subset (batbox, cesu, mfe, mfsu, cable, transformers) as proof of parity.
 */
public final class IC2Blocks {

    private static final Map<TeBlock, Block> BLOCKS = new HashMap<>();
    private static final Map<TeBlock, BlockEntityType<?>> BE_TYPES = new HashMap<>();

    public static Block BATBOX_BLOCK;
    public static Block CESU_BLOCK;
    public static Block MFE_BLOCK;
    public static Block MFSU_BLOCK;
    public static Block CABLE_BLOCK;
    public static Block LV_TRANSFORMER_BLOCK;
    public static Block MV_TRANSFORMER_BLOCK;
    public static Block HV_TRANSFORMER_BLOCK;
    public static Block EV_TRANSFORMER_BLOCK;
    public static Block MACERATOR_BLOCK;
    public static Block COMPRESSOR_BLOCK;
    public static Block EXTRACTOR_BLOCK;
    public static Block ELECTRIC_FURNACE_BLOCK;
    public static Block RECYCLER_BLOCK;
    public static Block CENTRIFUGE_BLOCK;
    public static Block BLAST_FURNACE_BLOCK;
    public static Block METAL_FORMER_BLOCK;
    public static Block ORE_WASHING_BLOCK;
    public static Block BLOCK_CUTTER_BLOCK;
    public static Block CANNER_BLOCK;
    public static Block SOLID_CANNER_BLOCK;
    public static Block FLUID_BOTTLER_BLOCK;
    public static Block INDUCTION_FURNACE_BLOCK;
    public static Block MASS_FABRICATOR_BLOCK;
    public static Block MATTER_BLOCK;
    public static Block GENERATOR_BLOCK;
    public static Block SOLAR_BLOCK;
    public static Block WATER_BLOCK;
    public static Block WIND_BLOCK;
    public static Block GEOTHERMAL_BLOCK;
    public static Block SEMIFLUID_BLOCK;
    public static Block RT_BLOCK;
    public static Block STIRLING_BLOCK;
    public static Block KINETIC_BLOCK;
    public static Block SOLID_HEAT_BLOCK;
    public static Block FLUID_HEAT_BLOCK;
    public static Block ELECTRIC_HEAT_BLOCK;
    public static Block RT_HEAT_BLOCK;
    public static Block ELECTRIC_KINETIC_BLOCK;
    public static Block MANUAL_KINETIC_BLOCK;
    public static Block STEAM_KINETIC_BLOCK;
    public static Block STIRLING_KINETIC_BLOCK;
    public static Block WATER_KINETIC_BLOCK;
    public static Block WIND_KINETIC_BLOCK;
    public static Block NUCLEAR_REACTOR_BLOCK;
    public static Block REACTOR_CHAMBER_BLOCK;
    public static Block REACTOR_HATCH_BLOCK;
    public static Block REACTOR_FLUID_PORT_BLOCK;
    public static Block REACTOR_REDSTONE_PORT_BLOCK;
    public static Block REACTOR_VESSEL_BLOCK;
    public static Block ENERGY_O_MAT_BLOCK;
    public static Block PERSONAL_CHEST_BLOCK;
    public static Block TRADE_O_MAT_BLOCK;
    public static Block CROP_BLOCK;
    public static Block BRONZE_TANK_BLOCK;
    public static Block IRON_TANK_BLOCK;
    public static Block STEEL_TANK_BLOCK;
    public static Block IRIDIUM_TANK_BLOCK;
    public static Block PUMP_BLOCK;
    public static Block MINER_BLOCK;
    public static Block ADV_MINER_BLOCK;
    public static Block CROP_HARVESTER_BLOCK;
    public static Block CROPMATRON_BLOCK;
    public static Block LIQUID_HEAT_EXCHANGER_BLOCK;
    public static Block CONDENSER_BLOCK;
    public static Block TELEPORTER_BLOCK;
    public static Block MAGNETIZER_BLOCK;
    public static Block TESLA_BLOCK;
    public static Block SORTING_BLOCK;
    public static Block ELECTROLYZER_BLOCK;
    public static Block FERMENTER_BLOCK;
    public static Block COKE_KILN_BLOCK;
    public static Block BURNING_BOX_BLOCK;
    public static Block STEAM_GENERATOR_BLOCK;
    public static Block TANK_BLOCK;
    public static Block TERRAFORMER_BLOCK;
    public static Block CHUNK_LOADER_BLOCK;
    public static Block IRON_FURNACE_BLOCK;
    public static Block FLUID_DISTRIBUTOR_BLOCK;
    public static Block FLUID_REGULATOR_BLOCK;
    public static Block WEIGHTED_FLUID_DISTRIBUTOR_BLOCK;
    public static Block WEIGHTED_ITEM_DISTRIBUTOR_BLOCK;
    public static Block INDUSTRIAL_WORKBENCH_BLOCK;
    public static Block BATCH_CRAFTER_BLOCK;
    public static Block WOODEN_BOX_BLOCK;
    public static Block IRON_BOX_BLOCK;
    public static Block BRONZE_BOX_BLOCK;
    public static Block STEEL_BOX_BLOCK;
    public static Block IRIDIUM_BOX_BLOCK;
    public static Block FLUID_PIPE_BLOCK;
    public static Block COKE_HATCH_BLOCK;
    public static Block COKE_GRATE_BLOCK;
    public static Block SOLAR_DISTILLER_BLOCK;
    public static Block CREATIVE_GENERATOR_BLOCK;
    public static Block RCI_RSH_BLOCK;
    public static Block RCI_LZH_BLOCK;
    public static Block STEAM_REPRESSURIZER_BLOCK;
    public static Block BARREL_BLOCK;
    public static Block WALL_BLOCK;
    public static Block ITNT_BLOCK;
    public static Block NUKE_BLOCK;
    public static Block ITEM_BUFFER_BLOCK;
    public static Block ITEM_BUFFER_2_BLOCK;
    public static Block CHARGEPAD_BATBOX_BLOCK;
    public static Block CHARGEPAD_CESU_BLOCK;
    public static Block CHARGEPAD_MFE_BLOCK;
    public static Block CHARGEPAD_MFSU_BLOCK;
    public static Block LUMINATOR_FLAT_BLOCK;
    public static Block PATTERN_STORAGE_BLOCK;
    public static Block REPLICATOR_BLOCK;
    public static Block SCANNER_BLOCK;
    public static Block TRADING_TERMINAL_BLOCK;
    public static Block UU_ASSEMBLY_BENCH_BLOCK;

    public static BlockEntityType<TileEntityElectricBatBox> BATBOX_BE;
    public static BlockEntityType<TileEntityElectricCESU> CESU_BE;
    public static BlockEntityType<TileEntityElectricMFE> MFE_BE;
    public static BlockEntityType<TileEntityElectricMFSU> MFSU_BE;
    public static BlockEntityType<TileEntityCable> CABLE_BE;
    public static BlockEntityType<TileEntityTransformerLV> LV_TRANSFORMER_BE;
    public static BlockEntityType<TileEntityTransformerMV> MV_TRANSFORMER_BE;
    public static BlockEntityType<TileEntityTransformerHV> HV_TRANSFORMER_BE;
    public static BlockEntityType<TileEntityTransformerEV> EV_TRANSFORMER_BE;
    public static BlockEntityType<TileEntityMacerator> MACERATOR_BE;
    public static BlockEntityType<TileEntityCompressor> COMPRESSOR_BE;
    public static BlockEntityType<TileEntityExtractor> EXTRACTOR_BE;
    public static BlockEntityType<TileEntityElectricFurnace> ELECTRIC_FURNACE_BE;
    public static BlockEntityType<TileEntityRecycler> RECYCLER_BE;
    public static BlockEntityType<TileEntityCentrifuge> CENTRIFUGE_BE;
    public static BlockEntityType<TileEntityBlastFurnace> BLAST_FURNACE_BE;
    public static BlockEntityType<TileEntityMetalFormer> METAL_FORMER_BE;
    public static BlockEntityType<TileEntityOreWashing> ORE_WASHING_BE;
    public static BlockEntityType<TileEntityBlockCutter> BLOCK_CUTTER_BE;
    public static BlockEntityType<TileEntityCanner> CANNER_BE;
    public static BlockEntityType<TileEntitySolidCanner> SOLID_CANNER_BE;
    public static BlockEntityType<TileEntityFluidBottler> FLUID_BOTTLER_BE;
    public static BlockEntityType<TileEntityInduction> INDUCTION_BE;
    public static BlockEntityType<TileEntityMatter> MATTER_BE;
    public static BlockEntityType<TileEntityMassFabricator> MASS_FABRICATOR_BE;
    public static BlockEntityType<TileEntityGenerator> GENERATOR_BE;
    public static BlockEntityType<TileEntitySolarGenerator> SOLAR_BE;
    public static BlockEntityType<TileEntityWaterGenerator> WATER_BE;
    public static BlockEntityType<TileEntityWindGenerator> WIND_BE;
    public static BlockEntityType<TileEntityGeoGenerator> GEOTHERMAL_BE;
    public static BlockEntityType<TileEntitySemifluidGenerator> SEMIFLUID_BE;
    public static BlockEntityType<TileEntityRTGenerator> RT_BE;
    public static BlockEntityType<TileEntityStirlingGenerator> STIRLING_BE;
    public static BlockEntityType<TileEntityKineticGenerator> KINETIC_BE;
    public static BlockEntityType<TileEntitySolidHeatGenerator> SOLID_HEAT_BE;
    public static BlockEntityType<TileEntityFluidHeatGenerator> FLUID_HEAT_BE;
    public static BlockEntityType<TileEntityElectricHeatGenerator> ELECTRIC_HEAT_BE;
    public static BlockEntityType<TileEntityRTHeatGenerator> RT_HEAT_BE;
    public static BlockEntityType<TileEntityElectricKineticGenerator> ELECTRIC_KINETIC_BE;
    public static BlockEntityType<TileEntityManualKineticGenerator> MANUAL_KINETIC_BE;
    public static BlockEntityType<TileEntitySteamKineticGenerator> STEAM_KINETIC_BE;
    public static BlockEntityType<TileEntityStirlingKineticGenerator> STIRLING_KINETIC_BE;
    public static BlockEntityType<TileEntityWaterKineticGenerator> WATER_KINETIC_BE;
    public static BlockEntityType<TileEntityWindKineticGenerator> WIND_KINETIC_BE;
    public static BlockEntityType<TileEntityNuclearReactorElectric> NUCLEAR_REACTOR_BE;
    public static BlockEntityType<TileEntityReactorChamberElectric> REACTOR_CHAMBER_BE;
    public static BlockEntityType<TileEntityReactorAccessHatch> REACTOR_HATCH_BE;
    public static BlockEntityType<TileEntityReactorFluidPort> REACTOR_FLUID_PORT_BE;
    public static BlockEntityType<TileEntityReactorRedstonePort> REACTOR_REDSTONE_PORT_BE;
    public static BlockEntityType<TileEntityReactorVessel> REACTOR_VESSEL_BE;
    public static BlockEntityType<TileEntityEnergyOMat> ENERGY_O_MAT_BE;
    public static BlockEntityType<TileEntityPersonalChest> PERSONAL_CHEST_BE;
    public static BlockEntityType<TileEntityTradeOMat> TRADE_O_MAT_BE;
    public static BlockEntityType<TileEntityCrop> CROP_BE;
    public static BlockEntityType<TileEntityBronzeTank> BRONZE_TANK_BE;
    public static BlockEntityType<TileEntityIronTank> IRON_TANK_BE;
    public static BlockEntityType<TileEntitySteelTank> STEEL_TANK_BE;
    public static BlockEntityType<TileEntityIridiumTank> IRIDIUM_TANK_BE;
    public static BlockEntityType<TileEntityPump> PUMP_BE;
    public static BlockEntityType<TileEntityMiner> MINER_BE;
    public static BlockEntityType<TileEntityAdvMiner> ADV_MINER_BE;
    public static BlockEntityType<TileEntityCropHarvester> CROP_HARVESTER_BE;
    public static BlockEntityType<TileEntityCropmatron> CROPMATRON_BE;
    public static BlockEntityType<TileEntityLiquidHeatExchanger> LIQUID_HEAT_EXCHANGER_BE;
    public static BlockEntityType<TileEntityCondenser> CONDENSER_BE;
    public static BlockEntityType<TileEntityTeleporter> TELEPORTER_BE;
    public static BlockEntityType<TileEntityMagnetizer> MAGNETIZER_BE;
    public static BlockEntityType<TileEntityTesla> TESLA_BE;
    public static BlockEntityType<TileEntitySortingMachine> SORTING_BE;
    public static BlockEntityType<TileEntityElectrolyzer> ELECTROLYZER_BE;
    public static BlockEntityType<TileEntityFermenter> FERMENTER_BE;
    public static BlockEntityType<TileEntityCokeKiln> COKE_KILN_BE;
    public static BlockEntityType<TileEntityBurningBox> BURNING_BOX_BE;
    public static BlockEntityType<TileEntitySteamGenerator> STEAM_GENERATOR_BE;
    public static BlockEntityType<TileEntityTank> TANK_BE;
    public static BlockEntityType<TileEntityTerraformer> TERRAFORMER_BE;
    public static BlockEntityType<TileEntityChunkloader> CHUNK_LOADER_BE;
    public static BlockEntityType<TileEntityIronFurnace> IRON_FURNACE_BE;
    public static BlockEntityType<TileEntityFluidDistributor> FLUID_DISTRIBUTOR_BE;
    public static BlockEntityType<TileEntityFluidRegulator> FLUID_REGULATOR_BE;
    public static BlockEntityType<TileEntityWeightedFluidDistributor> WEIGHTED_FLUID_DISTRIBUTOR_BE;
    public static BlockEntityType<TileEntityWeightedItemDistributor> WEIGHTED_ITEM_DISTRIBUTOR_BE;
    public static BlockEntityType<TileEntityIndustrialWorkbench> INDUSTRIAL_WORKBENCH_BE;
    public static BlockEntityType<TileEntityBatchCrafter> BATCH_CRAFTER_BE;
    public static BlockEntityType<TileEntityWoodenStorageBox> WOODEN_BOX_BE;
    public static BlockEntityType<TileEntityIronStorageBox> IRON_BOX_BE;
    public static BlockEntityType<TileEntityBronzeStorageBox> BRONZE_BOX_BE;
    public static BlockEntityType<TileEntitySteelStorageBox> STEEL_BOX_BE;
    public static BlockEntityType<TileEntityIridiumStorageBox> IRIDIUM_BOX_BE;
    public static BlockEntityType<TileEntityFluidPipe> FLUID_PIPE_BE;
    public static BlockEntityType<TileEntityCokeKilnHatch> COKE_HATCH_BE;
    public static BlockEntityType<TileEntityCokeKilnGrate> COKE_GRATE_BE;
    public static BlockEntityType<TileEntitySolarDistiller> SOLAR_DISTILLER_BE;
    public static BlockEntityType<TileEntityCreativeGenerator> CREATIVE_GENERATOR_BE;
    public static BlockEntityType<TileEntityRCI_RSH> RCI_RSH_BE;
    public static BlockEntityType<TileEntityRCI_LZH> RCI_LZH_BE;
    public static BlockEntityType<TileEntitySteamRepressurizer> STEAM_REPRESSURIZER_BE;
    public static BlockEntityType<TileEntityBarrel> BARREL_BE;
    public static BlockEntityType<TileEntityWall> WALL_BE;
    public static BlockEntityType<ic2.core.block.machine.tileentity.TileEntityITnt> ITNT_BE;
    public static BlockEntityType<TileEntityNuke> NUKE_BE;
    public static BlockEntityType<TileEntityItemBuffer> ITEM_BUFFER_BE;
    public static BlockEntityType<ic2.core.block.machine.tileentity.TileEntityBetterItemBuffer> ITEM_BUFFER_2_BE;
    public static BlockEntityType<ic2.core.block.wiring.TileEntityChargepadBatBox> CHARGEPAD_BATBOX_BE;
    public static BlockEntityType<ic2.core.block.wiring.TileEntityChargepadCESU> CHARGEPAD_CESU_BE;
    public static BlockEntityType<ic2.core.block.wiring.TileEntityChargepadMFE> CHARGEPAD_MFE_BE;
    public static BlockEntityType<ic2.core.block.wiring.TileEntityChargepadMFSU> CHARGEPAD_MFSU_BE;
    public static BlockEntityType<ic2.core.block.wiring.TileEntityLuminator> LUMINATOR_FLAT_BE;
    public static BlockEntityType<TileEntityPatternStorage> PATTERN_STORAGE_BE;
    public static BlockEntityType<TileEntityReplicator> REPLICATOR_BE;
    public static BlockEntityType<TileEntityScanner> SCANNER_BE;
    public static BlockEntityType<TileEntityTradingTerminal> TRADING_TERMINAL_BE;
    public static BlockEntityType<TileEntityAssemblyBench> UU_ASSEMBLY_BENCH_BE;

    private IC2Blocks() {}

    public static void registerAll() {
        // Use FabricBlockSettings for 1.21.1
        BATBOX_BLOCK = registerTeBlock(TeBlock.batbox, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        CESU_BLOCK = registerTeBlock(TeBlock.cesu, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        MFE_BLOCK = registerTeBlock(TeBlock.mfe, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        MFSU_BLOCK = registerTeBlock(TeBlock.mfsu, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        // one block per cable variant; the bare copper one stands in for "the cable block"
        // wherever the port needs a single handle on it. There is deliberately no separate
        // ic2:cable block: it was the same bare copper cable twice, once in the creative tab too.
        registerCableVariants();
        CABLE_BLOCK = Registries.BLOCK.get(Identifier.of("ic2", CableType.copper.getName(0, null)));
        LV_TRANSFORMER_BLOCK = registerTeBlock(TeBlock.lv_transformer, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        MV_TRANSFORMER_BLOCK = registerTeBlock(TeBlock.mv_transformer, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        HV_TRANSFORMER_BLOCK = registerTeBlock(TeBlock.hv_transformer, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        EV_TRANSFORMER_BLOCK = registerTeBlock(TeBlock.ev_transformer, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        MACERATOR_BLOCK = registerTeBlock(TeBlock.macerator, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        COMPRESSOR_BLOCK = registerTeBlock(TeBlock.compressor, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        EXTRACTOR_BLOCK = registerTeBlock(TeBlock.extractor, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        ELECTRIC_FURNACE_BLOCK = registerTeBlock(TeBlock.electric_furnace, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        RECYCLER_BLOCK = registerTeBlock(TeBlock.recycler, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        CENTRIFUGE_BLOCK = registerTeBlock(TeBlock.centrifuge, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        BLAST_FURNACE_BLOCK = registerTeBlock(TeBlock.blast_furnace, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        METAL_FORMER_BLOCK = registerTeBlock(TeBlock.metal_former, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        ORE_WASHING_BLOCK = registerTeBlock(TeBlock.ore_washing_plant, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        BLOCK_CUTTER_BLOCK = registerTeBlock(TeBlock.block_cutter, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        CANNER_BLOCK = registerTeBlock(TeBlock.canner, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        SOLID_CANNER_BLOCK = registerTeBlock(TeBlock.solid_canner, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        FLUID_BOTTLER_BLOCK = registerTeBlock(TeBlock.fluid_bottler, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        INDUCTION_FURNACE_BLOCK = registerTeBlock(TeBlock.induction_furnace, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        MASS_FABRICATOR_BLOCK = registerTeBlock(TeBlock.mass_fabricator, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        MATTER_BLOCK = registerTeBlock(TeBlock.matter_generator, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        GENERATOR_BLOCK = registerTeBlock(TeBlock.generator, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        SOLAR_BLOCK = registerTeBlock(TeBlock.solar_generator, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        WATER_BLOCK = registerTeBlock(TeBlock.water_generator, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        WIND_BLOCK = registerTeBlock(TeBlock.wind_generator, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        GEOTHERMAL_BLOCK = registerTeBlock(TeBlock.geo_generator, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        SEMIFLUID_BLOCK = registerTeBlock(TeBlock.semifluid_generator, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        RT_BLOCK = registerTeBlock(TeBlock.rt_generator, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        STIRLING_BLOCK = registerTeBlock(TeBlock.stirling_generator, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        KINETIC_BLOCK = registerTeBlock(TeBlock.kinetic_generator, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        SOLID_HEAT_BLOCK = registerTeBlock(TeBlock.solid_heat_generator, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        FLUID_HEAT_BLOCK = registerTeBlock(TeBlock.fluid_heat_generator, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        ELECTRIC_HEAT_BLOCK = registerTeBlock(TeBlock.electric_heat_generator, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        RT_HEAT_BLOCK = registerTeBlock(TeBlock.rt_heat_generator, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        ELECTRIC_KINETIC_BLOCK = registerTeBlock(TeBlock.electric_kinetic_generator, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        MANUAL_KINETIC_BLOCK = registerTeBlock(TeBlock.manual_kinetic_generator, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        STEAM_KINETIC_BLOCK = registerTeBlock(TeBlock.steam_kinetic_generator, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        STIRLING_KINETIC_BLOCK = registerTeBlock(TeBlock.stirling_kinetic_generator, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        WATER_KINETIC_BLOCK = registerTeBlock(TeBlock.water_kinetic_generator, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        WIND_KINETIC_BLOCK = registerTeBlock(TeBlock.wind_kinetic_generator, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        NUCLEAR_REACTOR_BLOCK = registerTeBlock(TeBlock.nuclear_reactor, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        REACTOR_CHAMBER_BLOCK = registerTeBlock(TeBlock.reactor_chamber, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        REACTOR_HATCH_BLOCK = registerTeBlock(TeBlock.reactor_access_hatch, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        REACTOR_FLUID_PORT_BLOCK = registerTeBlock(TeBlock.reactor_fluid_port, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        REACTOR_REDSTONE_PORT_BLOCK = registerTeBlock(TeBlock.reactor_redstone_port, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        REACTOR_VESSEL_BLOCK = registerTeBlock(TeBlock.reactor_vessel, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        ENERGY_O_MAT_BLOCK = registerTeBlock(TeBlock.energy_o_mat, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        PERSONAL_CHEST_BLOCK = registerTeBlock(TeBlock.personal_chest, MapColor.STONE_GRAY, BlockSoundGroup.WOOD);
        TRADE_O_MAT_BLOCK = registerTeBlock(TeBlock.trade_o_mat, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        CROP_BLOCK = registerCropBlock();
        BRONZE_TANK_BLOCK = registerTeBlock(TeBlock.bronze_tank, MapColor.ORANGE, BlockSoundGroup.METAL);
        IRON_TANK_BLOCK = registerTeBlock(TeBlock.iron_tank, MapColor.LIGHT_GRAY, BlockSoundGroup.METAL);
        STEEL_TANK_BLOCK = registerTeBlock(TeBlock.steel_tank, MapColor.GRAY, BlockSoundGroup.METAL);
        IRIDIUM_TANK_BLOCK = registerTeBlock(TeBlock.iridium_tank, MapColor.WHITE, BlockSoundGroup.METAL);
        PUMP_BLOCK = registerTeBlock(TeBlock.pump, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        MINER_BLOCK = registerTeBlock(TeBlock.miner, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        ADV_MINER_BLOCK = registerTeBlock(TeBlock.advanced_miner, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        CROP_HARVESTER_BLOCK = registerTeBlock(TeBlock.crop_harvester, MapColor.GREEN, BlockSoundGroup.WOOD);
        CROPMATRON_BLOCK = registerTeBlock(TeBlock.cropmatron, MapColor.GREEN, BlockSoundGroup.STONE);
        LIQUID_HEAT_EXCHANGER_BLOCK = registerTeBlock(TeBlock.liquid_heat_exchanger, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        CONDENSER_BLOCK = registerTeBlock(TeBlock.condenser, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        TELEPORTER_BLOCK = registerTeBlock(TeBlock.teleporter, MapColor.PURPLE, BlockSoundGroup.STONE);
        MAGNETIZER_BLOCK = registerTeBlock(TeBlock.magnetizer, MapColor.GRAY, BlockSoundGroup.STONE);
        TESLA_BLOCK = registerTeBlock(TeBlock.tesla_coil, MapColor.GRAY, BlockSoundGroup.STONE);
        SORTING_BLOCK = registerTeBlock(TeBlock.sorting_machine, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        ELECTROLYZER_BLOCK = registerTeBlock(TeBlock.electrolyzer, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        FERMENTER_BLOCK = registerTeBlock(TeBlock.fermenter, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        COKE_KILN_BLOCK = registerTeBlock(TeBlock.coke_kiln, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        BURNING_BOX_BLOCK = registerTeBlock(TeBlock.burning_box, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        STEAM_GENERATOR_BLOCK = registerTeBlock(TeBlock.steam_generator, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        TANK_BLOCK = registerTeBlock(TeBlock.tank, MapColor.CLEAR, BlockSoundGroup.GLASS);
        TERRAFORMER_BLOCK = registerTeBlock(TeBlock.terraformer, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        CHUNK_LOADER_BLOCK = registerTeBlock(TeBlock.chunk_loader, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        IRON_FURNACE_BLOCK = registerTeBlock(TeBlock.iron_furnace, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        FLUID_DISTRIBUTOR_BLOCK = registerTeBlock(TeBlock.fluid_distributor, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        FLUID_REGULATOR_BLOCK = registerTeBlock(TeBlock.fluid_regulator, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        WEIGHTED_FLUID_DISTRIBUTOR_BLOCK = registerTeBlock(TeBlock.weighted_fluid_distributor, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        WEIGHTED_ITEM_DISTRIBUTOR_BLOCK = registerTeBlock(TeBlock.weighted_item_distributor, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        INDUSTRIAL_WORKBENCH_BLOCK = registerTeBlock(TeBlock.industrial_workbench, MapColor.BROWN, BlockSoundGroup.WOOD);
        BATCH_CRAFTER_BLOCK = registerTeBlock(TeBlock.batch_crafter, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        WOODEN_BOX_BLOCK = registerTeBlock(TeBlock.wooden_storage_box, MapColor.BROWN, BlockSoundGroup.WOOD);
        IRON_BOX_BLOCK = registerTeBlock(TeBlock.iron_storage_box, MapColor.LIGHT_GRAY, BlockSoundGroup.METAL);
        BRONZE_BOX_BLOCK = registerTeBlock(TeBlock.bronze_storage_box, MapColor.ORANGE, BlockSoundGroup.METAL);
        STEEL_BOX_BLOCK = registerTeBlock(TeBlock.steel_storage_box, MapColor.GRAY, BlockSoundGroup.METAL);
        IRIDIUM_BOX_BLOCK = registerTeBlock(TeBlock.iridium_storage_box, MapColor.PURPLE, BlockSoundGroup.METAL);
        // one block per pipe type and size, like the cables; the small bronze pipe answers for
        // TeBlock.fluid_pipe. There is no separate ic2:fluid_pipe block any more.
        registerPipeVariants();
        FLUID_PIPE_BLOCK = BLOCKS.get(TeBlock.fluid_pipe);
        COKE_HATCH_BLOCK = registerTeBlock(TeBlock.coke_kiln_hatch, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        COKE_GRATE_BLOCK = registerTeBlock(TeBlock.coke_kiln_grate, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        SOLAR_DISTILLER_BLOCK = registerTeBlock(TeBlock.solar_distiller, MapColor.CLEAR, BlockSoundGroup.GLASS);
        CREATIVE_GENERATOR_BLOCK = registerTeBlock(TeBlock.creative_generator, MapColor.PURPLE, BlockSoundGroup.STONE);
        RCI_RSH_BLOCK = registerTeBlock(TeBlock.rci_rsh, MapColor.RED, BlockSoundGroup.STONE);
        RCI_LZH_BLOCK = registerTeBlock(TeBlock.rci_lzh, MapColor.BLUE, BlockSoundGroup.STONE);
        STEAM_REPRESSURIZER_BLOCK = registerTeBlock(TeBlock.steam_repressurizer, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        BARREL_BLOCK = registerTeBlock(TeBlock.barrel, MapColor.BROWN, BlockSoundGroup.WOOD);
        WALL_BLOCK = registerTeBlock(TeBlock.wall, MapColor.GRAY, BlockSoundGroup.STONE);
        ITNT_BLOCK = registerTeBlock(TeBlock.itnt, MapColor.RED, BlockSoundGroup.WOOD);
        NUKE_BLOCK = registerTeBlock(TeBlock.nuke, MapColor.RED, BlockSoundGroup.STONE);
        ITEM_BUFFER_BLOCK = registerTeBlock(TeBlock.item_buffer, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        ITEM_BUFFER_2_BLOCK = registerTeBlock(TeBlock.item_buffer_2, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        CHARGEPAD_BATBOX_BLOCK = registerTeBlock(TeBlock.chargepad_batbox, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        CHARGEPAD_CESU_BLOCK = registerTeBlock(TeBlock.chargepad_cesu, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        CHARGEPAD_MFE_BLOCK = registerTeBlock(TeBlock.chargepad_mfe, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        CHARGEPAD_MFSU_BLOCK = registerTeBlock(TeBlock.chargepad_mfsu, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        LUMINATOR_FLAT_BLOCK = registerTeBlock(TeBlock.luminator_flat, MapColor.WHITE, BlockSoundGroup.GLASS);
        PATTERN_STORAGE_BLOCK = registerTeBlock(TeBlock.pattern_storage, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        REPLICATOR_BLOCK = registerTeBlock(TeBlock.replicator, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        SCANNER_BLOCK = registerTeBlock(TeBlock.scanner, MapColor.STONE_GRAY, BlockSoundGroup.STONE);
        TRADING_TERMINAL_BLOCK = registerTeBlock(TeBlock.trading_terminal, MapColor.STONE_GRAY, BlockSoundGroup.WOOD);
        UU_ASSEMBLY_BENCH_BLOCK = registerTeBlock(TeBlock.uu_assembly_bench, MapColor.STONE_GRAY, BlockSoundGroup.STONE);

        // BlockEntityTypes - use FabricBlockEntityTypeBuilder
        BATBOX_BE = registerBE("batbox", FabricBlockEntityTypeBuilder.create(TileEntityElectricBatBox::new, BATBOX_BLOCK).build());
        CESU_BE = registerBE("cesu", FabricBlockEntityTypeBuilder.create(TileEntityElectricCESU::new, CESU_BLOCK).build());
        MFE_BE = registerBE("mfe", FabricBlockEntityTypeBuilder.create(TileEntityElectricMFE::new, MFE_BLOCK).build());
        MFSU_BE = registerBE("mfsu", FabricBlockEntityTypeBuilder.create(TileEntityElectricMFSU::new, MFSU_BLOCK).build());
        // every cable variant is its own block, and they all run on the one cable block entity
        Block[] cableBlocks = CABLE_VARIANTS.toArray(Block[]::new);
        CABLE_BE = registerBE("cable", FabricBlockEntityTypeBuilder.create(TileEntityCable::create, cableBlocks).build());
        LV_TRANSFORMER_BE = registerBE("lv_transformer", FabricBlockEntityTypeBuilder.create(TileEntityTransformerLV::new, LV_TRANSFORMER_BLOCK).build());
        MV_TRANSFORMER_BE = registerBE("mv_transformer", FabricBlockEntityTypeBuilder.create(TileEntityTransformerMV::new, MV_TRANSFORMER_BLOCK).build());
        HV_TRANSFORMER_BE = registerBE("hv_transformer", FabricBlockEntityTypeBuilder.create(TileEntityTransformerHV::new, HV_TRANSFORMER_BLOCK).build());
        EV_TRANSFORMER_BE = registerBE("ev_transformer", FabricBlockEntityTypeBuilder.create(TileEntityTransformerEV::new, EV_TRANSFORMER_BLOCK).build());
        MACERATOR_BE = registerBE("macerator", FabricBlockEntityTypeBuilder.create(TileEntityMacerator::new, MACERATOR_BLOCK).build());
        COMPRESSOR_BE = registerBE("compressor", FabricBlockEntityTypeBuilder.create(TileEntityCompressor::new, COMPRESSOR_BLOCK).build());
        EXTRACTOR_BE = registerBE("extractor", FabricBlockEntityTypeBuilder.create(TileEntityExtractor::new, EXTRACTOR_BLOCK).build());
        ELECTRIC_FURNACE_BE = registerBE("electric_furnace", FabricBlockEntityTypeBuilder.create(TileEntityElectricFurnace::new, ELECTRIC_FURNACE_BLOCK).build());
        RECYCLER_BE = registerBE("recycler", FabricBlockEntityTypeBuilder.create(TileEntityRecycler::new, RECYCLER_BLOCK).build());
        CENTRIFUGE_BE = registerBE("centrifuge", FabricBlockEntityTypeBuilder.create(TileEntityCentrifuge::new, CENTRIFUGE_BLOCK).build());
        BLAST_FURNACE_BE = registerBE("blast_furnace", FabricBlockEntityTypeBuilder.create(TileEntityBlastFurnace::new, BLAST_FURNACE_BLOCK).build());
        METAL_FORMER_BE = registerBE("metal_former", FabricBlockEntityTypeBuilder.create(TileEntityMetalFormer::new, METAL_FORMER_BLOCK).build());
        ORE_WASHING_BE = registerBE("ore_washing", FabricBlockEntityTypeBuilder.create(TileEntityOreWashing::new, ORE_WASHING_BLOCK).build());
        BLOCK_CUTTER_BE = registerBE("block_cutter", FabricBlockEntityTypeBuilder.create(TileEntityBlockCutter::new, BLOCK_CUTTER_BLOCK).build());
        CANNER_BE = registerBE("canner", FabricBlockEntityTypeBuilder.create(TileEntityCanner::new, CANNER_BLOCK).build());
        SOLID_CANNER_BE = registerBE("solid_canner", FabricBlockEntityTypeBuilder.create(TileEntitySolidCanner::new, SOLID_CANNER_BLOCK).build());
        FLUID_BOTTLER_BE = registerBE("fluid_bottler", FabricBlockEntityTypeBuilder.create(TileEntityFluidBottler::new, FLUID_BOTTLER_BLOCK).build());
        INDUCTION_BE = registerBE("induction_furnace", FabricBlockEntityTypeBuilder.create(TileEntityInduction::new, INDUCTION_FURNACE_BLOCK).build());
        MASS_FABRICATOR_BE = registerBE("mass_fabricator", FabricBlockEntityTypeBuilder.create(TileEntityMassFabricator::new, MASS_FABRICATOR_BLOCK).build());
        MATTER_BE = registerBE("matter", FabricBlockEntityTypeBuilder.create(TileEntityMatter::new, MATTER_BLOCK).build());
        GENERATOR_BE = registerBE("generator", FabricBlockEntityTypeBuilder.create(TileEntityGenerator::new, GENERATOR_BLOCK).build());
        SOLAR_BE = registerBE("solar_generator", FabricBlockEntityTypeBuilder.create(TileEntitySolarGenerator::new, SOLAR_BLOCK).build());
        WATER_BE = registerBE("water_generator", FabricBlockEntityTypeBuilder.create(TileEntityWaterGenerator::new, WATER_BLOCK).build());
        WIND_BE = registerBE("wind_generator", FabricBlockEntityTypeBuilder.create(TileEntityWindGenerator::new, WIND_BLOCK).build());
        GEOTHERMAL_BE = registerBE("geo_generator", FabricBlockEntityTypeBuilder.create(TileEntityGeoGenerator::new, GEOTHERMAL_BLOCK).build());
        SEMIFLUID_BE = registerBE("semifluid_generator", FabricBlockEntityTypeBuilder.create(TileEntitySemifluidGenerator::new, SEMIFLUID_BLOCK).build());
        RT_BE = registerBE("rt_generator", FabricBlockEntityTypeBuilder.create(TileEntityRTGenerator::new, RT_BLOCK).build());
        STIRLING_BE = registerBE("stirling_generator", FabricBlockEntityTypeBuilder.create(TileEntityStirlingGenerator::new, STIRLING_BLOCK).build());
        KINETIC_BE = registerBE("kinetic_generator", FabricBlockEntityTypeBuilder.create(TileEntityKineticGenerator::new, KINETIC_BLOCK).build());
        SOLID_HEAT_BE = registerBE("solid_heat", FabricBlockEntityTypeBuilder.create(TileEntitySolidHeatGenerator::new, SOLID_HEAT_BLOCK).build());
        FLUID_HEAT_BE = registerBE("fluid_heat", FabricBlockEntityTypeBuilder.create(TileEntityFluidHeatGenerator::new, FLUID_HEAT_BLOCK).build());
        ELECTRIC_HEAT_BE = registerBE("electric_heat", FabricBlockEntityTypeBuilder.create(TileEntityElectricHeatGenerator::new, ELECTRIC_HEAT_BLOCK).build());
        RT_HEAT_BE = registerBE("rt_heat", FabricBlockEntityTypeBuilder.create(TileEntityRTHeatGenerator::new, RT_HEAT_BLOCK).build());
        ELECTRIC_KINETIC_BE = registerBE("electric_kinetic", FabricBlockEntityTypeBuilder.create(TileEntityElectricKineticGenerator::new, ELECTRIC_KINETIC_BLOCK).build());
        MANUAL_KINETIC_BE = registerBE("manual_kinetic", FabricBlockEntityTypeBuilder.create(TileEntityManualKineticGenerator::new, MANUAL_KINETIC_BLOCK).build());
        STEAM_KINETIC_BE = registerBE("steam_kinetic", FabricBlockEntityTypeBuilder.create(TileEntitySteamKineticGenerator::new, STEAM_KINETIC_BLOCK).build());
        STIRLING_KINETIC_BE = registerBE("stirling_kinetic", FabricBlockEntityTypeBuilder.create(TileEntityStirlingKineticGenerator::new, STIRLING_KINETIC_BLOCK).build());
        WATER_KINETIC_BE = registerBE("water_kinetic", FabricBlockEntityTypeBuilder.create(TileEntityWaterKineticGenerator::new, WATER_KINETIC_BLOCK).build());
        WIND_KINETIC_BE = registerBE("wind_kinetic", FabricBlockEntityTypeBuilder.create(TileEntityWindKineticGenerator::new, WIND_KINETIC_BLOCK).build());
        NUCLEAR_REACTOR_BE = registerBE("nuclear_reactor", FabricBlockEntityTypeBuilder.create(TileEntityNuclearReactorElectric::new, NUCLEAR_REACTOR_BLOCK).build());
        REACTOR_CHAMBER_BE = registerBE("reactor_chamber", FabricBlockEntityTypeBuilder.create(TileEntityReactorChamberElectric::new, REACTOR_CHAMBER_BLOCK).build());
        REACTOR_HATCH_BE = registerBE("reactor_hatch", FabricBlockEntityTypeBuilder.create(TileEntityReactorAccessHatch::new, REACTOR_HATCH_BLOCK).build());
        REACTOR_FLUID_PORT_BE = registerBE("reactor_fluid_port", FabricBlockEntityTypeBuilder.create(TileEntityReactorFluidPort::new, REACTOR_FLUID_PORT_BLOCK).build());
        REACTOR_REDSTONE_PORT_BE = registerBE("reactor_redstone_port", FabricBlockEntityTypeBuilder.create(TileEntityReactorRedstonePort::new, REACTOR_REDSTONE_PORT_BLOCK).build());
        REACTOR_VESSEL_BE = registerBE("reactor_vessel", FabricBlockEntityTypeBuilder.create(TileEntityReactorVessel::new, REACTOR_VESSEL_BLOCK).build());
        ENERGY_O_MAT_BE = registerBE("energy_o_mat", FabricBlockEntityTypeBuilder.create(TileEntityEnergyOMat::new, ENERGY_O_MAT_BLOCK).build());
        PERSONAL_CHEST_BE = registerBE("personal_chest", FabricBlockEntityTypeBuilder.create(TileEntityPersonalChest::new, PERSONAL_CHEST_BLOCK).build());
        TRADE_O_MAT_BE = registerBE("trade_o_mat", FabricBlockEntityTypeBuilder.create(TileEntityTradeOMat::new, TRADE_O_MAT_BLOCK).build());
        CROP_BE = registerBE("crop", FabricBlockEntityTypeBuilder.create(TileEntityCrop::new, CROP_BLOCK).build());
        BRONZE_TANK_BE = registerBE("bronze_tank", FabricBlockEntityTypeBuilder.create(TileEntityBronzeTank::new, BRONZE_TANK_BLOCK).build());
        IRON_TANK_BE = registerBE("iron_tank", FabricBlockEntityTypeBuilder.create(TileEntityIronTank::new, IRON_TANK_BLOCK).build());
        STEEL_TANK_BE = registerBE("steel_tank", FabricBlockEntityTypeBuilder.create(TileEntitySteelTank::new, STEEL_TANK_BLOCK).build());
        IRIDIUM_TANK_BE = registerBE("iridium_tank", FabricBlockEntityTypeBuilder.create(TileEntityIridiumTank::new, IRIDIUM_TANK_BLOCK).build());
        PUMP_BE = registerBE("pump", FabricBlockEntityTypeBuilder.create(TileEntityPump::new, PUMP_BLOCK).build());
        MINER_BE = registerBE("miner", FabricBlockEntityTypeBuilder.create(TileEntityMiner::new, MINER_BLOCK).build());
        ADV_MINER_BE = registerBE("advanced_miner", FabricBlockEntityTypeBuilder.create(TileEntityAdvMiner::new, ADV_MINER_BLOCK).build());
        CROP_HARVESTER_BE = registerBE("crop_harvester", FabricBlockEntityTypeBuilder.create(TileEntityCropHarvester::new, CROP_HARVESTER_BLOCK).build());
        CROPMATRON_BE = registerBE("cropmatron", FabricBlockEntityTypeBuilder.create(TileEntityCropmatron::new, CROPMATRON_BLOCK).build());
        LIQUID_HEAT_EXCHANGER_BE = registerBE("liquid_heat_exchanger", FabricBlockEntityTypeBuilder.create(TileEntityLiquidHeatExchanger::new, LIQUID_HEAT_EXCHANGER_BLOCK).build());
        CONDENSER_BE = registerBE("condenser", FabricBlockEntityTypeBuilder.create(TileEntityCondenser::new, CONDENSER_BLOCK).build());
        TELEPORTER_BE = registerBE("teleporter", FabricBlockEntityTypeBuilder.create(TileEntityTeleporter::new, TELEPORTER_BLOCK).build());
        MAGNETIZER_BE = registerBE("magnetizer", FabricBlockEntityTypeBuilder.create(TileEntityMagnetizer::new, MAGNETIZER_BLOCK).build());
        TESLA_BE = registerBE("tesla", FabricBlockEntityTypeBuilder.create(TileEntityTesla::new, TESLA_BLOCK).build());
        SORTING_BE = registerBE("sorting_machine", FabricBlockEntityTypeBuilder.create(TileEntitySortingMachine::new, SORTING_BLOCK).build());
        ELECTROLYZER_BE = registerBE("electrolyzer", FabricBlockEntityTypeBuilder.create(TileEntityElectrolyzer::new, ELECTROLYZER_BLOCK).build());
        FERMENTER_BE = registerBE("fermenter", FabricBlockEntityTypeBuilder.create(TileEntityFermenter::new, FERMENTER_BLOCK).build());
        COKE_KILN_BE = registerBE("coke_kiln", FabricBlockEntityTypeBuilder.create(TileEntityCokeKiln::new, COKE_KILN_BLOCK).build());
        BURNING_BOX_BE = registerBE("burning_box", FabricBlockEntityTypeBuilder.create(TileEntityBurningBox::new, BURNING_BOX_BLOCK).build());
        STEAM_GENERATOR_BE = registerBE("steam_generator", FabricBlockEntityTypeBuilder.create(TileEntitySteamGenerator::new, STEAM_GENERATOR_BLOCK).build());
        TANK_BE = registerBE("tank", FabricBlockEntityTypeBuilder.create(TileEntityTank::new, TANK_BLOCK).build());
        TERRAFORMER_BE = registerBE("terraformer", FabricBlockEntityTypeBuilder.create(TileEntityTerraformer::new, TERRAFORMER_BLOCK).build());
        CHUNK_LOADER_BE = registerBE("chunk_loader", FabricBlockEntityTypeBuilder.create(TileEntityChunkloader::new, CHUNK_LOADER_BLOCK).build());
        IRON_FURNACE_BE = registerBE("iron_furnace", FabricBlockEntityTypeBuilder.create(TileEntityIronFurnace::new, IRON_FURNACE_BLOCK).build());
        FLUID_DISTRIBUTOR_BE = registerBE("fluid_distributor", FabricBlockEntityTypeBuilder.create(TileEntityFluidDistributor::new, FLUID_DISTRIBUTOR_BLOCK).build());
        FLUID_REGULATOR_BE = registerBE("fluid_regulator", FabricBlockEntityTypeBuilder.create(TileEntityFluidRegulator::new, FLUID_REGULATOR_BLOCK).build());
        WEIGHTED_FLUID_DISTRIBUTOR_BE = registerBE("weighted_fluid_distributor", FabricBlockEntityTypeBuilder.create(TileEntityWeightedFluidDistributor::new, WEIGHTED_FLUID_DISTRIBUTOR_BLOCK).build());
        WEIGHTED_ITEM_DISTRIBUTOR_BE = registerBE("weighted_item_distributor", FabricBlockEntityTypeBuilder.create(TileEntityWeightedItemDistributor::new, WEIGHTED_ITEM_DISTRIBUTOR_BLOCK).build());
        INDUSTRIAL_WORKBENCH_BE = registerBE("industrial_workbench", FabricBlockEntityTypeBuilder.create(TileEntityIndustrialWorkbench::new, INDUSTRIAL_WORKBENCH_BLOCK).build());
        BATCH_CRAFTER_BE = registerBE("batch_crafter", FabricBlockEntityTypeBuilder.create(TileEntityBatchCrafter::new, BATCH_CRAFTER_BLOCK).build());
        WOODEN_BOX_BE = registerBE("wooden_box", FabricBlockEntityTypeBuilder.create(TileEntityWoodenStorageBox::new, WOODEN_BOX_BLOCK).build());
        IRON_BOX_BE = registerBE("iron_box", FabricBlockEntityTypeBuilder.create(TileEntityIronStorageBox::new, IRON_BOX_BLOCK).build());
        BRONZE_BOX_BE = registerBE("bronze_box", FabricBlockEntityTypeBuilder.create(TileEntityBronzeStorageBox::new, BRONZE_BOX_BLOCK).build());
        STEEL_BOX_BE = registerBE("steel_box", FabricBlockEntityTypeBuilder.create(TileEntitySteelStorageBox::new, STEEL_BOX_BLOCK).build());
        IRIDIUM_BOX_BE = registerBE("iridium_box", FabricBlockEntityTypeBuilder.create(TileEntityIridiumStorageBox::new, IRIDIUM_BOX_BLOCK).build());
        FLUID_PIPE_BE = registerBE("fluid_pipe", FabricBlockEntityTypeBuilder.create(TileEntityFluidPipe::new, PIPE_VARIANTS.toArray(Block[]::new)).build());
        COKE_HATCH_BE = registerBE("coke_hatch", FabricBlockEntityTypeBuilder.create(TileEntityCokeKilnHatch::new, COKE_HATCH_BLOCK).build());
        COKE_GRATE_BE = registerBE("coke_grate", FabricBlockEntityTypeBuilder.create(TileEntityCokeKilnGrate::new, COKE_GRATE_BLOCK).build());
        SOLAR_DISTILLER_BE = registerBE("solar_distiller", FabricBlockEntityTypeBuilder.create(TileEntitySolarDistiller::new, SOLAR_DISTILLER_BLOCK).build());
        CREATIVE_GENERATOR_BE = registerBE("creative_generator", FabricBlockEntityTypeBuilder.create(TileEntityCreativeGenerator::new, CREATIVE_GENERATOR_BLOCK).build());
        RCI_RSH_BE = registerBE("rci_rsh", FabricBlockEntityTypeBuilder.create(TileEntityRCI_RSH::new, RCI_RSH_BLOCK).build());
        RCI_LZH_BE = registerBE("rci_lzh", FabricBlockEntityTypeBuilder.create(TileEntityRCI_LZH::new, RCI_LZH_BLOCK).build());
        STEAM_REPRESSURIZER_BE = registerBE("steam_repressurizer", FabricBlockEntityTypeBuilder.create(TileEntitySteamRepressurizer::new, STEAM_REPRESSURIZER_BLOCK).build());
        BARREL_BE = registerBE("barrel", FabricBlockEntityTypeBuilder.create(TileEntityBarrel::new, BARREL_BLOCK).build());
        WALL_BE = registerBE("wall", FabricBlockEntityTypeBuilder.create(TileEntityWall::new, WALL_BLOCK).build());
        ITNT_BE = registerBE("itnt", FabricBlockEntityTypeBuilder.create(ic2.core.block.machine.tileentity.TileEntityITnt::new, ITNT_BLOCK).build());
        NUKE_BE = registerBE("nuke", FabricBlockEntityTypeBuilder.create(TileEntityNuke::new, NUKE_BLOCK).build());
        ITEM_BUFFER_BE = registerBE("item_buffer", FabricBlockEntityTypeBuilder.create(TileEntityItemBuffer::new, ITEM_BUFFER_BLOCK).build());
        ITEM_BUFFER_2_BE = registerBE("item_buffer_2", FabricBlockEntityTypeBuilder.create(ic2.core.block.machine.tileentity.TileEntityBetterItemBuffer::new, ITEM_BUFFER_2_BLOCK).build());
        CHARGEPAD_BATBOX_BE = registerBE("chargepad_batbox", FabricBlockEntityTypeBuilder.create(ic2.core.block.wiring.TileEntityChargepadBatBox::new, CHARGEPAD_BATBOX_BLOCK).build());
        CHARGEPAD_CESU_BE = registerBE("chargepad_cesu", FabricBlockEntityTypeBuilder.create(ic2.core.block.wiring.TileEntityChargepadCESU::new, CHARGEPAD_CESU_BLOCK).build());
        CHARGEPAD_MFE_BE = registerBE("chargepad_mfe", FabricBlockEntityTypeBuilder.create(ic2.core.block.wiring.TileEntityChargepadMFE::new, CHARGEPAD_MFE_BLOCK).build());
        CHARGEPAD_MFSU_BE = registerBE("chargepad_mfsu", FabricBlockEntityTypeBuilder.create(ic2.core.block.wiring.TileEntityChargepadMFSU::new, CHARGEPAD_MFSU_BLOCK).build());
        LUMINATOR_FLAT_BE = registerBE("luminator_flat", FabricBlockEntityTypeBuilder.create(ic2.core.block.wiring.TileEntityLuminator::new, LUMINATOR_FLAT_BLOCK).build());
        PATTERN_STORAGE_BE = registerBE("pattern_storage", FabricBlockEntityTypeBuilder.create(TileEntityPatternStorage::new, PATTERN_STORAGE_BLOCK).build());
        REPLICATOR_BE = registerBE("replicator", FabricBlockEntityTypeBuilder.create(TileEntityReplicator::new, REPLICATOR_BLOCK).build());
        SCANNER_BE = registerBE("scanner", FabricBlockEntityTypeBuilder.create(TileEntityScanner::new, SCANNER_BLOCK).build());
        TRADING_TERMINAL_BE = registerBE("trading_terminal", FabricBlockEntityTypeBuilder.create(TileEntityTradingTerminal::new, TRADING_TERMINAL_BLOCK).build());
        UU_ASSEMBLY_BENCH_BE = registerBE("uu_assembly_bench", FabricBlockEntityTypeBuilder.create(TileEntityAssemblyBench::new, UU_ASSEMBLY_BENCH_BLOCK).build());

        // Wire BlockTileEntity's BlockEntityType back to the block (for createBlockEntity)
        ((BlockTileEntity) BATBOX_BLOCK).setBlockEntityType(BATBOX_BE);
        ((BlockTileEntity) CESU_BLOCK).setBlockEntityType(CESU_BE);
        ((BlockTileEntity) MFE_BLOCK).setBlockEntityType(MFE_BE);
        ((BlockTileEntity) MFSU_BLOCK).setBlockEntityType(MFSU_BE);
        // every variant needs the type, not just the one CABLE_BLOCK points at, or placing an
        // insulated cable leaves a block with no block entity behind it
        for (Block cable : CABLE_VARIANTS) {
            ((BlockTileEntity) cable).setBlockEntityType(CABLE_BE);
        }
        ((BlockTileEntity) LV_TRANSFORMER_BLOCK).setBlockEntityType(LV_TRANSFORMER_BE);
        ((BlockTileEntity) MV_TRANSFORMER_BLOCK).setBlockEntityType(MV_TRANSFORMER_BE);
        ((BlockTileEntity) HV_TRANSFORMER_BLOCK).setBlockEntityType(HV_TRANSFORMER_BE);
        ((BlockTileEntity) EV_TRANSFORMER_BLOCK).setBlockEntityType(EV_TRANSFORMER_BE);
        ((BlockTileEntity) MACERATOR_BLOCK).setBlockEntityType(MACERATOR_BE);
        ((BlockTileEntity) COMPRESSOR_BLOCK).setBlockEntityType(COMPRESSOR_BE);
        ((BlockTileEntity) EXTRACTOR_BLOCK).setBlockEntityType(EXTRACTOR_BE);
        ((BlockTileEntity) ELECTRIC_FURNACE_BLOCK).setBlockEntityType(ELECTRIC_FURNACE_BE);
        ((BlockTileEntity) RECYCLER_BLOCK).setBlockEntityType(RECYCLER_BE);
        ((BlockTileEntity) CENTRIFUGE_BLOCK).setBlockEntityType(CENTRIFUGE_BE);
        ((BlockTileEntity) BLAST_FURNACE_BLOCK).setBlockEntityType(BLAST_FURNACE_BE);
        ((BlockTileEntity) METAL_FORMER_BLOCK).setBlockEntityType(METAL_FORMER_BE);
        ((BlockTileEntity) ORE_WASHING_BLOCK).setBlockEntityType(ORE_WASHING_BE);
        ((BlockTileEntity) BLOCK_CUTTER_BLOCK).setBlockEntityType(BLOCK_CUTTER_BE);
        ((BlockTileEntity) CANNER_BLOCK).setBlockEntityType(CANNER_BE);
        ((BlockTileEntity) SOLID_CANNER_BLOCK).setBlockEntityType(SOLID_CANNER_BE);
        ((BlockTileEntity) FLUID_BOTTLER_BLOCK).setBlockEntityType(FLUID_BOTTLER_BE);
        ((BlockTileEntity) INDUCTION_FURNACE_BLOCK).setBlockEntityType(INDUCTION_BE);
        ((BlockTileEntity) MASS_FABRICATOR_BLOCK).setBlockEntityType(MASS_FABRICATOR_BE);
        ((BlockTileEntity) MATTER_BLOCK).setBlockEntityType(MATTER_BE);
        ((BlockTileEntity) GENERATOR_BLOCK).setBlockEntityType(GENERATOR_BE);
        ((BlockTileEntity) SOLAR_BLOCK).setBlockEntityType(SOLAR_BE);
        ((BlockTileEntity) WATER_BLOCK).setBlockEntityType(WATER_BE);
        ((BlockTileEntity) WIND_BLOCK).setBlockEntityType(WIND_BE);
        ((BlockTileEntity) GEOTHERMAL_BLOCK).setBlockEntityType(GEOTHERMAL_BE);
        ((BlockTileEntity) SEMIFLUID_BLOCK).setBlockEntityType(SEMIFLUID_BE);
        ((BlockTileEntity) RT_BLOCK).setBlockEntityType(RT_BE);
        ((BlockTileEntity) STIRLING_BLOCK).setBlockEntityType(STIRLING_BE);
        ((BlockTileEntity) KINETIC_BLOCK).setBlockEntityType(KINETIC_BE);
        ((BlockTileEntity) SOLID_HEAT_BLOCK).setBlockEntityType(SOLID_HEAT_BE);
        ((BlockTileEntity) FLUID_HEAT_BLOCK).setBlockEntityType(FLUID_HEAT_BE);
        ((BlockTileEntity) ELECTRIC_HEAT_BLOCK).setBlockEntityType(ELECTRIC_HEAT_BE);
        ((BlockTileEntity) RT_HEAT_BLOCK).setBlockEntityType(RT_HEAT_BE);
        ((BlockTileEntity) ELECTRIC_KINETIC_BLOCK).setBlockEntityType(ELECTRIC_KINETIC_BE);
        ((BlockTileEntity) MANUAL_KINETIC_BLOCK).setBlockEntityType(MANUAL_KINETIC_BE);
        ((BlockTileEntity) STEAM_KINETIC_BLOCK).setBlockEntityType(STEAM_KINETIC_BE);
        ((BlockTileEntity) STIRLING_KINETIC_BLOCK).setBlockEntityType(STIRLING_KINETIC_BE);
        ((BlockTileEntity) WATER_KINETIC_BLOCK).setBlockEntityType(WATER_KINETIC_BE);
        ((BlockTileEntity) WIND_KINETIC_BLOCK).setBlockEntityType(WIND_KINETIC_BE);
        ((BlockTileEntity) NUCLEAR_REACTOR_BLOCK).setBlockEntityType(NUCLEAR_REACTOR_BE);
        ((BlockTileEntity) REACTOR_CHAMBER_BLOCK).setBlockEntityType(REACTOR_CHAMBER_BE);
        ((BlockTileEntity) REACTOR_HATCH_BLOCK).setBlockEntityType(REACTOR_HATCH_BE);
        ((BlockTileEntity) REACTOR_FLUID_PORT_BLOCK).setBlockEntityType(REACTOR_FLUID_PORT_BE);
        ((BlockTileEntity) REACTOR_REDSTONE_PORT_BLOCK).setBlockEntityType(REACTOR_REDSTONE_PORT_BE);
        ((BlockTileEntity) REACTOR_VESSEL_BLOCK).setBlockEntityType(REACTOR_VESSEL_BE);
        ((BlockTileEntity) ENERGY_O_MAT_BLOCK).setBlockEntityType(ENERGY_O_MAT_BE);
        ((BlockTileEntity) PERSONAL_CHEST_BLOCK).setBlockEntityType(PERSONAL_CHEST_BE);
        ((BlockTileEntity) TRADE_O_MAT_BLOCK).setBlockEntityType(TRADE_O_MAT_BE);
        ((BlockTileEntity) CROP_BLOCK).setBlockEntityType(CROP_BE);
        ((BlockTileEntity) BRONZE_TANK_BLOCK).setBlockEntityType(BRONZE_TANK_BE);
        ((BlockTileEntity) IRON_TANK_BLOCK).setBlockEntityType(IRON_TANK_BE);
        ((BlockTileEntity) STEEL_TANK_BLOCK).setBlockEntityType(STEEL_TANK_BE);
        ((BlockTileEntity) IRIDIUM_TANK_BLOCK).setBlockEntityType(IRIDIUM_TANK_BE);
        ((BlockTileEntity) PUMP_BLOCK).setBlockEntityType(PUMP_BE);
        ((BlockTileEntity) MINER_BLOCK).setBlockEntityType(MINER_BE);
        ((BlockTileEntity) ADV_MINER_BLOCK).setBlockEntityType(ADV_MINER_BE);
        ((BlockTileEntity) CROP_HARVESTER_BLOCK).setBlockEntityType(CROP_HARVESTER_BE);
        ((BlockTileEntity) CROPMATRON_BLOCK).setBlockEntityType(CROPMATRON_BE);
        ((BlockTileEntity) LIQUID_HEAT_EXCHANGER_BLOCK).setBlockEntityType(LIQUID_HEAT_EXCHANGER_BE);
        ((BlockTileEntity) CONDENSER_BLOCK).setBlockEntityType(CONDENSER_BE);
        ((BlockTileEntity) TELEPORTER_BLOCK).setBlockEntityType(TELEPORTER_BE);
        ((BlockTileEntity) MAGNETIZER_BLOCK).setBlockEntityType(MAGNETIZER_BE);
        ((BlockTileEntity) TESLA_BLOCK).setBlockEntityType(TESLA_BE);
        ((BlockTileEntity) SORTING_BLOCK).setBlockEntityType(SORTING_BE);
        ((BlockTileEntity) ELECTROLYZER_BLOCK).setBlockEntityType(ELECTROLYZER_BE);
        ((BlockTileEntity) FERMENTER_BLOCK).setBlockEntityType(FERMENTER_BE);
        ((BlockTileEntity) COKE_KILN_BLOCK).setBlockEntityType(COKE_KILN_BE);
        ((BlockTileEntity) BURNING_BOX_BLOCK).setBlockEntityType(BURNING_BOX_BE);
        ((BlockTileEntity) STEAM_GENERATOR_BLOCK).setBlockEntityType(STEAM_GENERATOR_BE);
        ((BlockTileEntity) TANK_BLOCK).setBlockEntityType(TANK_BE);
        ((BlockTileEntity) TERRAFORMER_BLOCK).setBlockEntityType(TERRAFORMER_BE);
        ((BlockTileEntity) CHUNK_LOADER_BLOCK).setBlockEntityType(CHUNK_LOADER_BE);
        ((BlockTileEntity) IRON_FURNACE_BLOCK).setBlockEntityType(IRON_FURNACE_BE);
        ((BlockTileEntity) FLUID_DISTRIBUTOR_BLOCK).setBlockEntityType(FLUID_DISTRIBUTOR_BE);
        ((BlockTileEntity) FLUID_REGULATOR_BLOCK).setBlockEntityType(FLUID_REGULATOR_BE);
        ((BlockTileEntity) WEIGHTED_FLUID_DISTRIBUTOR_BLOCK).setBlockEntityType(WEIGHTED_FLUID_DISTRIBUTOR_BE);
        ((BlockTileEntity) WEIGHTED_ITEM_DISTRIBUTOR_BLOCK).setBlockEntityType(WEIGHTED_ITEM_DISTRIBUTOR_BE);
        ((BlockTileEntity) INDUSTRIAL_WORKBENCH_BLOCK).setBlockEntityType(INDUSTRIAL_WORKBENCH_BE);
        ((BlockTileEntity) BATCH_CRAFTER_BLOCK).setBlockEntityType(BATCH_CRAFTER_BE);
        ((BlockTileEntity) WOODEN_BOX_BLOCK).setBlockEntityType(WOODEN_BOX_BE);
        ((BlockTileEntity) IRON_BOX_BLOCK).setBlockEntityType(IRON_BOX_BE);
        ((BlockTileEntity) BRONZE_BOX_BLOCK).setBlockEntityType(BRONZE_BOX_BE);
        ((BlockTileEntity) STEEL_BOX_BLOCK).setBlockEntityType(STEEL_BOX_BE);
        ((BlockTileEntity) IRIDIUM_BOX_BLOCK).setBlockEntityType(IRIDIUM_BOX_BE);
        for (Block pipe : PIPE_VARIANTS) {
            ((BlockTileEntity) pipe).setBlockEntityType(FLUID_PIPE_BE);
        }
        ((BlockTileEntity) COKE_HATCH_BLOCK).setBlockEntityType(COKE_HATCH_BE);
        ((BlockTileEntity) COKE_GRATE_BLOCK).setBlockEntityType(COKE_GRATE_BE);
        ((BlockTileEntity) SOLAR_DISTILLER_BLOCK).setBlockEntityType(SOLAR_DISTILLER_BE);
        ((BlockTileEntity) CREATIVE_GENERATOR_BLOCK).setBlockEntityType(CREATIVE_GENERATOR_BE);
        ((BlockTileEntity) RCI_RSH_BLOCK).setBlockEntityType(RCI_RSH_BE);
        ((BlockTileEntity) RCI_LZH_BLOCK).setBlockEntityType(RCI_LZH_BE);
        ((BlockTileEntity) STEAM_REPRESSURIZER_BLOCK).setBlockEntityType(STEAM_REPRESSURIZER_BE);
        ((BlockTileEntity) BARREL_BLOCK).setBlockEntityType(BARREL_BE);
        ((BlockTileEntity) WALL_BLOCK).setBlockEntityType(WALL_BE);
        ((BlockTileEntity) ITNT_BLOCK).setBlockEntityType(ITNT_BE);
        ((BlockTileEntity) NUKE_BLOCK).setBlockEntityType(NUKE_BE);
        ((BlockTileEntity) ITEM_BUFFER_BLOCK).setBlockEntityType(ITEM_BUFFER_BE);
        ((BlockTileEntity) ITEM_BUFFER_2_BLOCK).setBlockEntityType(ITEM_BUFFER_2_BE);
        ((BlockTileEntity) CHARGEPAD_BATBOX_BLOCK).setBlockEntityType(CHARGEPAD_BATBOX_BE);
        ((BlockTileEntity) CHARGEPAD_CESU_BLOCK).setBlockEntityType(CHARGEPAD_CESU_BE);
        ((BlockTileEntity) CHARGEPAD_MFE_BLOCK).setBlockEntityType(CHARGEPAD_MFE_BE);
        ((BlockTileEntity) CHARGEPAD_MFSU_BLOCK).setBlockEntityType(CHARGEPAD_MFSU_BE);
        ((BlockTileEntity) LUMINATOR_FLAT_BLOCK).setBlockEntityType(LUMINATOR_FLAT_BE);
        ((BlockTileEntity) PATTERN_STORAGE_BLOCK).setBlockEntityType(PATTERN_STORAGE_BE);
        ((BlockTileEntity) REPLICATOR_BLOCK).setBlockEntityType(REPLICATOR_BE);
        ((BlockTileEntity) SCANNER_BLOCK).setBlockEntityType(SCANNER_BE);
        ((BlockTileEntity) TRADING_TERMINAL_BLOCK).setBlockEntityType(TRADING_TERMINAL_BE);
        ((BlockTileEntity) UU_ASSEMBLY_BENCH_BLOCK).setBlockEntityType(UU_ASSEMBLY_BENCH_BE);
    }

    /**
     * Registers a tile entity block with the hardness, blast resistance, tool requirement and
     * rarity its TeBlock had in 1.12.2. A hardness of -1 is a block only its owner takes out.
     */
    private static Block registerTeBlock(TeBlock teBlock, MapColor mapColor, BlockSoundGroup sound) {
        AbstractBlock.Settings settings = AbstractBlock.Settings.create()
                .mapColor(mapColor)
                .sounds(sound)
                .strength(teBlock.getHardness(), teBlock.getExplosionResistance());
        if (teBlock.requiresTool()) {
            settings.requiresTool();
        }
        if (teBlock == TeBlock.luminator_flat) {
            // a lit luminator is a light source of 15, as getLightValue gave it in 1.12.2
            settings.luminance(state -> state.contains(BlockTileEntity.ACTIVE) && state.get(BlockTileEntity.ACTIVE) ? 15 : 0).nonOpaque();
        }
        if (teBlock == TeBlock.personal_chest) {
            // an open case with the door drawn by its renderer: the blocks around it stay visible
            settings.nonOpaque();
        }
        Block block = new BlockTileEntity(settings, teBlock);
        Identifier id = Identifier.of("ic2", teBlock.getName());
        Registry.register(Registries.BLOCK, id, block);
        Registry.register(Registries.ITEM, id, new BlockItem(block, new Item.Settings().rarity(teBlock.getRarity())));
        BLOCKS.put(teBlock, block);
        return block;
    }

    /**
     * The crop stick. Unlike the other tile entity blocks it has no item: 1.12.2 gave it none, the
     * crop stick item places it, and breaking it gives the stick back. It breaks by hand, takes no
     * collision and lets light through.
     */
    private static Block registerCropBlock() {
        Block block = new ic2.core.crop.BlockCrop(FabricBlockSettings.create()
                .mapColor(MapColor.DARK_GREEN)
                .sounds(BlockSoundGroup.WOOD)
                .strength(0.8f, 0.2f)
                .noCollision()
                .nonOpaque()
                .pistonBehavior(net.minecraft.block.piston.PistonBehavior.DESTROY));
        Registry.register(Registries.BLOCK, Identifier.of("ic2", TeBlock.crop.getName()), block);
        BLOCKS.put(TeBlock.crop, block);
        return block;
    }

    /**
     * Registers one block per cable variant, named the way the cable items are
     * ({@code ic2:copper_cable_1}), so every cable keeps its own thickness and texture.
     *
     * <p>The 1.12.2 block carried the type and the insulation as metadata; since the flattening
     * that is a block of its own, exactly as it is for the items.
     */
    public static List<Block> registerCableVariants() {
        List<Block> ret = new ArrayList<>();

        for (CableType type : CableType.values()) {
            for (int insulation = 0; insulation <= type.maxInsulation; insulation++) {
                Identifier id = Identifier.of("ic2", type.getName(insulation, null));
                ret.add(cableBlock(TeBlock.cable, id, type, insulation));
            }
        }

        CABLE_VARIANTS.addAll(ret);
        // TeBlock.cable is what the rest of the port looks the cable up by; the bare copper
        // variant answers for it
        BLOCKS.put(TeBlock.cable, ret.get(0));
        return ret;
    }

    /** The cable variant blocks, in registration order; the cable block entity backs all of them. */
    public static final List<Block> CABLE_VARIANTS = new ArrayList<>();

    private static Block cableBlock(TeBlock teBlock, Identifier id, CableType type, int insulation) {
        FabricBlockSettings settings = FabricBlockSettings.create()
                .mapColor(MapColor.STONE_GRAY)
                .sounds(BlockSoundGroup.STONE)
                .strength(teBlock.getHardness(), teBlock.getExplosionResistance())
                // a cable is a thin thing in the middle of its block: it must not shade the world
                // around it or hide the blocks behind it
                .nonOpaque();
        Block block = type.isColorable(insulation)
                ? new ic2.core.block.wiring.BlockColoredCable(settings, teBlock, type, insulation)
                : type == CableType.detector || type == CableType.splitter
                ? new ic2.core.block.wiring.BlockSignalCable(settings, teBlock, type, insulation)
                : new ic2.core.block.wiring.BlockCable(settings, teBlock, type, insulation);
        Registry.register(Registries.BLOCK, id, block);
        Registry.register(Registries.ITEM, id, new BlockItem(block, new Item.Settings()));
        return block;
    }

    /** The fluid pipe blocks, bronze then steel, tiny to large; the fluid pipe block entity backs all of them. */
    public static final List<Block> PIPE_VARIANTS = new ArrayList<>();

    /**
     * Registers one block per pipe type and size, named as the 1.12.2 item variants were
     * ({@code ic2:bronze_pipe_small}). The 1.12.2 pipe was one block whose tile entity carried
     * type and size; since the flattening each is a block of its own, as the cables are.
     */
    private static void registerPipeVariants() {
        for (ic2.core.block.transport.items.PipeType type : ic2.core.block.transport.items.PipeType.values()) {
            for (ic2.core.block.transport.items.PipeSize size : ic2.core.block.transport.items.PipeSize.values()) {
                Identifier id = Identifier.of("ic2", type.getName(size));
                AbstractBlock.Settings settings = AbstractBlock.Settings.create()
                        .mapColor(MapColor.CLEAR)
                        .sounds(BlockSoundGroup.METAL)
                        .strength(TeBlock.fluid_pipe.getHardness(), TeBlock.fluid_pipe.getExplosionResistance())
                        .requiresTool()
                        .nonOpaque();
                Block block = new ic2.core.block.transport.BlockPipe(settings, type, size);
                Registry.register(Registries.BLOCK, id, block);
                Registry.register(Registries.ITEM, id, new BlockItem(block, new Item.Settings()));
                PIPE_VARIANTS.add(block);
                if (type == ic2.core.block.transport.items.PipeType.bronze && size == ic2.core.block.transport.items.PipeSize.small) {
                    BLOCKS.put(TeBlock.fluid_pipe, block);
                }
            }
        }
    }

    private static <T extends BlockEntityType<?>> T registerBE(String name, T type) {
        Identifier id = Identifier.of("ic2", name);
        return Registry.register(Registries.BLOCK_ENTITY_TYPE, id, type);
    }

    public static Block getBlock(TeBlock teBlock) {
        return BLOCKS.get(teBlock);
    }

    @SuppressWarnings("unchecked")
    public static <T extends TileEntityBlock> BlockEntityType<T> getBeType(TeBlock teBlock) {
        return (BlockEntityType<T>) BE_TYPES.get(teBlock);
    }
}
