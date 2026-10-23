package ic2.core.network;

import ic2.core.block.TileEntityBlock;
import net.minecraft.network.PacketByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stub for TileEntity field sync packet.
 * Original used GrowingBuffer + DataEncoder + SubPacketType.TileEntityData.
 * Fabric 1.21.1 will use CustomPayload + PacketByteBuf codec.
 * For now payload is not serialized — encode/decode are no-ops.
 */
public final class PacketUpdateTileEntityField {

    private static final Logger LOGGER = LoggerFactory.getLogger("IC2/Network");

    public final TileEntityBlock tile;
    public final String field;
    public final Object value;

    public PacketUpdateTileEntityField(TileEntityBlock tile, String field, Object value) {
        this.tile = tile;
        this.field = field;
        this.value = value;
    }

    /**
     * Stub encode — logs and writes nothing (or minimal placeholder).
     * Signature kept compatible with future Payload codec that takes PacketByteBuf.
     */
    public void encode(PacketByteBuf buf) {
        LOGGER.debug("[IC2 Network] PacketUpdateTileEntityField.encode stub: tile={}, field={}, value={}",
                tile, field, value);
        // Future: buf.writeString(field); DataEncoder.encode(buf, value); etc.
    }

    /**
     * Stub decode — returns a dummy packet (null tile/value).
     */
    public static PacketUpdateTileEntityField decode(PacketByteBuf buf) {
        LOGGER.debug("[IC2 Network] PacketUpdateTileEntityField.decode stub: readableBytes={}",
                buf != null ? buf.readableBytes() : -1);
        return new PacketUpdateTileEntityField(null, "", null);
    }

    /** Convenience factory used by NetworkManager when wiring real networking. */
    public static PacketUpdateTileEntityField of(TileEntityBlock tile, String field, Object value) {
        return new PacketUpdateTileEntityField(tile, field, value);
    }
}
