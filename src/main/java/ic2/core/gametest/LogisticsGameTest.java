package ic2.core.gametest;

import ic2.api.item.ICustomDamageItem;
import ic2.core.block.machine.tileentity.TileEntityAssemblyBench;
import ic2.core.block.machine.tileentity.TileEntityBatchCrafter;
import ic2.core.block.machine.tileentity.TileEntityBetterItemBuffer;
import ic2.core.block.machine.tileentity.TileEntityIndustrialWorkbench;
import ic2.core.block.machine.tileentity.TileEntityItemBuffer;
import ic2.core.block.machine.tileentity.TileEntitySortingMachine;
import ic2.core.block.machine.tileentity.TileEntityWeightedItemDistributor;
import ic2.core.gui.DynamicContainer;
import ic2.core.gui.dynamic.SlotCraftResult;
import ic2.core.gui.dynamic.SlotHologram;
import ic2.core.init.IC2Blocks;
import ic2.core.init.IC2ItemVariants;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/** The item logistics blocks: buffers, sorter, distributor and the crafters. Run with {@code ./gradlew runGametest}. */
public class LogisticsGameTest implements FabricGameTest {
    private static final BlockPos POS = new BlockPos(2, 2, 2);

    static int count(TestContext ctx, BlockPos pos, Item item) {
        ChestBlockEntity chest = ctx.getBlockEntity(pos);
        int ret = 0;
        for (int i = 0; i < chest.size(); i++) {
            if (chest.getStack(i).isOf(item)) {
                ret += chest.getStack(i).getCount();
            }
        }
        return ret;
    }

