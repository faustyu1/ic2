package ic2.core.item.handheld;

import java.util.List;

import ic2.core.init.IC2ScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

/**
 * Fabric port of {@code ic2.core.item.tool.ContainerToolScanner}: what the OD scanner found.
 *
 * <p>The scan happens once, when the scanner is used, so the results travel with the packet that
 * opens the screen instead of over a channel of their own - 1.12.2 needed
 * {@code sendContainerField} only because its container was built before the scan ran.
 */
public class ScannerScreenHandler extends ScreenHandler {

    /** One line of the result list: what was found and how much of it. */
    public record ScanEntry(ItemStack stack, int count) {
        public static final PacketCodec<RegistryByteBuf, ScanEntry> CODEC = PacketCodec.tuple(
                ItemStack.PACKET_CODEC, ScanEntry::stack,
                PacketCodecs.VAR_INT, ScanEntry::count,
                ScanEntry::new);
    }

    /** The screen shows ten lines, so there is no point sending more. */
    public static final int MAX_RESULTS = 10;

    public record OpenData(List<ScanEntry> results) {
        public static final PacketCodec<RegistryByteBuf, OpenData> CODEC =
                ScanEntry.CODEC.collect(PacketCodecs.toList(MAX_RESULTS))
                        .xmap(OpenData::new, OpenData::results);
    }

    private final PlayerEntity player;
    private final List<ScanEntry> results;

    public ScannerScreenHandler(int syncId, PlayerInventory playerInventory, OpenData data) {
        this(syncId, playerInventory, data.results());
    }

    public ScannerScreenHandler(int syncId, PlayerInventory playerInventory, List<ScanEntry> results) {
        super(IC2ScreenHandlers.SCANNER, syncId);
        this.player = playerInventory.player;
        this.results = List.copyOf(results);

        addPlayerSlots(playerInventory);
    }

    /** The 1.12.2 offsets for a screen 231 tall, which is what its background is drawn for. */
    private void addPlayerSlots(PlayerInventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 149 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 207));
        }
    }

    public List<ScanEntry> getResults() {
        return results;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return player == this.player;
    }

    /** Nothing to move: the scanner's screen holds no inventory of its own. */
    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        return ItemStack.EMPTY;
    }
}
