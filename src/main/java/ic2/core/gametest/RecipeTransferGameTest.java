package ic2.core.gametest;

import ic2.core.block.machine.tileentity.TileEntityBatchCrafter;
import ic2.core.block.machine.tileentity.TileEntityIndustrialWorkbench;
import ic2.core.init.IC2Blocks;
import ic2.core.init.IC2ItemVariants;
import ic2.core.network.RecipeTransferPayload;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/** A recipe viewer's "+" in the industrial workbench and the batch crafter. Run with {@code ./gradlew runGametest}. */
public class RecipeTransferGameTest implements FabricGameTest {
    private static final BlockPos POS = new BlockPos(2, 2, 2);

    /** A 3x3 grid with the item in the given cells. */
    private static List<List<ItemStack>> grid(Item item, int... cells) {
        List<List<ItemStack>> grid = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            grid.add(new ArrayList<>());
        }
        for (int cell : cells) {
            grid.get(cell).add(new ItemStack(item));
        }
        return grid;
    }

    private static int count(ServerPlayerEntity player, Item item) {
        int n = 0;
        for (ItemStack stack : player.getInventory().main) {
            if (stack.isOf(item)) {
                n += stack.getCount();
            }
        }
        return n;
    }

    /** The storage gives first, then the player; shift fills as many crafts as there are items for. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void workbenchFillsTheGrid(TestContext ctx) {
        ctx.setBlockState(POS, IC2Blocks.INDUSTRIAL_WORKBENCH_BLOCK.getDefaultState());
        TileEntityIndustrialWorkbench bench = ctx.getBlockEntity(POS);
        ServerPlayerEntity player = ctx.createMockCreativeServerPlayerInWorld();
        player.getInventory().main.clear();
        bench.craftingStorage.put(0, new ItemStack(Items.OAK_PLANKS, 2));
        player.getInventory().insertStack(new ItemStack(Items.OAK_PLANKS, 3));

        ctx.assertTrue(bench.transferRecipe(player, grid(Items.OAK_PLANKS, 0, 3), false), "nothing moved");
        ctx.assertTrue(bench.craftingGrid.get(0).getCount() == 1 && bench.craftingGrid.get(3).getCount() == 1, "grid " + bench.craftingGrid.get(0) + " " + bench.craftingGrid.get(3));
        ctx.assertTrue(bench.craftingStorage.isEmpty(0) && count(player, Items.OAK_PLANKS) == 3, "storage was not used first");

        // the grid goes back first, then as many rounds as five planks give for two cells
        ctx.assertTrue(bench.transferRecipe(player, grid(Items.OAK_PLANKS, 0, 3), true), "nothing moved with shift");
        ctx.assertTrue(bench.craftingGrid.get(0).getCount() == 2 && bench.craftingGrid.get(3).getCount() == 2, "max grid " + bench.craftingGrid.get(0) + " " + bench.craftingGrid.get(3));
        int left = count(player, Items.OAK_PLANKS) + (bench.craftingStorage.isEmpty(0) ? 0 : bench.craftingStorage.get(0).getCount());
        ctx.assertTrue(left == 1, "planks left " + left);
        ctx.complete();
    }

    /** Without one craft's worth nothing moves, and the grid keeps what it had. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void workbenchLeavesTheGridWhenItemsAreMissing(TestContext ctx) {
        ctx.setBlockState(POS, IC2Blocks.INDUSTRIAL_WORKBENCH_BLOCK.getDefaultState());
        TileEntityIndustrialWorkbench bench = ctx.getBlockEntity(POS);
        ServerPlayerEntity player = ctx.createMockCreativeServerPlayerInWorld();
        player.getInventory().main.clear();
        bench.craftingGrid.put(4, new ItemStack(Items.STICK));
        player.getInventory().insertStack(new ItemStack(Items.COAL));

        List<List<ItemStack>> torch = grid(Items.COAL, 1);
        torch.get(4).add(new ItemStack(Items.STICK));
        torch.get(7).add(new ItemStack(Items.STICK));
        ctx.assertTrue(!bench.transferRecipe(player, torch, false), "moved with one stick short");
        ctx.assertTrue(bench.craftingGrid.get(4).isOf(Items.STICK) && count(player, Items.COAL) == 1, "something moved");

        player.getInventory().insertStack(new ItemStack(Items.STICK));
        ctx.assertTrue(bench.transferRecipe(player, torch, false), "the grid's own stick did not count");
        ctx.assertTrue(bench.craftingGrid.get(1).isOf(Items.COAL) && bench.craftingGrid.get(4).isOf(Items.STICK)
                && bench.craftingGrid.get(7).isOf(Items.STICK), "torch not laid out");
        ctx.complete();
    }

    /** A hammer recipe of two items goes into the hammer's spot, where it crafts. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void workbenchUsesTheToolSpot(TestContext ctx) {
        ctx.setBlockState(POS, IC2Blocks.INDUSTRIAL_WORKBENCH_BLOCK.getDefaultState());
        TileEntityIndustrialWorkbench bench = ctx.getBlockEntity(POS);
        ServerPlayerEntity player = ctx.createMockCreativeServerPlayerInWorld();
        player.getInventory().main.clear();
        Item hammer = Registries.ITEM.get(Identifier.of("ic2", "forge_hammer"));
        Item plate = IC2ItemVariants.get("plate", "bronze");
        player.getInventory().insertStack(new ItemStack(hammer));
        bench.craftingStorage.put(0, new ItemStack(plate, 4));

        List<List<ItemStack>> cells = grid(plate, 0);
        cells.get(1).add(new ItemStack(hammer));
        ctx.assertTrue(bench.transferRecipe(player, cells, true), "nothing moved");
        ctx.assertTrue(bench.leftCrafting.tool.get().isOf(hammer), "tool " + bench.leftCrafting.tool.get());
        ctx.assertTrue(bench.leftCrafting.input.get().isOf(plate) && bench.leftCrafting.input.get().getCount() == 4,
                "input " + bench.leftCrafting.input.get());
        ctx.assertTrue(bench.leftCrafting.getOutputStack().isOf(Registries.ITEM.get(Identifier.of("ic2", "casing_bronze"))),
                "crafts " + bench.leftCrafting.getOutputStack());
        ctx.assertTrue(bench.craftingGrid.isEmpty(), "grid used as well");
        ctx.complete();
    }

    /** The batch crafter only takes the recipe as its ghost grid. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void batchCrafterTakesTheRecipe(TestContext ctx) {
        ctx.setBlockState(POS, IC2Blocks.BATCH_CRAFTER_BLOCK.getDefaultState());
        TileEntityBatchCrafter crafter = ctx.getBlockEntity(POS);
        ServerPlayerEntity player = ctx.createMockCreativeServerPlayerInWorld();
        List<List<ItemStack>> torch = grid(Items.COAL, 1);
        torch.get(1).add(new ItemStack(Items.CHARCOAL));
        torch.get(4).add(new ItemStack(Items.STICK));

        ctx.assertTrue(crafter.transferRecipe(player, torch, false), "not taken");
        ctx.assertTrue(crafter.craftingGrid[1].isOf(Items.COAL) && crafter.craftingGrid[4].isOf(Items.STICK)
                && crafter.craftingGrid[0].isEmpty(), "grid " + java.util.Arrays.toString(crafter.craftingGrid));
        ctx.assertTrue(crafter.recipeOutput.isOf(Items.TORCH) && crafter.recipeOutput.getCount() == 4, "result " + crafter.recipeOutput);
        ctx.complete();
    }

    /** The packet only works on the window the player has open. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void packetGoesOnlyToTheOpenWindow(TestContext ctx) {
        ctx.setBlockState(POS, IC2Blocks.BATCH_CRAFTER_BLOCK.getDefaultState());
        TileEntityBatchCrafter crafter = ctx.getBlockEntity(POS);
        ServerPlayerEntity player = ctx.createMockCreativeServerPlayerInWorld();
        player.setPosition(ctx.getAbsolutePos(POS).toCenterPos().add(0, 1, 0));
        player.openHandledScreen(crafter);
        int syncId = player.currentScreenHandler.syncId;

        RecipeTransferPayload.receive(player, new RecipeTransferPayload(syncId + 1, grid(Items.STICK, 0), false));
        ctx.assertTrue(crafter.craftingGrid[0].isEmpty(), "taken by another window's packet");
        RecipeTransferPayload.receive(player, new RecipeTransferPayload(syncId, grid(Items.STICK, 0), false));
        ctx.assertTrue(crafter.craftingGrid[0].isOf(Items.STICK), "not taken from the open window: " + player.currentScreenHandler);
        ctx.complete();
    }
}