    /** One ejector pushes one item a tick into the chest above; a comparator reads how full the 48 slots are. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void itemBufferEjectsAndSignals(TestContext ctx) {
        ctx.setBlockState(POS, IC2Blocks.ITEM_BUFFER_BLOCK.getDefaultState());
        ctx.setBlockState(POS.up(), Blocks.CHEST.getDefaultState());
        TileEntityItemBuffer buffer = ctx.getBlockEntity(POS);
        ctx.assertTrue(buffer.getComparatorOutput() == 0, "empty buffer signals");

        // 24 full stacks are half of 48 slots: 1 + 1536 * 14 / 3072 = 8
        for (int i = 0; i < 24; i++) {
            buffer.leftContentSlot.put(i, new ItemStack(Items.COBBLESTONE, 64));
        }
        ctx.assertTrue(buffer.getComparatorOutput() == 8, "comparator " + buffer.getComparatorOutput());

        buffer.upgradeSlot.put(0, new ItemStack(IC2ItemVariants.get("upgrade", "ejector")));
        buffer.tick();
        ctx.assertTrue(count(ctx, POS.up(), Items.COBBLESTONE) == 1, "ejected " + count(ctx, POS.up(), Items.COBBLESTONE));
        buffer.tick();
        ctx.assertTrue(count(ctx, POS.up(), Items.COBBLESTONE) == 2, "ejected " + count(ctx, POS.up(), Items.COBBLESTONE));
        ctx.complete();
    }

    /** A pulling upgrade in the compact buffer draws one item a tick out of the chest above. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void compactBufferPulls(TestContext ctx) {
        ctx.setBlockState(POS, IC2Blocks.ITEM_BUFFER_2_BLOCK.getDefaultState());
        ctx.setBlockState(POS.up(), Blocks.CHEST.getDefaultState());
        ChestBlockEntity chest = ctx.getBlockEntity(POS.up());
        chest.setStack(0, new ItemStack(Items.IRON_INGOT, 10));
        TileEntityBetterItemBuffer buffer = ctx.getBlockEntity(POS);
        buffer.upgradeSlot.put(0, new ItemStack(IC2ItemVariants.get("upgrade", "pulling")));
        buffer.tick();
        buffer.tick();
        ctx.assertTrue(buffer.bufferSlot.get(0).isOf(Items.IRON_INGOT) && buffer.bufferSlot.get(0).getCount() == 2,
                "buffer " + buffer.bufferSlot.get(0));
        ctx.assertTrue(count(ctx, POS.up(), Items.IRON_INGOT) == 8, "chest " + count(ctx, POS.up(), Items.IRON_INGOT));
        ctx.complete();
    }

    /**
     * A filter of 4 cobblestone on the east side sends batches of 4 east (80 EU each); dirt, which
     * no filter names, leaves through the default route below one item a tick (20 EU).
     */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void sortingMachineSortsByFilter(TestContext ctx) {
        ctx.setBlockState(POS, IC2Blocks.SORTING_BLOCK.getDefaultState());
        ctx.setBlockState(POS.east(), Blocks.CHEST.getDefaultState());
        ctx.setBlockState(POS.down(), Blocks.CHEST.getDefaultState());
        TileEntitySortingMachine sorter = ctx.getBlockEntity(POS);
        sorter.getFilterSlots(Direction.EAST)[0] = new ItemStack(Items.COBBLESTONE, 4);
        sorter.buffer.put(0, new ItemStack(Items.COBBLESTONE, 10));
        sorter.buffer.put(1, new ItemStack(Items.DIRT, 3));

        sorter.injectEnergy(Direction.UP, 19, 128);
        sorter.tick();
        ctx.assertTrue(sorter.buffer.get(0).getCount() == 10, "sorted below 20 EU");

        sorter.injectEnergy(Direction.UP, 981, 128);
        sorter.tick();
        ctx.assertTrue(count(ctx, POS.east(), Items.COBBLESTONE) == 4, "east " + count(ctx, POS.east(), Items.COBBLESTONE));
        ctx.assertTrue(count(ctx, POS.down(), Items.DIRT) == 1, "down " + count(ctx, POS.down(), Items.DIRT));
        ctx.assertTrue(count(ctx, POS.down(), Items.COBBLESTONE) == 0, "filtered item took the default route");
        ctx.assertTrue(sorter.getEnergy() == 900, "energy " + sorter.getEnergy());

        sorter.tick();
        sorter.tick();
        // 10 -> 6 -> 2, and 2 is less than a batch of 4
        ctx.assertTrue(count(ctx, POS.east(), Items.COBBLESTONE) == 8, "east " + count(ctx, POS.east(), Items.COBBLESTONE));
        ctx.assertTrue(sorter.buffer.get(0).getCount() == 2, "left " + sorter.buffer.get(0));
        ctx.assertTrue(count(ctx, POS.down(), Items.DIRT) == 3, "down " + count(ctx, POS.down(), Items.DIRT));
        ctx.assertTrue(sorter.getEnergy() == 900 - 80 - 40, "energy " + sorter.getEnergy());

        // the default route moves with the window's button
        sorter.onGuiEvent(null, "route:1");
        ctx.assertTrue(sorter.defaultRoute == Direction.UP && sorter.getGuiCondition("route_1"), "route " + sorter.defaultRoute);
        ctx.assertTrue(sorter.getGuiCondition("adj_5") && !sorter.getGuiCondition("adj_4"), "adjacency");
        ctx.complete();
    }

