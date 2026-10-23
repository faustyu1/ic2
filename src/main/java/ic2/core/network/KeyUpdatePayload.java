package ic2.core.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * The client's key state, sent whenever it changes.
 *
 * <p>In 1.12.2 this was {@code initiateKeyUpdate} over IC2's own {@code FMLEventChannel}; on
 * Fabric it is a C2S custom payload holding the same packed int that
 * {@link ic2.core.util.Keyboard.Key#toInt} produces.
 */
public record KeyUpdatePayload(int keyState) implements CustomPayload {
    public static final CustomPayload.Id<KeyUpdatePayload> ID =
            new CustomPayload.Id<>(Identifier.of("ic2", "key_update"));

    public static final PacketCodec<RegistryByteBuf, KeyUpdatePayload> CODEC =
            PacketCodecs.VAR_INT.xmap(KeyUpdatePayload::new, KeyUpdatePayload::keyState).cast();

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
