package ic2.core.gametest;

import ic2.core.block.machine.tileentity.TileEntityScanner;
import ic2.core.init.IC2Blocks;
import ic2.core.init.IC2Items;
import ic2.core.uu.UuGraph;
import ic2.core.uu.UuIndex;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * UU values derived through the server's recipes for items the config gives no value. Run with
 * {@code ./gradlew runGametest}.
 */
public class UuGraphGameTest implements FabricGameTest {
    private static final BlockPos POS = new BlockPos(2, 2, 2);
    private static final double DELTA = 1.0E-9;

    /**
     * A furnace is eight cobblestone (1.0 each) crafted for 1 UU: 9.0. Stone is cobblestone smelted
     * for 14: 15.0, and smooth stone is that smelted again: 29.0.
     */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void craftedAndSmeltedItemsGetValues(TestContext ctx) {
        ctx.assertTrue(UuIndex.getInstance().getValue(Items.FURNACE) == null, "the furnace has a configured value");
        ctx.assertTrue(UuIndex.getInstance().getValue(Items.STONE) == null, "stone has a configured value");
        ctx.assertTrue(UuIndex.getInstance().getValue(Items.COBBLESTONE) == 1.0, "cobblestone config");

        double cobblestone = UuGraph.get(new ItemStack(Items.COBBLESTONE));
        double furnace = UuGraph.get(new ItemStack(Items.FURNACE));
        double stone = UuGraph.get(new ItemStack(Items.STONE));
        double smoothStone = UuGraph.get(new ItemStack(Items.SMOOTH_STONE));
        ctx.assertTrue(Math.abs(cobblestone - 1.0) < DELTA, "cobblestone " + cobblestone);
        ctx.assertTrue(Math.abs(furnace - 9.0) < DELTA, "furnace " + furnace);
        ctx.assertTrue(Math.abs(stone - 15.0) < DELTA, "stone " + stone);
        ctx.assertTrue(Math.abs(smoothStone - 29.0) < DELTA, "smooth stone " + smoothStone);
        ctx.assertTrue(Math.abs(UuGraph.getInBuckets(new ItemStack(Items.FURNACE)) - 9.0E-5) < DELTA, "furnace in buckets");
        ctx.assertTrue(UuGraph.getValues().containsKey(Items.FURNACE), "furnace missing from the value list");
        ctx.assertTrue(UuGraph.get(new ItemStack(Items.BEDROCK)) == Double.POSITIVE_INFINITY, "bedrock got a value");
        ctx.complete();
    }

    /**
     * Scrap comes out of the recycler: eight of the cheapest recyclable item plus the recycler's
     * energy (55 * 8 / 4000 * 107 UU), so at most eight cobblestone's worth on top of that.
     */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void scrapIsWorthWhatTheRecyclerEats(TestContext ctx) {
        double recycling = 55.0 * 8 / 4000.0 * 107.0;
        double scrap = UuGraph.get(new ItemStack(IC2Items.SCRAP));
        ctx.assertTrue(scrap > recycling && scrap <= recycling + 8.0 + DELTA, "scrap " + scrap);
        ctx.complete();
    }

    /** A furnace now scans like any configured item and records 9.0 UU (90 microbuckets). */
    @GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 400)
    public void craftedItemIsScannable(TestContext ctx) {
        ctx.setBlockState(POS, IC2Blocks.SCANNER_BLOCK.getDefaultState());
        ctx.setBlockState(POS.east(), IC2Blocks.PATTERN_STORAGE_BLOCK.getDefaultState());
        TileEntityScanner scanner = ctx.getBlockEntity(POS);

        ItemStack furnace = new ItemStack(Items.FURNACE);
        ctx.assertTrue(scanner.inputSlot.accepts(furnace), "the scanner refuses a furnace");
        ctx.assertTrue(UuGraph.find(furnace).isOf(Items.FURNACE), "pattern " + UuGraph.find(furnace));

        scanner.inputSlot.put(furnace);
        for (int i = 0; i < TileEntityScanner.DURATION; i++) {
            scanner.injectEnergy(Direction.UP, 256, 2048);
            scanner.tick();
        }
        ctx.assertTrue(scanner.getState() == TileEntityScanner.State.COMPLETED, "state " + scanner.getState());
        ctx.assertTrue(Math.abs(scanner.patternUu - 9.0E-5) < DELTA, "pattern uu " + scanner.patternUu);
        ctx.complete();
    }
}
