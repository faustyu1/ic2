package ic2.core.gametest;

import ic2.core.block.TileEntityBlock;
import ic2.core.block.storage.box.TileEntityStorageBox;
import ic2.core.block.wiring.TileEntityElectricBlock;
import ic2.core.init.IC2Blocks;
import ic2.core.init.IC2ClassicBlocks;
import ic2.core.init.IC2ItemVariants;
import ic2.core.init.IC2Items;
import ic2.core.item.ItemWrench;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

/**
 * What IC2 blocks give back when they are mined, wrenched or blown up, and the IC2 loot in the
 * vanilla chests - checked through the real breaking path of a survival player.
 * Run with {@code ./gradlew runGametest}.
 */
public class DropGameTest implements FabricGameTest {
    private static final BlockPos POS = new BlockPos(2, 2, 2);

    @SuppressWarnings("removal")
    private static ServerPlayerEntity player(TestContext ctx) {
        ServerPlayerEntity player = ctx.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);
        return player;
    }

    /**
     * Breaks the block as a survival player does - the steps of tryBreakBlock's survival branch.
     * The game test player answers isCreative() with true whatever its game mode, so the real
     * method would never drop anything.
     */
    private static void mine(TestContext ctx, ServerPlayerEntity player, ItemStack tool, BlockPos rel) {
        player.setStackInHand(Hand.MAIN_HAND, tool);
        BlockPos pos = ctx.getAbsolutePos(rel);
        BlockState state = ctx.getWorld().getBlockState(pos);
        BlockEntity be = ctx.getWorld().getBlockEntity(pos);
        boolean harvest = player.canHarvest(state);
        BlockState broken = state.getBlock().onBreak(ctx.getWorld(), pos, state, player);
        ctx.getWorld().removeBlock(pos, false);
        if (harvest) {
            broken.getBlock().afterBreak(ctx.getWorld(), player, pos, broken, be, tool.copy());
        }
    }

    private static List<ItemEntity> drops(TestContext ctx, BlockPos rel) {
        return ctx.getWorld().getEntitiesByClass(ItemEntity.class, new Box(ctx.getAbsolutePos(rel)).expand(1.5), e -> true);
    }

    private static ItemStack only(TestContext ctx, BlockPos rel, String what) {
        List<ItemEntity> items = drops(ctx, rel);
        ctx.assertTrue(items.size() == 1, what + ": expected one drop, got " + items.stream().map(e -> e.getStack().toString()).toList());
        ItemStack stack = items.get(0).getStack().copy();
        items.forEach(ItemEntity::discard);
        return stack;
    }

    private static void none(TestContext ctx, BlockPos rel, String what) {
        List<ItemEntity> items = drops(ctx, rel);
        ctx.assertTrue(items.isEmpty(), what + ": expected no drop, got " + items.stream().map(e -> e.getStack().toString()).toList());
    }

    private static Item item(String id) {
        return Registries.ITEM.get(Identifier.of("ic2", id));
    }

    /** Places a block from its item through the item's own placement, as a player would. */
    private static void place(TestContext ctx, ServerPlayerEntity player, ItemStack stack, BlockPos rel) {
        player.setStackInHand(Hand.MAIN_HAND, stack);
        BlockPos below = ctx.getAbsolutePos(rel.down());
        BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(below).add(0, 0.5, 0), Direction.UP, below, false);
        stack.useOnBlock(new ItemPlacementContext(player, Hand.MAIN_HAND, stack, hit));
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void machinesGiveTheirCasingMinedAndThemselvesWrenched(TestContext ctx) {
        ServerPlayerEntity player = player(ctx);

        ctx.setBlockState(POS, IC2Blocks.MACERATOR_BLOCK.getDefaultState());
        mine(ctx, player, new ItemStack(Items.IRON_PICKAXE), POS);
        ctx.assertTrue(only(ctx, POS, "macerator mined").isOf(IC2ClassicBlocks.MACHINE_BLOCK.asItem()), "macerator mined is not a casing");

        // a hand cannot get a machine out at all
        ctx.setBlockState(POS, IC2Blocks.MACERATOR_BLOCK.getDefaultState());
        mine(ctx, player, ItemStack.EMPTY, POS);
        none(ctx, POS, "macerator by hand");

        // the wrench on the side it already faces takes it out whole
        ctx.setBlockState(POS, IC2Blocks.MACERATOR_BLOCK.getDefaultState());
        TileEntityBlock macerator = ctx.getBlockEntity(POS);
        ItemWrench.wrenchBlock(ctx.getWorld(), ctx.getAbsolutePos(POS), macerator.getFacing(), player, true);
        ctx.assertTrue(only(ctx, POS, "macerator wrenched").isOf(IC2Blocks.MACERATOR_BLOCK.asItem()), "wrench did not give the macerator");

        // a geothermal generator mined gives a plain generator
        ctx.setBlockState(POS, IC2Blocks.GEOTHERMAL_BLOCK.getDefaultState());
        mine(ctx, player, new ItemStack(Items.IRON_PICKAXE), POS);
        ctx.assertTrue(only(ctx, POS, "geothermal mined").isOf(IC2Blocks.GENERATOR_BLOCK.asItem()), "geothermal is not a generator");

        // the wrench turns a horizontal machine only sideways, and leaves crops and cables alone
        ctx.setBlockState(POS, IC2Blocks.MACERATOR_BLOCK.getDefaultState());
        macerator = ctx.getBlockEntity(POS);
        ctx.assertTrue(!macerator.setFacingWrench(Direction.UP, player), "macerator turned up");
        ctx.assertTrue(macerator.setFacingWrench(Direction.EAST, player) && macerator.getFacing() == Direction.EAST, "macerator not turned east");
        ctx.setBlockState(POS, IC2Blocks.CABLE_BLOCK.getDefaultState());
        ctx.assertTrue(ItemWrench.wrenchBlock(ctx.getWorld(), ctx.getAbsolutePos(POS), Direction.NORTH, player, true)
                == ItemWrench.WrenchResult.NOTHING, "wrench removed a cable");
        mine(ctx, player, ItemStack.EMPTY, POS);
        ctx.assertTrue(only(ctx, POS, "cable by hand").isOf(IC2Blocks.CABLE_BLOCK.asItem()), "cable not dropped");

        // every tile entity block can build the block entity its tooltip asks
        for (Block block : Registries.BLOCK) {
            if (block instanceof ic2.core.block.BlockTileEntity) {
                List<net.minecraft.text.Text> tooltip = new java.util.ArrayList<>();
                block.appendTooltip(new ItemStack(block), Item.TooltipContext.DEFAULT, tooltip, net.minecraft.item.tooltip.TooltipType.BASIC);
                if (block == IC2Blocks.getBlock(ic2.core.ref.TeBlock.iron_storage_box)) {
                    ctx.assertTrue(tooltip.size() == 2, "storage box tooltip " + tooltip);
                }
            }
        }

        // the safe is only for its owner's wrench
        ctx.assertTrue(IC2Blocks.PERSONAL_CHEST_BLOCK.getDefaultState().getHardness(ctx.getWorld(), POS) < 0, "safe breakable");
        ctx.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void storageKeepsItsContentsInTheItem(TestContext ctx) {
        ServerPlayerEntity player = player(ctx);
        ctx.setBlockState(POS.down(), Blocks.STONE.getDefaultState());

        // a storage box carries its items and gives them back when placed
        ctx.setBlockState(POS, IC2Blocks.getBlock(ic2.core.ref.TeBlock.iron_storage_box).getDefaultState());
        TileEntityStorageBox box = ctx.getBlockEntity(POS);
        box.getInventory().put(3, new ItemStack(Items.DIAMOND, 7));
        mine(ctx, player, new ItemStack(Items.IRON_PICKAXE), POS);
        ItemStack boxItem = only(ctx, POS, "box mined");
        ctx.assertTrue(boxItem.contains(DataComponentTypes.CUSTOM_DATA), "box item holds nothing");
        place(ctx, player, boxItem, POS);
        TileEntityStorageBox placed = ctx.getBlockEntity(POS);
        ctx.assertTrue(placed.getInventory().get(3).isOf(Items.DIAMOND) && placed.getInventory().get(3).getCount() == 7, "box contents not back");

        // blown up, it keeps them as well
        ctx.getWorld().breakBlock(ctx.getAbsolutePos(POS), true);
        ItemStack blown = only(ctx, POS, "box broken");
        ctx.assertTrue(blown.contains(DataComponentTypes.CUSTOM_DATA), "broken box lost its items");

        // a BatBox keeps 80% of its energy
        ctx.setBlockState(POS, IC2Blocks.BATBOX_BLOCK.getDefaultState());
        TileEntityElectricBlock batbox = ctx.getBlockEntity(POS);
        batbox.injectEnergy(Direction.UP, 10000, 32);
        mine(ctx, player, new ItemStack(Items.IRON_PICKAXE), POS);
        ItemStack batboxItem = only(ctx, POS, "batbox mined");
        place(ctx, player, batboxItem, POS);
        TileEntityElectricBlock placedBatbox = ctx.getBlockEntity(POS);
        ctx.assertTrue(placedBatbox.getStored() == 8000, "batbox energy " + placedBatbox.getStored());

        // a tank keeps its fluid
        ctx.setBlockState(POS, IC2Blocks.getBlock(ic2.core.ref.TeBlock.iron_tank).getDefaultState());
        ic2.core.block.storage.tank.TileEntityTank tank = ctx.getBlockEntity(POS);
        tank.getTank().fillInternal(new net.minecraftforge.fluids.FluidStack(net.minecraftforge.fluids.FluidRegistry.getFluid("water"), 3000), true);
        mine(ctx, player, new ItemStack(Items.IRON_PICKAXE), POS);
        place(ctx, player, only(ctx, POS, "tank mined"), POS);
        ic2.core.block.storage.tank.TileEntityTank placedTank = ctx.getBlockEntity(POS);
        ctx.assertTrue(placedTank.getTank().getFluidAmount() == 3000, "tank fluid " + placedTank.getTank().getFluidAmount());

        // a block entity loaded from a chunk still knows its type, so it can be saved again
        BlockEntity loaded = BlockEntity.createFromNbt(ctx.getAbsolutePos(POS), ctx.getBlockState(POS),
                placedTank.createNbtWithIdentifyingData(ctx.getWorld().getRegistryManager()), ctx.getWorld().getRegistryManager());
        ctx.assertTrue(loaded != null && loaded.getType() == placedTank.getType(), "loaded tank has no type");
        ctx.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void plainBlocksNeedTheirTool(TestContext ctx) {
        ServerPlayerEntity player = player(ctx);

        ctx.setBlockState(POS, IC2ClassicBlocks.TIN_ORE.getDefaultState());
        mine(ctx, player, new ItemStack(Items.WOODEN_PICKAXE), POS);
        none(ctx, POS, "tin ore with wood");
        ctx.setBlockState(POS, IC2ClassicBlocks.TIN_ORE.getDefaultState());
        mine(ctx, player, new ItemStack(Items.STONE_PICKAXE), POS);
        ctx.assertTrue(only(ctx, POS, "tin ore with stone").isOf(IC2ClassicBlocks.TIN_ORE.asItem()), "tin ore");

        ctx.setBlockState(POS, IC2ClassicBlocks.URANIUM_ORE.getDefaultState());
        mine(ctx, player, new ItemStack(Items.STONE_PICKAXE), POS);
        none(ctx, POS, "uranium with stone");
        ctx.setBlockState(POS, IC2ClassicBlocks.URANIUM_ORE.getDefaultState());
        mine(ctx, player, new ItemStack(Items.IRON_PICKAXE), POS);
        ctx.assertTrue(only(ctx, POS, "uranium with iron").isOf(IC2ClassicBlocks.URANIUM_ORE.asItem()), "uranium ore");

        for (Block block : new Block[] {IC2ClassicBlocks.BRONZE_BLOCK, IC2ClassicBlocks.MACHINE_BLOCK, IC2ClassicBlocks.REFRACTORY_BRICKS}) {
            ctx.setBlockState(POS, block.getDefaultState());
            mine(ctx, player, new ItemStack(Items.WOODEN_PICKAXE), POS);
            ctx.assertTrue(only(ctx, POS, block.toString()).isOf(block.asItem()), block + " not dropped");
        }

        ctx.setBlockState(POS, IC2ClassicBlocks.RUBBER_WOOD.getDefaultState());
        mine(ctx, player, ItemStack.EMPTY, POS);
        ctx.assertTrue(drops(ctx, POS).stream().anyMatch(e -> e.getStack().isOf(IC2ClassicBlocks.RUBBER_WOOD.asItem())), "rubber wood");
        drops(ctx, POS).forEach(ItemEntity::discard);

        ctx.setBlockState(POS, IC2ClassicBlocks.RUBBER_LEAVES.getDefaultState());
        mine(ctx, player, new ItemStack(Items.SHEARS), POS);
        ctx.assertTrue(only(ctx, POS, "leaves with shears").isOf(IC2ClassicBlocks.RUBBER_LEAVES.asItem()), "leaves");

        ctx.setBlockState(POS, IC2ClassicBlocks.REINFORCED_GLASS.getDefaultState());
        mine(ctx, player, new ItemStack(Items.IRON_PICKAXE), POS);
        none(ctx, POS, "reinforced glass");

        BlockState door = IC2ClassicBlocks.REINFORCED_DOOR.getDefaultState();
        ctx.setBlockState(POS, door.with(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        ctx.setBlockState(POS.up(), door.with(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        mine(ctx, player, new ItemStack(Items.DIAMOND_PICKAXE), POS);
        ctx.assertTrue(only(ctx, POS, "door").isOf(IC2ClassicBlocks.REINFORCED_DOOR.asItem()), "door");
        ctx.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void dungeonChestsHoldIc2LootAndTinCansFeed(TestContext ctx) {
        LootTable table = ctx.getWorld().getServer().getReloadableRegistries().getLootTable(
                RegistryKey.of(RegistryKeys.LOOT_TABLE, Identifier.ofVanilla("chests/simple_dungeon")));
        boolean found = false;
        for (int i = 0; i < 300 && !found; i++) {
            LootContextParameterSet params = new LootContextParameterSet.Builder(ctx.getWorld())
                    .add(LootContextParameters.ORIGIN, Vec3d.ofCenter(ctx.getAbsolutePos(POS)))
                    .build(LootContextTypes.CHEST);
            found = table.generateLoot(params).stream().anyMatch(s -> Registries.ITEM.getId(s.getItem()).getNamespace().equals("ic2"));
        }
        ctx.assertTrue(found, "no IC2 item in 300 dungeon chests");

        ServerPlayerEntity player = player(ctx);
        player.getHungerManager().setFoodLevel(10);
        ItemStack cans = new ItemStack(item("filled_tin_can"), 5);
        player.setStackInHand(Hand.MAIN_HAND, cans);
        cans.use(ctx.getWorld(), player, Hand.MAIN_HAND);
        ctx.assertTrue(player.getHungerManager().getFoodLevel() == 15, "food " + player.getHungerManager().getFoodLevel());
        Item emptyCan = IC2ItemVariants.get("crafting", "tin_can");
        ctx.assertTrue(player.getInventory().count(emptyCan) == 5, "empty cans " + player.getInventory().count(emptyCan));
        ctx.complete();
    }
}
