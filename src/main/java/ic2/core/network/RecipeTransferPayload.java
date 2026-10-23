package ic2.core.network;

import ic2.core.gui.DynamicContainer;
import ic2.core.gui.IRecipeTransferTarget;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * A recipe viewer's "+" button in an IC2 window: the recipe as the items each grid cell may take.
 *
 * <p>The server moves the items itself, from what the player and the block hold, so the client
 * cannot conjure anything: it only says what it would like where, as a player could by clicking.
 *
 * @param syncId the window the recipe goes into
 */
public record RecipeTransferPayload(int syncId, List<List<ItemStack>> cells, boolean max) implements CustomPayload {
    public static final int CELLS = 9;
    /** Enough for any tag in a recipe, small enough that a forged packet costs nothing. */
    public static final int MAX_ALTERNATIVES = 256;

    public static final CustomPayload.Id<RecipeTransferPayload> ID =
            new CustomPayload.Id<>(Identifier.of("ic2", "recipe_transfer"));

    public static final PacketCodec<RegistryByteBuf, RecipeTransferPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, RecipeTransferPayload::syncId,
            ItemStack.LIST_PACKET_CODEC.collect(PacketCodecs.toList(CELLS)), RecipeTransferPayload::cells,
            PacketCodecs.BOOL, RecipeTransferPayload::max,
            RecipeTransferPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static void register() {
        try {
            PayloadTypeRegistry.playC2S().register(ID, CODEC);
            ServerPlayNetworking.registerGlobalReceiver(ID,
                    (payload, context) -> context.server().execute(() -> receive(context.player(), payload)));
        } catch (Throwable t) {
            // unit tests run without a Fabric loader context
        }
    }

    /** Server side of the packet: only into the window the player has open, and only an IC2 one that takes recipes. */
    public static void receive(ServerPlayerEntity player, RecipeTransferPayload payload) {
        if (!(player.currentScreenHandler instanceof DynamicContainer container)
                || container.syncId != payload.syncId()
                || !container.canUse(player)
                || !(container.getTile() instanceof IRecipeTransferTarget target)) {
            return;
        }

        List<List<ItemStack>> cells = new ArrayList<>(CELLS);
        for (int i = 0; i < CELLS; i++) {
            List<ItemStack> cell = i < payload.cells().size() ? payload.cells().get(i) : List.of();
            cells.add(cell.size() > MAX_ALTERNATIVES ? cell.subList(0, MAX_ALTERNATIVES) : cell);
        }
        if (target.transferRecipe(player, cells, payload.max())) {
            container.sendContentUpdates();
        }
    }
}
