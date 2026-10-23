package ic2.core.network;

import ic2.api.network.INetworkManager;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric 1.21.1 stub for IC2 NetworkManager.
 * Original Forge (1.12) version used FMLEventChannel + FMLProxyPacket (855 lines).
 * This stub keeps the singleton + INetworkManager contract so that
 * TileEntityBlock / NetworkHelper can call it without NPE, but does NOT
 * register any Fabric networking channel yet (PayloadTypeRegistry / S2C / C2S
 * will be wired in a later step).
 */
public final class NetworkManager implements INetworkManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("IC2/Network");
    private static NetworkManager INSTANCE;

    private NetworkManager() {
    }

    public static synchronized NetworkManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new NetworkManager();
        }
        return INSTANCE;
    }

    /**
     * Called from {@link ic2.core.IC2Mod#onInitialize()}.
     * Stub init — only logs, no Fabric networking registration yet.
     */
    public static void init() {
        NetworkManager mgr = getInstance();
        registerKeyUpdateChannel();
        LOGGER.info("[IC2 Network] NetworkManager initialized (key updates live, tile entity sync still a stub)");
        // Wire into api helper so NetworkHelper.getNetworkManager() doesn't return null.
        // Guarded to not crash outside Fabric loader context (unit tests).
        try {
            ic2.api.network.NetworkHelper.setInstance(mgr, mgr);
            LOGGER.debug("[IC2 Network] Registered as NetworkHelper server+client instance");
        } catch (Throwable t) {
            LOGGER.debug("[IC2 Network] NetworkHelper.setInstance skipped: {}", t.toString());
        }
    }

    /**
     * The one channel that is real so far: the client's key state, which the IC2 equipment on the
     * server has to be able to ask about ({@link ic2.core.util.Keyboard}).
     */
    private static void registerKeyUpdateChannel() {
        try {
            net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playC2S()
                    .register(KeyUpdatePayload.ID, KeyUpdatePayload.CODEC);

            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
                    KeyUpdatePayload.ID,
                    (payload, context) -> context.server().execute(
                            () -> ic2.core.IC2.keyboard.processKeyUpdate(context.player(), payload.keyState())));

            net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register(
                    (handler, server) -> ic2.core.IC2.keyboard.removePlayerReferences(handler.getPlayer()));

            LOGGER.debug("[IC2 Network] Registered C2S channel {}", KeyUpdatePayload.ID.id());
        } catch (Throwable t) {
            // unit tests run without a Fabric loader context
            LOGGER.debug("[IC2 Network] key update channel not registered: {}", t.toString());
        }
    }

    // ------------------------------------------------------------------
    // INetworkManager — all methods are stubs with logging
    // ------------------------------------------------------------------

    @Override
    public void updateTileEntityField(BlockEntity tileEntity, String field) {
        LOGGER.debug("[IC2 Network] updateTileEntityField stub: te={}, field={}, world={}",
                tileEntity, field, tileEntity != null ? tileEntity.getWorld() : "null");
        // Fabric S2C CustomPayload will be sent here later:
        // ServerPlayNetworking.send(player, new PacketUpdateTileEntityField(...))
    }

    @Override
    public void initiateTileEntityEvent(BlockEntity tileEntity, int event, boolean limitRange) {
        LOGGER.debug("[IC2 Network] initiateTileEntityEvent stub: te={}, event={}, limitRange={}",
                tileEntity, event, limitRange);
    }

    @Override
    public void initiateItemEvent(PlayerEntity player, ItemStack stack, int event, boolean limitRange) {
        LOGGER.debug("[IC2 Network] initiateItemEvent stub: player={}, event={}, limitRange={}",
                player, event, limitRange);
    }

    @Override
    public void initiateClientTileEntityEvent(BlockEntity tileEntity, int event) {
        LOGGER.debug("[IC2 Network] initiateClientTileEntityEvent stub: te={}, event={}", tileEntity, event);
    }

    @Override
    public void initiateClientItemEvent(ItemStack stack, int event) {
        LOGGER.debug("[IC2 Network] initiateClientItemEvent stub: event={}", event);
    }

    @Override
    public void sendInitialData(BlockEntity tileEntity) {
        LOGGER.debug("[IC2 Network] sendInitialData stub: te={}", tileEntity);
    }

    // ------------------------------------------------------------------
    // Additional helpers present in 1.12 NetworkManager — stubs
    // ------------------------------------------------------------------

    /**
     * Generic sendPacket stub. Fabric networking (CustomPayload + PacketByteBuf)
     * will be implemented later; for now just logs.
     */
    public void sendPacket(net.minecraft.network.PacketByteBuf buf) {
        LOGGER.debug("[IC2 Network] sendPacket(PacketByteBuf) stub: readableBytes={}",
                buf != null ? buf.readableBytes() : -1);
    }

    public void sendPacket(BlockEntity tileEntity, String field, Object value) {
        LOGGER.debug("[IC2 Network] sendPacket stub: te={}, field={}, value={}", tileEntity, field, value);
    }

    public void updateTileEntityField(BlockEntity tileEntity, String field, Object value) {
        LOGGER.debug("[IC2 Network] updateTileEntityField(te,field,value) stub: te={}, field={}, value={}",
                tileEntity, field, value);
        updateTileEntityField(tileEntity, field);
    }
}
