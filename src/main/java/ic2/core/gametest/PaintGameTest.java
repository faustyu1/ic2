package ic2.core.gametest;

import ic2.core.block.BlockFoam;
import ic2.core.block.BlockWall;
import ic2.core.block.wiring.BlockCable;
import ic2.core.block.wiring.BlockColoredCable;
import ic2.core.block.wiring.TileEntityCable;
import ic2.core.init.IC2ClassicBlocks;
import ic2.core.init.IC2ItemVariants;
import ic2.core.item.tool.ItemToolPainter;
import ic2.core.util.Ic2Color;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

/**
 * The painter on everything it paints, and construction foam setting into walls.
 * Run with {@code ./gradlew runGametest}.
 */
public class PaintGameTest implements FabricGameTest {
    private static final BlockPos A = new BlockPos(2, 2, 2);
    private static final BlockPos B = A.east();

    @SuppressWarnings("removal")
    private static ServerPlayerEntity player(TestContext ctx) {
        ServerPlayerEntity player = ctx.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);
        return player;
    }

    private static ItemStack painter(Ic2Color color) {
        return new ItemStack(IC2ItemVariants.get("painter", color.getName()));
    }

    /** Uses the stack in the main hand on the top face of the block. */
    private static ActionResult use(TestContext ctx, ServerPlayerEntity player, ItemStack stack, BlockPos rel) {
        player.setStackInHand(Hand.MAIN_HAND, stack);
        BlockPos abs = ctx.getAbsolutePos(rel);
        BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(abs).add(0, 0.5, 0), Direction.UP, abs, false);
        return player.getStackInHand(Hand.MAIN_HAND).useOnBlock(new ItemUsageContext(player, Hand.MAIN_HAND, hit));
    }

    private static Block cable(String name) {
        return Registries.BLOCK.get(Identifier.of("ic2", name));
    }

    private static void placeCable(TestContext ctx, Block block, BlockPos rel) {
        BlockCable cable = (BlockCable) block;
        ctx.setBlockState(rel, cable.withConnections(cable.getDefaultState(), ctx.getWorld(), ctx.getAbsolutePos(rel)));
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void paintedCablesOnlyJoinTheirOwnColour(TestContext ctx) {
        ServerPlayerEntity player = player(ctx);
        Block insulated = cable("copper_cable_1");
        placeCable(ctx, insulated, A);
        placeCable(ctx, insulated, B);
        ctx.assertTrue(ctx.getBlockState(A).get(net.minecraft.state.property.Properties.EAST), "unpainted cables not joined");

        // red next to unpainted still joins
        ItemStack red = painter(Ic2Color.red);
        use(ctx, player, red, A);
        ctx.assertTrue(ctx.getBlockState(A).get(BlockColoredCable.COLOR) == Ic2Color.red, "cable not red");
        ctx.assertTrue(player.getStackInHand(Hand.MAIN_HAND).getDamage() == 1, "painter not worn");
        ctx.assertTrue(ctx.getBlockState(A).get(net.minecraft.state.property.Properties.EAST), "red left unpainted");

        // red and blue part
        use(ctx, player, painter(Ic2Color.blue), B);
        ctx.assertTrue(!ctx.getBlockState(A).get(net.minecraft.state.property.Properties.EAST)
                && !ctx.getBlockState(B).get(net.minecraft.state.property.Properties.WEST), "red and blue still joined");
        TileEntityCable cableA = ctx.getBlockEntity(A);
        TileEntityCable cableB = ctx.getBlockEntity(B);
        ctx.assertTrue(!cableA.canInteractWith(cableB, Direction.EAST), "energy passes red to blue");

        // both red join again
        use(ctx, player, painter(Ic2Color.red), B);
        ctx.assertTrue(ctx.getBlockState(A).get(net.minecraft.state.property.Properties.EAST)
                && cableA.canInteractWith(cableB, Direction.EAST), "red and red not joined");

        // a bare copper cable takes no paint, glass fibre does
        placeCable(ctx, cable("copper_cable_0"), A);
        ctx.assertTrue(use(ctx, player, painter(Ic2Color.red), A) == ActionResult.PASS, "bare cable painted");
        placeCable(ctx, cable("glass_cable"), A);
        use(ctx, player, painter(Ic2Color.lime), A);
        ctx.assertTrue(ctx.getBlockState(A).get(BlockColoredCable.COLOR) == Ic2Color.lime, "glass cable not painted");
        ctx.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void painterWearsOutOrRefills(TestContext ctx) {
        ServerPlayerEntity player = player(ctx);
        ctx.setBlockState(A, Blocks.WHITE_WOOL.getDefaultState());

        ItemStack worn = painter(Ic2Color.red);
        worn.setDamage(ItemToolPainter.MAX_DAMAGE);
        use(ctx, player, worn, A);
        ctx.expectBlock(Blocks.RED_WOOL, A);
        ctx.assertTrue(player.getStackInHand(Hand.MAIN_HAND).isOf(IC2ItemVariants.get("painter", null)), "painter not blank");

        // the blank painter paints nothing
        ctx.assertTrue(use(ctx, player, player.getStackInHand(Hand.MAIN_HAND), A) == ActionResult.PASS, "blank painted");

        // with automatic refill a red dye fills it again
        ItemStack refill = painter(Ic2Color.blue);
        refill.setDamage(ItemToolPainter.MAX_DAMAGE);
        net.minecraft.nbt.NbtCompound nbt = new net.minecraft.nbt.NbtCompound();
        nbt.putBoolean("autoRefill", true);
        refill.set(net.minecraft.component.DataComponentTypes.CUSTOM_DATA, net.minecraft.component.type.NbtComponent.of(nbt));
        player.getInventory().insertStack(new ItemStack(Items.BLUE_DYE, 2));
        use(ctx, player, refill, A);
        ctx.expectBlock(Blocks.BLUE_WOOL, A);
        ItemStack held = player.getStackInHand(Hand.MAIN_HAND);
        ctx.assertTrue(held.getItem() instanceof ItemToolPainter p && p.getColor() == Ic2Color.blue && held.getDamage() == 0, "not refilled");
        ctx.assertTrue(player.getInventory().count(Items.BLUE_DYE) == 1, "dye not used");
        ctx.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void painterDyesVanillaBlocksAndSheep(TestContext ctx) {
        ServerPlayerEntity player = player(ctx);

        Block[][] cases = {
                {Blocks.GLASS, Blocks.CYAN_STAINED_GLASS},
                {Blocks.TERRACOTTA, Blocks.CYAN_TERRACOTTA},
                {Blocks.ORANGE_CONCRETE, Blocks.CYAN_CONCRETE},
                {Blocks.BLACK_CARPET, Blocks.CYAN_CARPET},
        };
        for (Block[] pair : cases) {
            ctx.setBlockState(A, pair[0].getDefaultState());
            use(ctx, player, painter(Ic2Color.cyan), A);
            ctx.expectBlock(pair[1], A);
        }

        // a pane keeps its connections
        ctx.setBlockState(A.down(), Blocks.STONE.getDefaultState());
        BlockState pane = Blocks.GLASS_PANE.getDefaultState().with(PaneBlock.NORTH, true).with(PaneBlock.SOUTH, true);
        ctx.setBlockState(A, pane);
        use(ctx, player, painter(Ic2Color.cyan), A);
        BlockState painted = ctx.getBlockState(A);
        ctx.assertTrue(painted.isOf(Blocks.CYAN_STAINED_GLASS_PANE) && painted.get(PaneBlock.NORTH), "pane " + painted);

        // stone takes no paint
        ctx.setBlockState(A, Blocks.STONE.getDefaultState());
        ctx.assertTrue(use(ctx, player, painter(Ic2Color.cyan), A) == ActionResult.PASS, "stone painted");

        SheepEntity sheep = ctx.spawnEntity(EntityType.SHEEP, B.up());
        sheep.setColor(DyeColor.WHITE);
        player.setStackInHand(Hand.MAIN_HAND, painter(Ic2Color.purple));
        UseEntityCallback.EVENT.invoker().interact(player, ctx.getWorld(), Hand.MAIN_HAND, sheep, null);
        ctx.assertTrue(sheep.getColor() == DyeColor.PURPLE, "sheep " + sheep.getColor());
        ctx.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void foamSetsIntoAWallThePainterColours(TestContext ctx) {
        ServerPlayerEntity player = player(ctx);

        // soft foam has nothing to stand on, and sand sets it at once
        ctx.setBlockState(A, IC2ClassicBlocks.FOAM.getDefaultState());
        ctx.assertTrue(ctx.getBlockState(A).getCollisionShape(ctx.getWorld(), ctx.getAbsolutePos(A), ShapeContext.absent()).isEmpty(),
                "foam is solid");
        ItemStack sand = new ItemStack(Items.SAND, 3);
        player.setStackInHand(Hand.MAIN_HAND, sand);
        BlockPos abs = ctx.getAbsolutePos(A);
        player.interactionManager.interactBlock(player, ctx.getWorld(), sand, Hand.MAIN_HAND,
                new BlockHitResult(Vec3d.ofCenter(abs), Direction.UP, abs, false));
        ctx.expectBlock(BlockWall.get(Ic2Color.light_gray), A);
        ctx.assertTrue(player.getStackInHand(Hand.MAIN_HAND).getCount() == 2, "sand not used");

        // left alone it sets on its own; the odds are 300 * (16 - light) seconds on average
        ctx.assertTrue(BlockFoam.hardenChance(15) == 1.0F / 6000.0F && BlockFoam.hardenChance(0) == 1.0F / 96000.0F, "harden odds");
        ctx.setBlockState(B, IC2ClassicBlocks.FOAM.getDefaultState());
        // the test server runs with random ticks off, which leaves foam soft for good
        var tickSpeed = ctx.getWorld().getGameRules().get(net.minecraft.world.GameRules.RANDOM_TICK_SPEED);
        int oldTickSpeed = tickSpeed.get();
        tickSpeed.set(3, ctx.getWorld().getServer());
        for (int i = 0; i < 20000 && ctx.getBlockState(B).isOf(IC2ClassicBlocks.FOAM); i++) {
            ctx.getBlockState(B).randomTick(ctx.getWorld(), ctx.getAbsolutePos(B), ctx.getWorld().getRandom());
        }
        tickSpeed.set(oldTickSpeed, ctx.getWorld().getServer());
        ctx.expectBlock(BlockWall.get(Ic2Color.light_gray), B);

        // the painter turns the wall red, and the red wall drops itself
        use(ctx, player, painter(Ic2Color.red), A);
        ctx.expectBlock(BlockWall.get(Ic2Color.red), A);
        ctx.assertTrue(Block.getDroppedStacks(ctx.getBlockState(A), ctx.getWorld(), ctx.getAbsolutePos(A), null, player,
                new ItemStack(Items.WOODEN_PICKAXE)).get(0).isOf(BlockWall.get(Ic2Color.red).asItem()), "red wall drop");
        ctx.complete();
    }
}
