package ic2.core.item.tool;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ic2.api.item.ElectricItem;
import ic2.api.item.IBoxable;
import ic2.core.init.OreValues;
import ic2.core.item.ItemElectric;
import ic2.core.item.handheld.ScannerScreenHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Fabric port of {@code ic2.core.item.tool.ItemScanner}: the OD scanner.
 *
 * <p>Using it spends EU, sweeps the cube around the player for ores and opens a screen listing
 * what it found, most plentiful first.
 *
 * <p>1.12.2 decided what counted by looking the block's drops up in an ore-dictionary table; the
 * port asks {@link OreValues} instead, which is the same question in 1.21.1 terms (a block tag).
 */
public class ItemScanner extends ItemElectric implements IBoxable {

    private final double energyPerScan;
    private final int scanRange;

    public ItemScanner() {
        // "od_scanner", not "scanner": the UU scanner machine block already owns ic2:scanner.
        this("od_scanner", 1, 100000.0, 128.0, 50.0, 6);
    }

    protected ItemScanner(String name, int tier, double maxCharge, double transferLimit,
                          double energyPerScan, int scanRange) {
        super(name, tier, maxCharge, transferLimit);
        this.energyPerScan = energyPerScan;
        this.scanRange = scanRange;
    }

    /** The advanced scanner: same tool, more charge and twice the reach. */
    public static ItemScanner advanced() {
        return new ItemScanner("advanced_scanner", 2, 1000000.0, 512.0, 250.0, 12);
    }

    public int getScanRange() {
        return scanRange;
    }

    /**
     * A miner starting on a new layer: costs one scan's energy and gives the radius it searches,
     * half the scanner's range; 0 when the scanner is flat.
     */
    public int startLayerScan(ItemStack stack) {
        return ElectricItem.manager != null && ElectricItem.manager.use(stack, energyPerScan, null) ? scanRange / 2 : 0;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        tooltip.add(Text.translatable("scanner.range", scanRange));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);

        if (!ElectricItem.manager.use(stack, energyPerScan, player)) {
            return TypedActionResult.fail(stack);
        }

        if (!world.isClient) {
            ic2.core.init.IC2Sounds.play(world, null, player.getX(), player.getY(), player.getZ(), ic2.core.init.IC2Sounds.TOOLS_OD_SCANNER);
            List<ScannerScreenHandler.ScanEntry> results = scan(world, player.getBlockPos(), scanRange);
            player.openHandledScreen(new ScannerFactory(stack, results));
        }

        return TypedActionResult.success(stack, world.isClient);
    }

    @Override
    public boolean canBeStoredInToolbox(ItemStack stack) {
        return true;
    }

    /** A tool is not a battery: it never powers anything else, as in 1.12.2. */
    @Override
    public boolean canProvideEnergy(ItemStack stack) {
        return false;
    }

    /**
     * Every ore in the cube of side {@code 2 * range + 1} around {@code center}, counted by kind
     * and sorted with the most plentiful first.
     */
    public static List<ScannerScreenHandler.ScanEntry> scan(World world, BlockPos center, int range) {
        Map<Item, Integer> found = new LinkedHashMap<>();
        BlockPos.Mutable cursor = new BlockPos.Mutable();

        for (int y = center.getY() - range; y <= center.getY() + range; y++) {
            if (y < world.getBottomY() || y >= world.getTopY()) continue;

            for (int z = center.getZ() - range; z <= center.getZ() + range; z++) {
                for (int x = center.getX() - range; x <= center.getX() + range; x++) {
                    cursor.set(x, y, z);
                    if (!world.isChunkLoaded(cursor.getX() >> 4, cursor.getZ() >> 4)) continue;

                    BlockState state = world.getBlockState(cursor);
                    if (state.isAir() || !OreValues.isValuable(state)) continue;

                    Item item = itemFor(state);
                    if (item == null) continue;
                    found.merge(item, 1, Integer::sum);
                }
            }
        }

        List<ScannerScreenHandler.ScanEntry> results = new ArrayList<>(found.size());
        found.forEach((item, count) -> results.add(new ScannerScreenHandler.ScanEntry(new ItemStack(item), count)));
        results.sort(Comparator.comparingInt(ScannerScreenHandler.ScanEntry::count).reversed());

        if (results.size() > ScannerScreenHandler.MAX_RESULTS) {
            return new ArrayList<>(results.subList(0, ScannerScreenHandler.MAX_RESULTS));
        }
        return results;
    }

    /** What an ore block looks like in a list; null for a block with no item form. */
    private static Item itemFor(BlockState state) {
        Block block = state.getBlock();
        Item item = block.asItem();
        return item == net.minecraft.item.Items.AIR ? null : item;
    }

    /** Opens a screen holding the result list; nothing more arrives after it. */
    private record ScannerFactory(ItemStack stack, List<ScannerScreenHandler.ScanEntry> results)
            implements ExtendedScreenHandlerFactory<ScannerScreenHandler.OpenData> {

        @Override
        public Text getDisplayName() {
            return stack.getName();
        }

        @Override
        public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
            return new ScannerScreenHandler(syncId, playerInventory, results);
        }

        @Override
        public ScannerScreenHandler.OpenData getScreenOpeningData(ServerPlayerEntity player) {
            return new ScannerScreenHandler.OpenData(results);
        }
    }
}
