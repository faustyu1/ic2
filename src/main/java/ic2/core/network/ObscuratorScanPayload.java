package ic2.core.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * What the obscurator scanned: the block state (by raw id), the face and the colour multipliers
 * the face's quads had there.
 *
 * <p>Only the client knows the block's model and colours, so it scans and the server stores the
 * result in the item, as {@code sendPlayerItemData} did in 1.12.2.
 *
 * @param slot the hotbar slot of the obscurator
 */
public record ObscuratorScanPayload(int slot, int stateId, int side, int[] colorMultipliers) implements CustomPayload {
    public static final CustomPayload.Id<ObscuratorScanPayload> ID =
            new CustomPayload.Id<>(Identifier.of("ic2", "obscurator_scan"));

    public static final PacketCodec<RegistryByteBuf, ObscuratorScanPayload> CODEC = PacketCodec.of(
            (payload, buf) -> {
                buf.writeVarInt(payload.slot);
                buf.writeVarInt(payload.stateId);
                buf.writeByte(payload.side);
                buf.writeIntArray(payload.colorMultipliers);
            },
            buf -> new ObscuratorScanPayload(buf.readVarInt(), buf.readVarInt(), buf.readByte(), buf.readIntArray(64)));

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
