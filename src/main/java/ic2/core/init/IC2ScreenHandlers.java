package ic2.core.init;

import ic2.core.gui.DynamicContainer;
import ic2.core.item.handheld.CropnalyzerScreenHandler;
import ic2.core.item.handheld.HandHeldScreenHandler;
import ic2.core.item.handheld.MeterScreenHandler;
import ic2.core.item.handheld.ScannerScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * Registration of the screen handler every guidef-driven machine shares.
 *
 * <p>One type is enough because the layout comes from the machine's guidef file rather than from
 * the handler class: the client receives the block position with the open packet, looks the block
 * entity up and parses the same definition the server used.
 */
public final class IC2ScreenHandlers {
    public static ScreenHandlerType<DynamicContainer> DYNAMIC;

    /** The one type every hand-held IC2 container uses; the kind travels with the open packet. */
    public static ScreenHandlerType<HandHeldScreenHandler> HAND_HELD;

    /** The EU reader, whose readings travel as screen handler properties. */
    public static ScreenHandlerType<MeterScreenHandler> METER;

    /** The OD scanner; its results travel with the open packet, because the scan is already done. */
    public static ScreenHandlerType<ScannerScreenHandler> SCANNER;

    /** The cropnalyzer: three slots inside the held stack, and nothing else to sync. */
    public static ScreenHandlerType<CropnalyzerScreenHandler> CROPNALYZER;

    private IC2ScreenHandlers() {}

    public static void registerAll() {
        DYNAMIC = Registry.register(Registries.SCREEN_HANDLER, Identifier.of("ic2", "dynamic"),
                new ExtendedScreenHandlerType<>(DynamicContainer::new, BlockPos.PACKET_CODEC));

        HAND_HELD = Registry.register(Registries.SCREEN_HANDLER, Identifier.of("ic2", "hand_held"),
                new ExtendedScreenHandlerType<>(HandHeldScreenHandler::new, HandHeldScreenHandler.OpenData.CODEC));

        METER = Registry.register(Registries.SCREEN_HANDLER, Identifier.of("ic2", "meter"),
                new ScreenHandlerType<>(MeterScreenHandler::new, net.minecraft.resource.featuretoggle.FeatureSet.empty()));

        SCANNER = Registry.register(Registries.SCREEN_HANDLER, Identifier.of("ic2", "scanner"),
                new ExtendedScreenHandlerType<>(ScannerScreenHandler::new, ScannerScreenHandler.OpenData.CODEC));

        CROPNALYZER = Registry.register(Registries.SCREEN_HANDLER, Identifier.of("ic2", "cropnalyzer"),
                new ScreenHandlerType<>(CropnalyzerScreenHandler::new, net.minecraft.resource.featuretoggle.FeatureSet.empty()));
    }
}