    /** Clicking an item into a filter slot of the window stores a copy and leaves the hand alone. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    @SuppressWarnings("removal")
    public void sortingMachineFilterIsAGhostSlot(TestContext ctx) {
        ctx.setBlockState(POS, IC2Blocks.SORTING_BLOCK.getDefaultState());
        TileEntitySortingMachine sorter = ctx.getBlockEntity(POS);
        ServerPlayerEntity player = ctx.createMockCreativeServerPlayerInWorld();
        DynamicContainer container = new DynamicContainer(0, player.getInventory(), sorter);

        int ghosts = 0;
        int first = -1;
        for (Slot slot : container.slots) {
            if (slot instanceof SlotHologram) {
                if (first < 0) {
                    first = slot.id;
                }
                ghosts++;
            }
        }
        ctx.assertTrue(ghosts == 42, "filter slots " + ghosts);

        container.setCursorStack(new ItemStack(Items.COBBLESTONE, 5));
        container.onSlotClick(first, 1, SlotActionType.PICKUP, player);
        container.onSlotClick(first, 1, SlotActionType.PICKUP, player);
        ItemStack filter = sorter.getFilterSlots(Direction.DOWN)[0];
        ctx.assertTrue(filter.isOf(Items.COBBLESTONE) && filter.getCount() == 2, "filter " + filter);
        ctx.assertTrue(container.getCursorStack().getCount() == 5, "cursor " + container.getCursorStack());
        ctx.assertTrue(sorter.isEmpty(), "filter counted as inventory");
        ctx.complete();
    }

    /** The first side of the list gets what it takes, the second the rest; the faced side drops out. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void weightedItemDistributorFollowsPriority(TestContext ctx) {
        ctx.setBlockState(POS, IC2Blocks.WEIGHTED_ITEM_DISTRIBUTOR_BLOCK.getDefaultState());
        ctx.setBlockState(POS.east(), Blocks.CHEST.getDefaultState());
        ctx.setBlockState(POS.west(), Blocks.CHEST.getDefaultState());
        TileEntityWeightedItemDistributor distributor = ctx.getBlockEntity(POS);
        distributor.setFacing(Direction.UP);
        distributor.onGuiEvent(null, "prio:0:4"); // east first
        distributor.onGuiEvent(null, "prio:1:3"); // then west
        ctx.assertTrue(distributor.getPriority().equals(java.util.List.of(Direction.EAST, Direction.WEST)), "priority " + distributor.getPriority());

        ChestBlockEntity east = ctx.getBlockEntity(POS.east());
        for (int i = 0; i < 26; i++) {
            east.setStack(i, new ItemStack(Items.DIRT, 64));
        }
        for (int i = 0; i < 9; i++) {
            distributor.buffer.put(i, new ItemStack(Items.COBBLESTONE, 64));
        }
        // the east chest takes one stack; as every stack it took went in whole, the tick ends there
        distributor.tick();
        ctx.assertTrue(count(ctx, POS.east(), Items.COBBLESTONE) == 64, "east " + count(ctx, POS.east(), Items.COBBLESTONE));
        ctx.assertTrue(count(ctx, POS.west(), Items.COBBLESTONE) == 0, "west " + count(ctx, POS.west(), Items.COBBLESTONE));
        // now the full east chest takes nothing and the rest goes west
        distributor.tick();
        ctx.assertTrue(count(ctx, POS.east(), Items.COBBLESTONE) == 64, "east " + count(ctx, POS.east(), Items.COBBLESTONE));
        ctx.assertTrue(count(ctx, POS.west(), Items.COBBLESTONE) == 512, "west " + count(ctx, POS.west(), Items.COBBLESTONE));
        ctx.assertTrue(distributor.buffer.isEmpty(), "buffer not emptied");

        distributor.setFacing(Direction.EAST);
        ctx.assertTrue(distributor.getPriority().equals(java.util.List.of(Direction.WEST)), "faced side kept " + distributor.getPriority());
        ctx.complete();
    }

    /** One oak log in the recipe grid: four planks every 40 ticks for 80 EU, one log used each time. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void batchCrafterCraftsPlanks(TestContext ctx) {
        ctx.setBlockState(POS, IC2Blocks.BATCH_CRAFTER_BLOCK.getDefaultState());
        TileEntityBatchCrafter crafter = ctx.getBlockEntity(POS);
        crafter.craftingGrid[4] = new ItemStack(Items.OAK_LOG);
        crafter.onHologramChanged("grid", 4);
        ctx.assertTrue(crafter.recipeOutput.isOf(Items.OAK_PLANKS) && crafter.recipeOutput.getCount() == 4, "recipe " + crafter.recipeOutput);
        ctx.assertTrue(crafter.ingredientsRow[4].accepts(new ItemStack(Items.OAK_LOG)), "log refused in its cell");
        ctx.assertTrue(!crafter.ingredientsRow[0].accepts(new ItemStack(Items.OAK_LOG)), "log taken in an empty cell");
        ctx.assertTrue(!crafter.ingredientsRow[4].accepts(new ItemStack(Items.DIRT)), "dirt taken");

        crafter.ingredientsRow[4].put(new ItemStack(Items.OAK_LOG, 3));
        crafter.injectEnergy(Direction.UP, 1000, 32);
        for (int i = 0; i < 39; i++) {
            crafter.tick();
        }
        ctx.assertTrue(crafter.craftingOutput.isEmpty(), "done before 40 ticks");
        ctx.assertTrue(crafter.getComparatorOutput() == 39 * 15 / 40, "comparator " + crafter.getComparatorOutput());
        crafter.tick();
        ctx.assertTrue(crafter.craftingOutput.get().isOf(Items.OAK_PLANKS) && crafter.craftingOutput.get().getCount() == 4,
                "output " + crafter.craftingOutput.get());
        ctx.assertTrue(crafter.ingredientsRow[4].get().getCount() == 2, "logs " + crafter.ingredientsRow[4].get());
        ctx.assertTrue(crafter.getEnergy() == 1000 - 80, "energy " + crafter.getEnergy());
        ctx.complete();
    }

    /** A honey bottle makes three sugar; its glass bottle goes into the container slots. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void batchCrafterKeepsContainers(TestContext ctx) {
        ctx.setBlockState(POS, IC2Blocks.BATCH_CRAFTER_BLOCK.getDefaultState());
        TileEntityBatchCrafter crafter = ctx.getBlockEntity(POS);
        crafter.craftingGrid[0] = new ItemStack(Items.HONEY_BOTTLE);
        crafter.onHologramChanged("grid", 0);
        crafter.ingredientsRow[0].put(new ItemStack(Items.HONEY_BOTTLE, 2));
        crafter.injectEnergy(Direction.UP, 1000, 32);
        for (int i = 0; i < 40; i++) {
            crafter.tick();
        }
        ctx.assertTrue(crafter.craftingOutput.get().isOf(Items.SUGAR) && crafter.craftingOutput.get().getCount() == 3,
                "output " + crafter.craftingOutput.get());
        ctx.assertTrue(crafter.containerOutput.get(0).isOf(Items.GLASS_BOTTLE), "containers " + crafter.containerOutput.get(0));
        ctx.assertTrue(crafter.ingredientsRow[0].get().getCount() == 1, "honey " + crafter.ingredientsRow[0].get());
        ctx.complete();
    }

    /** The assembly bench knows only UU patterns: one UU-matter in the middle makes 16 stone. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void assemblyBenchCraftsUuPatterns(TestContext ctx) {
        ctx.setBlockState(POS, IC2Blocks.UU_ASSEMBLY_BENCH_BLOCK.getDefaultState());
        TileEntityAssemblyBench bench = ctx.getBlockEntity(POS);
        ctx.assertTrue(!TileEntityAssemblyBench.RECIPES.isEmpty(), "no UU recipes loaded");
        Item matter = IC2ItemVariants.get("misc_resource", "matter");

        // a vanilla recipe is no pattern here
        bench.craftingGrid[4] = new ItemStack(Items.OAK_LOG);
        bench.onHologramChanged("grid", 4);
        ctx.assertTrue(bench.recipeOutput.isEmpty(), "vanilla recipe " + bench.recipeOutput);

        bench.craftingGrid[4] = new ItemStack(matter);
        bench.onHologramChanged("grid", 4);
        ctx.assertTrue(bench.recipeOutput.isOf(Items.STONE) && bench.recipeOutput.getCount() == 16, "pattern " + bench.recipeOutput);
        bench.ingredientsRow[4].put(new ItemStack(matter));
        bench.injectEnergy(Direction.UP, 1000, 32);
        for (int i = 0; i < 40; i++) {
            bench.tick();
        }
        ctx.assertTrue(bench.craftingOutput.get().isOf(Items.STONE) && bench.craftingOutput.get().getCount() == 16,
                "output " + bench.craftingOutput.get());
        ctx.assertTrue(bench.ingredientsRow[4].isEmpty(), "UU-matter left");
        ctx.complete();
    }

    /**
     * Sticks from planks in the grid; after the craft the grid refills from the storage, evenly.
     * The forge hammer spot makes a copper plate and wears the hammer by one use.
     */
    @GameTest(templateName = EMPTY_STRUCTURE)
    @SuppressWarnings("removal")
    public void industrialWorkbenchCraftsAndRefills(TestContext ctx) {
        ctx.setBlockState(POS, IC2Blocks.INDUSTRIAL_WORKBENCH_BLOCK.getDefaultState());
        TileEntityIndustrialWorkbench bench = ctx.getBlockEntity(POS);
        ServerPlayerEntity player = ctx.createMockCreativeServerPlayerInWorld();
        bench.onPlaced(new ItemStack(IC2Blocks.INDUSTRIAL_WORKBENCH_BLOCK), player);
        ctx.assertTrue(bench.leftCrafting.tool.get().isOf(Registries.ITEM.get(Identifier.of("ic2", "forge_hammer"))), "no hammer");
        ctx.assertTrue(!bench.rightCrafting.tool.isEmpty(), "no cutter");

        bench.craftingGrid.put(0, new ItemStack(Items.OAK_PLANKS, 2));
        bench.craftingGrid.put(3, new ItemStack(Items.OAK_PLANKS, 3));
        bench.craftingStorage.put(0, new ItemStack(Items.OAK_PLANKS, 9));

        DynamicContainer container = new DynamicContainer(0, player.getInventory(), bench);
        SlotCraftResult main = null;
        for (Slot slot : container.slots) {
            if (slot instanceof SlotCraftResult result && result.name.equals("main")) {
                main = result;
            }
        }
        ctx.assertTrue(main != null, "no result slot");
        container.sendContentUpdates();
        ctx.assertTrue(main.getStack().isOf(Items.STICK) && main.getStack().getCount() == 4, "result " + main.getStack());

        container.quickMove(player, main.id);
        // 1 + 2 left in the grid plus 9 from the storage, spread over the two cells
        ctx.assertTrue(bench.craftingGrid.get(0).getCount() == 6 && bench.craftingGrid.get(3).getCount() == 6,
                "grid " + bench.craftingGrid.get(0) + " " + bench.craftingGrid.get(3));
        ctx.assertTrue(bench.craftingStorage.isEmpty(), "storage " + bench.craftingStorage.get(0));
        ctx.assertTrue(player.getInventory().count(Items.STICK) == 4, "sticks " + player.getInventory().count(Items.STICK));

        bench.leftCrafting.input.put(new ItemStack(Items.COPPER_INGOT));
        ItemStack plate = bench.craftResult("left", player);
        ctx.assertTrue(plate.isOf(Registries.ITEM.get(Identifier.of("ic2", "plate_copper"))), "plate " + plate);
        ctx.assertTrue(bench.leftCrafting.input.isEmpty(), "ingot left");
        ItemStack hammer = bench.leftCrafting.tool.get();
        ctx.assertTrue(hammer.getItem() instanceof ICustomDamageItem tool && tool.getCustomDamage(hammer) == 1, "hammer " + hammer);

        bench.clear(player);
        ctx.assertTrue(bench.craftingGrid.isEmpty() && bench.craftingStorage.get(0).getCount() == 12, "clear " + bench.craftingStorage.get(0));
        ctx.complete();
    }
}
