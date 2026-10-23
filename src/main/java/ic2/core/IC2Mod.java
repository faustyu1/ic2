package ic2.core;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric entrypoint for the common (server + client) side of IndustrialCraft 2 Community Edition.
 *
 * The original Forge {@code @Mod} logic from {@code ic2.core.IC2} will be
 * migrated into this class as the port progresses (registration, config,
 * energy net bootstrap, etc.).
 */
public final class IC2Mod implements ModInitializer {
    public static final String MOD_ID = "ic2";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[IC2 Community Edition] Initializing common side");
        IC2Potion.init();
        // the EU manager every electric item goes through; it was never installed, so anything
        // that asked ElectricItem.manager for a charge got a null and crashed
        ic2.api.item.ElectricItem.rawManager = ic2.core.item.ElectricItemManager.INSTANCE;
        ic2.api.item.ElectricItem.manager = new ic2.core.item.GatewayElectricItemManager();
        // Network stub — no Fabric networking yet, just singleton + logger
        ic2.core.network.NetworkManager.init();
        // Register TeBlocks
        ic2.core.block.TeBlockRegistry.buildBlocks();
        // Fluids first: the blocks and the cell items look their instances up by name
        ic2.core.ref.FluidName.registerAll();
        // Register blocks and block entities for Fabric
        ic2.core.init.IC2Blocks.registerAll();
        ic2.core.init.IC2ClassicBlocks.registerAll();
        ic2.core.block.comp.Fluids.register();
        ic2.core.block.wiring.TileEntityCable.registerFoamBreaking();
        ic2.core.item.tool.ItemObscurator.registerNetworking();
        ic2.core.network.RecipeTransferPayload.register();
        ic2.core.recipe.OutputUnifier.register();
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTING.register(server -> IC2.server = server);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> IC2.server = null);
        // Bootstrap TickHandler — registers ServerTickEvents.END_SERVER_TICK
        ic2.core.TickHandler.init();
        IC2.tickHandler = ic2.core.TickHandler.getInstance();
        // Bootstrap EnergyNet
        ic2.api.energy.EnergyNet.instance = ic2.core.energy.grid.EnergyNetGlobal.create();
        // Init recipe managers
        ic2.core.block.machine.tileentity.TileEntityMacerator.init();
        ic2.core.block.machine.tileentity.TileEntityCompressor.init();
        ic2.core.block.machine.tileentity.TileEntityExtractor.init();
        ic2.core.block.machine.tileentity.TileEntityElectricFurnace.init();
        ic2.core.block.machine.tileentity.TileEntityRecycler.init();
        ic2.core.block.machine.tileentity.TileEntityCentrifuge.init();
        ic2.core.block.machine.tileentity.TileEntityBlastFurnace.init();
        ic2.core.block.machine.tileentity.TileEntityMetalFormer.init();
        ic2.core.block.machine.tileentity.TileEntityOreWashing.init();
        ic2.core.block.machine.tileentity.TileEntityBlockCutter.init();
        // Register items - flattened metadata variants first, the singletons bind to them
        ic2.core.init.IC2ItemVariants.registerAll();
        ic2.core.init.IC2Items.registerAll();
        ic2.core.item.ItemFluidStorages.register();
        ic2.core.init.IC2Entities.registerAll();
        // the canner's recipes name items, so they wait for the items
        ic2.core.block.machine.tileentity.TileEntityCanner.init();
        ic2.core.init.IC2Sounds.registerAll();
        ic2.core.init.IC2CreativeTabs.register();
        ic2.core.init.IC2ScreenHandlers.registerAll();
        ic2.core.init.IC2Recipes.init();
        ic2.core.uu.UuGraph.init();
        ic2.core.item.armor.jetpack.JetpackAttachmentRecipe.init();
        ic2.core.item.armor.jetpack.JetpackHandler.init();
        // World gen (ores, rubber tree) - Fabric BiomeModifications stub
        ic2.core.init.IC2WorldGen.register();
        // Crops: the registry, the biome bonuses, every crop card and the base seeds
        ic2.core.crop.IC2Crops.init();
        ic2.core.init.Ic2Loot.init();
        ic2.core.item.tool.ItemToolPainter.registerEvents();
        // bedrock stops an IC2 blast instead of letting it through, as in 1.12.2
        ic2.api.tile.ExplosionWhitelist.addWhitelistedBlock(net.minecraft.block.Blocks.BEDROCK);
        LOGGER.info("[IC2 Community Edition] Registered {} blocks", ic2.core.init.IC2Blocks.class.getSimpleName());
        // GUI stub registered: ic2.core.gui.GuiIC2 (HandledScreen), DynamicContainer (ScreenHandler), GuiParser (guidef stub)
        // ScreenHandlerType intentionally not registered (null/dummy) for stub phase — no networking/handling yet.
    }
}
