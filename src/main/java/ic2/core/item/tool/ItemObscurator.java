package ic2.core.item.tool;

import ic2.api.item.ElectricItem;
import ic2.core.block.comp.Obscuration;
import ic2.core.item.ItemElectric;
import ic2.core.network.ObscuratorScanPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Port of ItemObscurator: copies the look of one block's face onto the faces of foam walls.
 *
 * <p>Sneak-use it on a block to scan the face that was clicked (20 000 EU); use it on a foam wall,
 * or on a cable inside hardened foam, to dress the clicked face up as the scanned one (5 000 EU).
 * Faces showing through - transparent blocks such as glass - and faces that do not cover the whole
 * side of the block cannot be scanned. Painting the wall wipes its obscured faces. It holds
 * 100 000 EU and charges at tier 2.
 *
 * <p>Scanning reads the block's model, which only the client has, so the client works out the
 * face and its colours and sends them to the server ({@link ObscuratorScanPayload}), which takes
 * the energy and stores the scan in the item.
 */
public class ItemObscurator extends ItemElectric {
    public static final double SCAN_COST = 20000.0;
    public static final double PRINT_COST = 5000.0;

    /** The scan the client runs; set by the client entrypoint, null on a dedicated server. */
    public static @Nullable ClientScanner clientScanner;

    public interface ClientScanner {
        /**
         * Scans the face and sends it to the server.
         *
         * @return false when the face cannot be scanned or is the one stored already
         */
        boolean scan(ItemStack stack, PlayerEntity player, World world, BlockPos pos, Direction side, int slot);
    }

    public ItemObscurator() {
        super("obscurator", 2, 100000, 250);
    }

    @Override
    public boolean canProvideEnergy(ItemStack stack) {
        return false;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        PlayerEntity player = context.getPlayer();
        if (player == null) {
            return ActionResult.PASS;
        }
        World world = context.getWorld();
        ItemStack stack = context.getStack();

        if (!player.isSneaking()) {
            if (world.isClient || !ElectricItem.manager.canUse(stack, PRINT_COST)) {
                return ActionResult.PASS;
            }
            Obscuration.ObscurationData data = getData(stack);
            if (data == null) {
                clear(stack);
                return ActionResult.PASS;
            }
            if (Obscuration.retexture(world, context.getBlockPos(), context.getSide(), data)) {
                ElectricItem.manager.use(stack, PRINT_COST, player);
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        }

        if (world.isClient && clientScanner != null && ElectricItem.manager.canUse(stack, SCAN_COST)) {
            int slot = context.getHand() == Hand.MAIN_HAND ? player.getInventory().selectedSlot : PlayerInventory.OFF_HAND_SLOT;
            return clientScanner.scan(stack, player, world, context.getBlockPos(), context.getSide(), slot)
                    ? ActionResult.SUCCESS : ActionResult.PASS;
        }
        return ActionResult.PASS;
    }

    /** The server side of a scan: takes the energy and stores what was scanned. */
    public static void receiveScan(PlayerEntity player, ObscuratorScanPayload payload) {
        if (payload.slot() < 0 || payload.slot() >= player.getInventory().size()
                || payload.side() < 0 || payload.side() >= Direction.values().length
                || payload.colorMultipliers().length == 0) {
            return;
        }
        ItemStack stack = player.getInventory().getStack(payload.slot());
        BlockState state = Block.getStateFromRawId(payload.stateId());
        if (!(stack.getItem() instanceof ItemObscurator) || state.isAir()) {
            return;
        }
        if (ElectricItem.manager.use(stack, SCAN_COST, player)) {
            setData(stack, new Obscuration.ObscurationData(state, Direction.byId(payload.side()), payload.colorMultipliers()));
        }
    }

    public static void registerNetworking() {
        try {
            PayloadTypeRegistry.playC2S().register(ObscuratorScanPayload.ID, ObscuratorScanPayload.CODEC);
            ServerPlayNetworking.registerGlobalReceiver(ObscuratorScanPayload.ID,
                    (payload, context) -> context.server().execute(() -> receiveScan(context.player(), payload)));
        } catch (Throwable t) {
            // unit tests run without a Fabric loader context
        }
    }

    // ---------------------------------------------------------------- the stored scan

    /** The scanned face, or null when the obscurator holds none. */
    public static Obscuration.@Nullable ObscurationData getData(ItemStack stack) {
        NbtCompound nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        if (!nbt.contains("refState")) {
            return null;
        }
        BlockState state = Obscuration.ObscurationData.readState(nbt.getCompound("refState"));
        int side = nbt.getByte("refSide");
        int[] colorMultipliers = nbt.getIntArray("refColorMuls");
        if (state == null || side < 0 || side >= Direction.values().length || colorMultipliers.length == 0) {
            return null;
        }
        return new Obscuration.ObscurationData(state, Direction.byId(side), colorMultipliers);
    }

    public static void setData(ItemStack stack, Obscuration.ObscurationData data) {
        NbtCompound nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        nbt.put("refState", NbtHelper.fromBlockState(data.state));
        nbt.putByte("refSide", (byte) data.side.ordinal());
        nbt.putIntArray("refColorMuls", data.colorMultipliers);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    private static void clear(ItemStack stack) {
        NbtCompound nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        nbt.remove("refState");
        nbt.remove("refSide");
        nbt.remove("refColorMuls");
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }
}
