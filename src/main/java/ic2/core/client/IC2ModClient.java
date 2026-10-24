package ic2.core.client;

import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;
import ic2.core.gui.GuiIC2;
import ic2.core.init.IC2ClassicBlocks;
import ic2.core.init.IC2ScreenHandlers;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.object.builder.v1.client.model.FabricModelPredicateProviderRegistry;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.FoliageColors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric client-side entrypoint.
 *
 * Registers the guidef-driven machine screen and the IC2 key bindings; the rest of the
 * client-only logic from the original {@code ic2.core.PlatformClient} (rendering, overlays) still
 * has to follow.
 */
public final class IC2ModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Logger logger = LoggerFactory.getLogger("ic2");
        logger.info("[IC2 Community Edition] Initializing client side");

        // one screen for every machine: the layout comes from the guidef file, not from the class
        HandledScreens.register(IC2ScreenHandlers.DYNAMIC, GuiIC2::new);
        HandledScreens.register(IC2ScreenHandlers.HAND_HELD, ic2.core.client.gui.HandHeldScreen::new);
        HandledScreens.register(IC2ScreenHandlers.METER, ic2.core.client.gui.MeterScreen::new);
        HandledScreens.register(IC2ScreenHandlers.SCANNER, ic2.core.client.gui.ScannerScreen::new);
        HandledScreens.register(IC2ScreenHandlers.CROPNALYZER, ic2.core.client.gui.CropnalyzerScreen::new);

        registerKeyboard();
        GuiOverlayer.register();
        registerChargePredicate();
        registerRenderLayers();
        registerFluidRendering();
        registerBlockColors();
        registerPipeColors();
        registerFluidCellColors();
        hideTrinketsJetpackTooltip();
        registerModels();
        registerEntityRenderers();
        net.minecraft.client.render.block.entity.BlockEntityRendererFactories.register(ic2.core.init.IC2Blocks.WIND_KINETIC_BE,
                ic2.core.client.render.KineticRotorRenderer::new);
        net.minecraft.client.render.block.entity.BlockEntityRendererFactories.register(ic2.core.init.IC2Blocks.WATER_KINETIC_BE,
                ic2.core.client.render.KineticRotorRenderer::new);
        ic2.core.client.audio.MachineSoundHandler.register();
        ic2.core.client.audio.ToolSoundHandler.register();
        ic2.core.item.tool.ItemObscurator.clientScanner = ic2.core.client.model.ObscuredFaces::scan;
    }

    /** What PlatformClient gave the IC2 entities to draw them with. */
    private static void registerEntityRenderers() {
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(ic2.core.init.IC2Entities.MINING_LASER,
                ic2.core.client.render.MiningLaserRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(ic2.core.init.IC2Entities.ITNT,
                ic2.core.client.render.ExplosiveEntityRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(ic2.core.init.IC2Entities.NUKE,
                ic2.core.client.render.ExplosiveEntityRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(ic2.core.init.IC2Entities.DYNAMITE,
                net.minecraft.client.render.entity.FlyingItemEntityRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(ic2.core.init.IC2Entities.STICKY_DYNAMITE,
                net.minecraft.client.render.entity.FlyingItemEntityRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(ic2.core.init.IC2Entities.CARBON_BOAT,
                ctx -> new ic2.core.client.render.IC2BoatRenderer(ctx, "boat_carbon"));
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(ic2.core.init.IC2Entities.RUBBER_BOAT,
                ctx -> new ic2.core.client.render.IC2BoatRenderer(ctx, "boat_rubber"));
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(ic2.core.init.IC2Entities.ELECTRIC_BOAT,
                ctx -> new ic2.core.client.render.IC2BoatRenderer(ctx, "boat_electric"));
    }

    /**
     * The crop stick draws whatever grows on it, which the JSON model cannot know; the baked stick
     * model is wrapped by one that reads the crop from the block entity.
     */
    private static void registerModels() {
        net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin.register(plugin ->
                plugin.modifyModelAfterBake().register((model, context) -> {
                    var id = context.topLevelId();
                    if (model != null && id != null && !"inventory".equals(id.variant())
                            && id.id().equals(Identifier.of("ic2", "crop"))) {
                        return new ic2.core.client.model.CropModel(model);
                    }
                    // foamed cables and the obscurable wall draw the wall's colour and copied faces
                    if (model != null && id != null && !"inventory".equals(id.variant()) && "ic2".equals(id.id().getNamespace())) {
                        net.minecraft.block.Block block = Registries.BLOCK.get(id.id());
                        if (block instanceof ic2.core.block.wiring.BlockCable || block == ic2.core.init.IC2Blocks.WALL_BLOCK) {
                            return new ic2.core.client.model.FoamWallModel(model);
                        }
                    }
                    return model;
                }));
    }

    /**
     * Takes over {@link ic2.core.IC2#keyboard} with the client implementation and ticks it, which
     * is what puts the IC2 key bindings in the controls screen and keeps the server informed.
     */
    private static void registerKeyboard() {
        KeyboardClient keyboard = new KeyboardClient();
        ic2.core.IC2.keyboard = keyboard;
        ClientTickEvents.END_CLIENT_TICK.register(client -> keyboard.sendKeyUpdate());
    }

    /**
     * The blocks whose textures have holes in them. Without this they render as opaque cubes -
     * a sapling comes out as a solid block with the sapling picture on every side.
     */
    private static void registerRenderLayers() {
        BlockRenderLayerMap.INSTANCE.putBlock(IC2ClassicBlocks.RUBBER_SAPLING, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(IC2ClassicBlocks.RUBBER_LEAVES, RenderLayer.getCutoutMipped());
        BlockRenderLayerMap.INSTANCE.putBlock(IC2ClassicBlocks.REINFORCED_GLASS, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(IC2ClassicBlocks.DYNAMITE, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(IC2ClassicBlocks.IRON_FENCE, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(IC2ClassicBlocks.REINFORCED_DOOR, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ic2.core.init.IC2Blocks.CROP_BLOCK, RenderLayer.getCutout());
    }

    /**
     * How the IC2 fluids look: their own still and flowing textures, tinted in their 1.12.2
     * colour, in the world and wherever Fabric's {@code FluidVariantRendering} draws them - the
     * machine windows and REI.
     */
    private static void registerFluidRendering() {
        for (ic2.core.ref.FluidName name : ic2.core.ref.FluidName.values()) {
            FluidRenderHandlerRegistry.INSTANCE.register(name.getInstance(), name.getFlowing(),
                    new SimpleFluidRenderHandler(name.getTextureLocation(false), name.getTextureLocation(true),
                            0xFF000000 | name.getColor()));
            BlockRenderLayerMap.INSTANCE.putFluids(RenderLayer.getTranslucent(), name.getInstance(), name.getFlowing());
        }
    }

    /**
     * The rubber tree's leaves use the birch leaf texture, which vanilla only looks right with
     * the birch tint on top - untinted it comes out grey.
     */
    /**
     * The pipes share one grey texture set, tinted by their type (bronze or steel) as PipeModel
     * coloured its quads; the pump covers tint their foreground layer by their tier.
     */
    private static void registerPipeColors() {
        for (net.minecraft.block.Block block : ic2.core.init.IC2Blocks.PIPE_VARIANTS) {
            ic2.core.block.transport.items.PipeType type = ((ic2.core.block.transport.BlockPipe) block).getPipeType();
            // items draw the tint's alpha since 1.21, so it has to be opaque
            int color = 0xFF000000 | (type.red & 0xFF) << 16 | (type.green & 0xFF) << 8 | type.blue & 0xFF;
            ColorProviderRegistry.BLOCK.register((state, view, pos, tintIndex) -> color, block);
            ColorProviderRegistry.ITEM.register((stack, tintIndex) -> color, block);
        }
        for (Item item : Registries.ITEM) {
            if (item instanceof ic2.core.item.logistics.ItemPumpCover cover) {
                int color = 0xFF000000 | cover.getType().color;
                ColorProviderRegistry.ITEM.register((stack, tintIndex) -> tintIndex == 1 ? color : 0xFFFFFFFF, item);
            }
        }
    }

    /**
     * The fluid cells without a texture of their own show the fluid through the cell's window:
     * the window layer is tinted with the fluid's colour, as the 1.12.2 cell model drew the fluid
     * behind it.
     */
    private static void registerFluidCellColors() {
        java.util.Map<String, Integer> colors = java.util.Map.of(
                "water", 0xFF3F76E4,
                "lava", 0xFFE8641C,
                "ic2hot_water", 0xFF6FA8E8,
                "ic2weed_ex", 0xFF4E7A1E);
        colors.forEach((fluid, color) -> {
            Item cell = ic2.core.init.IC2ItemVariants.get("fluid_cell", fluid);
            if (cell != null) {
                ColorProviderRegistry.ITEM.register((stack, tintIndex) -> tintIndex == 1 ? color : 0xFFFFFFFF, cell);
            }
        });
    }

    /**
     * Trinkets lists the slots an item fits ("Используется как рюкзак") and an empty "When
     * equipped" heading; the packs that go on the back say so with a line of their own instead.
     */
    private static void hideTrinketsJetpackTooltip() {
        net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            if (stack.isIn(ic2.core.item.armor.jetpack.BackSlot.ITEMS)
                    && "ic2".equals(net.minecraft.registry.Registries.ITEM.getId(stack.getItem()).getNamespace())) {
                lines.removeIf(line -> line.getContent() instanceof net.minecraft.text.TranslatableTextContent content
                        && content.getKey().startsWith("trinkets.tooltip."));
                lines.add(net.minecraft.text.Text.translatable("ic2.tooltip.back_slot").formatted(net.minecraft.util.Formatting.BLUE));
            }
        });
    }

    private static void registerBlockColors() {
        if (IC2ClassicBlocks.RUBBER_LEAVES == null) {
            return;
        }

        ColorProviderRegistry.BLOCK.register(
                (state, view, pos, tintIndex) -> FoliageColors.getBirchColor(),
                IC2ClassicBlocks.RUBBER_LEAVES);
        ColorProviderRegistry.ITEM.register(
                (stack, tintIndex) -> FoliageColors.getBirchColor(),
                IC2ClassicBlocks.RUBBER_LEAVES);
    }

    /**
     * Makes the batteries show how full they are.
     *
     * <p>The 1.12.2 items picked one of five textures from their damage value; the flattened items
     * do it with model overrides on {@code ic2:charge}, the charge as a 0..1 ratio.
     */
    private static void registerChargePredicate() {
        Identifier charge = Identifier.of("ic2", "charge");

        for (Item item : Registries.ITEM) {
            if (!(item instanceof IElectricItem electric)) {
                continue;
            }

            FabricModelPredicateProviderRegistry.register(item, charge, (stack, world, entity, seed) -> {
                double max = electric.getMaxCharge(stack);
                if (max <= 0 || ElectricItem.manager == null) {
                    return 0.0f;
                }
                return (float) Math.min(1.0, ElectricItem.manager.getCharge(stack) / max);
            });
        }
    }
}
