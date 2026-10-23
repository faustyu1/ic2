package ic2.core.gametest;

import ic2.api.item.ElectricItem;
import ic2.core.ExplosionIC2;
import ic2.core.IC2;
import ic2.core.block.BlockDynamite;
import ic2.core.block.EntityDynamite;
import ic2.core.block.EntityItnt;
import ic2.core.block.EntityNuke;
import ic2.core.block.EntityStickyDynamite;
import ic2.core.block.machine.tileentity.TileEntityNuke;
import ic2.core.init.IC2Blocks;
import ic2.core.init.IC2ClassicBlocks;
import ic2.core.init.IC2Entities;
import ic2.core.init.IC2ItemVariants;
import ic2.core.init.IC2Items;
import ic2.core.item.tool.EntityMiningLaser;
import ic2.core.item.tool.ItemMiningLaser;
import ic2.core.util.Keyboard;
import java.util.EnumSet;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

/**
 * The mining laser, dynamite, the Dynamite-O-Mote, industrial TNT, the nuke and the IC2 explosion
 * itself, in a running world. Every test that sets something off runs in a batch of its own, so the
 * blast cannot reach the neighbouring tests.
 * Run with {@code ./gradlew runGametest}.
 */
public class ExplosiveGameTest implements FabricGameTest {

    /** A survival player standing in the test area, looking south. */
    @SuppressWarnings("removal")
    private static ServerPlayerEntity player(TestContext ctx) {
        ServerPlayerEntity player = ctx.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);
        player.refreshPositionAndAngles(ctx.getAbsolute(new Vec3d(1.5, 1.0, 1.5)), 0.0F, 0.0F);
        return player;
    }

    private static ItemStack chargedLaser() {
        ItemStack laser = new ItemStack(IC2Items.MINING_LASER);
        ElectricItem.manager.charge(laser, Double.POSITIVE_INFINITY, Integer.MAX_VALUE, true, false);
        return laser;
    }

    /** Fires a shot of the given mode's numbers from the relative position along the direction. */
    private static void shoot(TestContext ctx, ServerPlayerEntity owner, Vec3d relStart, Vec3d dir, float range, float power,
                              boolean explosive, boolean smelt) {
        ItemMiningLaser laser = (ItemMiningLaser) IC2Items.MINING_LASER;
        laser.shootLaser(chargedLaser(), ctx.getWorld(), ctx.getAbsolute(relStart), dir, owner, range, power, Integer.MAX_VALUE,
                explosive, smelt);
    }

    private static ActionResult useOn(TestContext ctx, ServerPlayerEntity player, ItemStack stack, BlockPos rel, Direction side) {
        player.setStackInHand(Hand.MAIN_HAND, stack);
        BlockPos abs = ctx.getAbsolutePos(rel);
        BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(abs).add(Vec3d.of(side.getVector()).multiply(0.5)), side, abs, false);
        return player.getStackInHand(Hand.MAIN_HAND).useOnBlock(new ItemUsageContext(player, Hand.MAIN_HAND, hit));
    }

    private static <T extends Entity> void discardAll(TestContext ctx, net.minecraft.entity.EntityType<T> type) {
        ctx.getEntities(type).forEach(Entity::discard);
    }

    // ---------------------------------------------------------------- mining laser

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void laserBreaksStoneShinesThroughGlassStopsAtObsidian(TestContext ctx) {
        ServerPlayerEntity player = player(ctx);
        // a floor, so what the stone drops stays in the test area
        for (int x = 1; x <= 7; x++) {
            ctx.setBlockState(new BlockPos(x, 1, 2), Blocks.SMOOTH_STONE);
        }
        ctx.setBlockState(new BlockPos(3, 2, 2), Blocks.STONE);
        ctx.setBlockState(new BlockPos(4, 2, 2), Blocks.STONE);
        ctx.setBlockState(new BlockPos(5, 2, 2), Blocks.GLASS);
        ctx.setBlockState(new BlockPos(6, 2, 2), Blocks.STONE);
        ctx.setBlockState(new BlockPos(7, 2, 1), Blocks.OBSIDIAN);
        ctx.setBlockState(new BlockPos(7, 2, 2), Blocks.OBSIDIAN);

        // the mining mode: unlimited range, power 5
        shoot(ctx, player, new Vec3d(1.5, 2.5, 2.5), new Vec3d(1, 0, 0), Float.POSITIVE_INFINITY, 5.0F, false, false);

        ctx.runAtTick(20, () -> {
            ctx.expectBlock(Blocks.AIR, new BlockPos(3, 2, 2));
            ctx.expectBlock(Blocks.AIR, new BlockPos(4, 2, 2));
            ctx.expectBlock(Blocks.GLASS, new BlockPos(5, 2, 2));
            ctx.expectBlock(Blocks.AIR, new BlockPos(6, 2, 2));
            ctx.expectBlock(Blocks.OBSIDIAN, new BlockPos(7, 2, 2));
            ctx.assertTrue(ctx.getEntities(IC2Entities.MINING_LASER).isEmpty(), "shot still flying after running out of power");
            ctx.assertTrue(!ctx.getWorld().getEntitiesByClass(ItemEntity.class, ctx.getTestBox(),
                    e -> e.getStack().isOf(Items.COBBLESTONE)).isEmpty(), "broken stone dropped nothing");
            ctx.complete();
        });
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void lowFocusShotBreaksOneBlockWithinFour(TestContext ctx) {
        ServerPlayerEntity player = player(ctx);
        ctx.setBlockState(new BlockPos(3, 2, 2), Blocks.DIRT);
        ctx.setBlockState(new BlockPos(4, 2, 2), Blocks.DIRT);

        ItemStack laser = chargedLaser();
        ItemMiningLaser item = (ItemMiningLaser) IC2Items.MINING_LASER;
        item.shootLaser(laser, ctx.getWorld(), ctx.getAbsolute(new Vec3d(1.5, 2.5, 2.5)), new Vec3d(1, 0, 0), player,
                4.0F, 5.0F, 1, false, false);

        ctx.runAtTick(10, () -> {
            ctx.expectBlock(Blocks.AIR, new BlockPos(3, 2, 2));
            ctx.expectBlock(Blocks.DIRT, new BlockPos(4, 2, 2));
            ctx.complete();
        });
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void laserSwitchesModesAndSpendsTheirEnergy(TestContext ctx) {
        ServerPlayerEntity player = player(ctx);
        ItemStack laser = chargedLaser();
        player.setStackInHand(Hand.MAIN_HAND, laser);
        double full = ElectricItem.manager.getCharge(laser);

        // the mode key and right click go to the next mode and fire nothing
        IC2.keyboard.processKeyUpdate(player, Keyboard.Key.toInt(EnumSet.of(Keyboard.Key.modeSwitch)));
        laser.use(ctx.getWorld(), player, Hand.MAIN_HAND);
        IC2.keyboard.processKeyUpdate(player, 0);
        ctx.assertTrue(ItemMiningLaser.getMode(laser) == ItemMiningLaser.MODE_LOW_FOCUS, "mode did not switch to low-focus");
        ctx.assertTrue(ElectricItem.manager.getCharge(laser) == full, "switching the mode cost energy");
        ctx.assertTrue(ctx.getEntities(IC2Entities.MINING_LASER).isEmpty(), "switching the mode fired");

        // a low-focus shot is 100 EU
        laser.use(ctx.getWorld(), player, Hand.MAIN_HAND);
        ctx.assertTrue(ElectricItem.manager.getCharge(laser) == full - 100, "low-focus shot did not cost 100 EU");
        ctx.assertTrue(ctx.getEntities(IC2Entities.MINING_LASER).size() == 1, "no shot fired");
        discardAll(ctx, IC2Entities.MINING_LASER);

        // the scatter mode fires 25 shots for 10000 EU
        ItemMiningLaser.setMode(laser, ItemMiningLaser.MODE_SCATTER);
        laser.use(ctx.getWorld(), player, Hand.MAIN_HAND);
        ctx.assertTrue(ElectricItem.manager.getCharge(laser) == full - 10100, "scatter did not cost 10000 EU");
        ctx.assertTrue(ctx.getEntities(IC2Entities.MINING_LASER).size() == 25, "scatter did not fire 25 shots");
        discardAll(ctx, IC2Entities.MINING_LASER);

        // without the energy nothing is fired
        ItemStack empty = new ItemStack(IC2Items.MINING_LASER);
        player.setStackInHand(Hand.MAIN_HAND, empty);
        empty.use(ctx.getWorld(), player, Hand.MAIN_HAND);
        ctx.assertTrue(ctx.getEntities(IC2Entities.MINING_LASER).isEmpty(), "an empty laser fired");
        ctx.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void horizontalModeFiresLevelWithTheBlockAimedAt(TestContext ctx) {
        ServerPlayerEntity player = player(ctx);
        ItemStack laser = chargedLaser();
        ItemMiningLaser.setMode(laser, ItemMiningLaser.MODE_3X3);
        // looking along +Z, level
        player.refreshPositionAndAngles(ctx.getAbsolute(new Vec3d(4.5, 2.0, 0.5)), 0.0F, 0.0F);
        for (int x = 3; x <= 5; x++) {
            for (int y = 1; y <= 3; y++) {
                ctx.setBlockState(new BlockPos(x, y, 3), Blocks.DIRT);
                // the shots end here instead of flying on into the next test
                ctx.setBlockState(new BlockPos(x, y, 5), Blocks.OBSIDIAN);
            }
        }

        double full = ElectricItem.manager.getCharge(laser);
        ActionResult result = useOn(ctx, player, laser, new BlockPos(4, 2, 3), Direction.NORTH);
        ctx.assertTrue(result.isAccepted(), "3x3 mode did not take the click on the block");
        ctx.assertTrue(ElectricItem.manager.getCharge(laser) == full - 3000, "3x3 shot did not cost 3000 EU");
        ctx.assertTrue(ctx.getEntities(IC2Entities.MINING_LASER).size() == 9, "3x3 did not fire nine shots");

        ctx.runAtTick(15, () -> {
            for (int x = 3; x <= 5; x++) {
                for (int y = 1; y <= 3; y++) {
                    ctx.expectBlock(Blocks.AIR, new BlockPos(x, y, 3));
                }
            }
            discardAll(ctx, IC2Entities.MINING_LASER);
            ctx.complete();
        });
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void superHeatShotSmeltsWhatItBreaks(TestContext ctx) {
        ServerPlayerEntity player = player(ctx);
        ctx.setBlockState(new BlockPos(2, 1, 4), Blocks.STONE);
        ctx.setBlockState(new BlockPos(2, 2, 4), Blocks.SAND);
        ctx.setBlockState(new BlockPos(5, 2, 4), Blocks.IRON_ORE);
        ctx.setBlockState(new BlockPos(5, 1, 4), Blocks.SMOOTH_STONE);
        ctx.setBlockState(new BlockPos(7, 1, 1), Blocks.SMOOTH_STONE);
        ctx.setBlockState(new BlockPos(7, 2, 1), Blocks.OAK_LOG);
        ctx.setBlockState(new BlockPos(7, 2, 3), Blocks.OBSIDIAN);

        shoot(ctx, player, new Vec3d(2.5, 2.5, 1.5), new Vec3d(0, 0, 1), Float.POSITIVE_INFINITY, 8.0F, false, true);
        shoot(ctx, player, new Vec3d(5.5, 2.5, 2.5), new Vec3d(0, 0, 1), Float.POSITIVE_INFINITY, 8.0F, false, true);
        shoot(ctx, player, new Vec3d(7.5, 2.5, 0.2), new Vec3d(0, 0, 1), Float.POSITIVE_INFINITY, 8.0F, false, true);

        ctx.runAtTick(10, () -> {
            // sand smelts into glass, which goes back in place of the sand
            ctx.expectBlock(Blocks.GLASS, new BlockPos(2, 2, 4));
            // iron ore drops raw iron, which smelts into an ingot - not a block, so it drops
            ctx.expectBlock(Blocks.AIR, new BlockPos(5, 2, 4));
            ctx.assertTrue(!ctx.getWorld().getEntitiesByClass(ItemEntity.class, ctx.getTestBox(),
                    e -> e.getStack().isOf(Items.IRON_INGOT)).isEmpty(), "smelted ore gave no ingot");
            ctx.assertTrue(ctx.getWorld().getEntitiesByClass(ItemEntity.class, ctx.getTestBox(),
                    e -> e.getStack().isOf(Items.RAW_IRON)).isEmpty(), "ore dropped raw iron as well");
            // wood burns away, and one time in ten sets the place on fire
            ctx.assertTrue(ctx.getBlockState(new BlockPos(7, 2, 1)).isAir() || ctx.getBlockState(new BlockPos(7, 2, 1)).isOf(Blocks.FIRE),
                    "wood was not burnt: " + ctx.getBlockState(new BlockPos(7, 2, 1)));
            ctx.assertTrue(ctx.getWorld().getEntitiesByClass(ItemEntity.class, ctx.getTestBox(),
                    e -> e.getStack().isOf(Items.OAK_LOG)).isEmpty(), "burnt wood dropped a log");
            discardAll(ctx, IC2Entities.MINING_LASER);
            ctx.complete();
        });
    }

    @GameTest(templateName = EMPTY_STRUCTURE, batchId = "explosive_laser")
    public void explosiveShotBlowsUpOnFirstHit(TestContext ctx) {
        ServerPlayerEntity player = player(ctx);
        ctx.setBlockState(new BlockPos(4, 2, 6), Blocks.STONE);
        ctx.setBlockState(new BlockPos(4, 3, 6), Blocks.STONE);

        shoot(ctx, player, new Vec3d(4.5, 2.5, 1.5), new Vec3d(0, 0, 1), Float.POSITIVE_INFINITY, 12.0F, true, false);

        ctx.runAtTick(10, () -> {
            ctx.expectBlock(Blocks.AIR, new BlockPos(4, 2, 6));
            ctx.expectBlock(Blocks.AIR, new BlockPos(4, 3, 6));
            ctx.assertTrue(ctx.getEntities(IC2Entities.MINING_LASER).isEmpty(), "explosive shot did not end");
            ctx.complete();
        });
    }

    // ---------------------------------------------------------------- the IC2 explosion

    @GameTest(templateName = EMPTY_STRUCTURE, batchId = "explosion_ic2")
    public void ic2ExplosionBreaksStopsAtBedrockAndLightsTnt(TestContext ctx) {
        BlockPos center = new BlockPos(4, 3, 4);
        for (Direction dir : Direction.values()) {
            if (dir != Direction.NORTH) {
                ctx.setBlockState(center.offset(dir), Blocks.DIRT);
            }
        }
        ctx.setBlockState(center.north(), IC2Blocks.ITNT_BLOCK);
        ctx.setBlockState(center.east(2), Blocks.BEDROCK);

        BlockPos abs = ctx.getAbsolutePos(center);
        new ExplosionIC2(ctx.getWorld(), null, abs.getX() + 0.5, abs.getY() + 0.5, abs.getZ() + 0.5, 3.0F, 1.0F).doExplosion();

        for (Direction dir : Direction.values()) {
            ctx.expectBlock(Blocks.AIR, center.offset(dir));
        }
        ctx.expectBlock(Blocks.BEDROCK, center.east(2));

        // the drops come in stacks rather than one item entity a block
        List<ItemEntity> dirt = ctx.getWorld().getEntitiesByClass(ItemEntity.class, ctx.getTestBox(), e -> e.getStack().isOf(Items.DIRT));
        int count = dirt.stream().mapToInt(e -> e.getStack().getCount()).sum();
        ctx.assertTrue(count >= 5, "drop rate 1 lost dirt: " + count);
        ctx.assertTrue(dirt.size() < count, "drops were not gathered");

        // the industrial TNT caught in it is lit with a short fuse
        List<EntityItnt> lit = ctx.getEntities(IC2Entities.ITNT);
        ctx.assertTrue(lit.size() == 1, "industrial TNT in the blast was not lit");
        ctx.assertTrue(lit.get(0).getFuse() < 60 * 3 / 8, "fuse not short: " + lit.get(0).getFuse());
        discardAll(ctx, IC2Entities.ITNT);
        ctx.complete();
    }

    // ---------------------------------------------------------------- dynamite

    @GameTest(templateName = EMPTY_STRUCTURE, batchId = "dynamite_point")
    public void dynamiteBreaksTheCubeAroundIt(TestContext ctx) {
        BlockPos center = new BlockPos(4, 3, 4);
        for (BlockPos pos : BlockPos.iterate(center.add(-1, -1, -1), center.add(1, 1, 1))) {
            ctx.setBlockState(pos, Blocks.DIRT);
        }
        ctx.setBlockState(center, Blocks.AIR);
        ctx.setBlockState(center.add(1, 1, 1), Blocks.OBSIDIAN);

        EntityDynamite dynamite = new EntityDynamite(ctx.getWorld(), ctx.getAbsolute(Vec3d.ofCenter(center)).x,
                ctx.getAbsolute(Vec3d.ofCenter(center)).y, ctx.getAbsolute(Vec3d.ofCenter(center)).z);
        dynamite.fuse = 2;
        dynamite.setVelocity(Vec3d.ZERO);
        ctx.getWorld().spawnEntity(dynamite);

        ctx.runAtTick(6, () -> {
            ctx.expectBlock(Blocks.AIR, center.add(-1, -1, -1));
            ctx.expectBlock(Blocks.AIR, center.add(1, 0, 0));
            ctx.expectBlock(Blocks.OBSIDIAN, center.add(1, 1, 1));
            ctx.assertTrue(ctx.getEntities(IC2Entities.DYNAMITE).isEmpty(), "dynamite still there");
            ctx.complete();
        });
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void stickyDynamiteStaysWhereItLands(TestContext ctx) {
        ctx.setBlockState(new BlockPos(5, 2, 2), Blocks.STONE);
        Vec3d start = ctx.getAbsolute(new Vec3d(2.5, 2.5, 2.5));
        EntityStickyDynamite sticky = new EntityStickyDynamite(ctx.getWorld(), start.x, start.y, start.z);
        sticky.fuse = 10000;
        sticky.setVelocity(0.6, 0.0, 0.0, 0.6F, 0.0F);
        ctx.getWorld().spawnEntity(sticky);

        ctx.runAtTick(20, () -> {
            Vec3d rel = sticky.getPos().subtract(Vec3d.of(ctx.getAbsolutePos(BlockPos.ORIGIN)));
            ctx.assertTrue(sticky.isAlive(), "sticky dynamite gone");
            ctx.assertTrue(sticky.getVelocity().lengthSquared() < 1.0E-6, "sticky dynamite still moving");
            ctx.assertTrue(rel.x > 4.5 && rel.x <= 5.0, "sticky dynamite not at the wall: " + rel);
            ctx.assertTrue(rel.y > 1.8, "sticky dynamite fell: " + rel);
            sticky.discard();
            ctx.complete();
        });
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void throwingDynamiteUsesUpOneStick(TestContext ctx) {
        ServerPlayerEntity player = player(ctx);
        ItemStack sticks = new ItemStack(IC2Items.DYNAMITE, 3);
        player.setStackInHand(Hand.MAIN_HAND, sticks);
        sticks.use(ctx.getWorld(), player, Hand.MAIN_HAND);
        ctx.assertTrue(sticks.getCount() == 2, "throwing did not use a stick");
        List<EntityDynamite> thrown = ctx.getEntities(IC2Entities.DYNAMITE);
        ctx.assertTrue(thrown.size() == 1 && thrown.get(0).owner == player, "no dynamite thrown by the player");
        ctx.assertTrue(thrown.get(0).getVelocity().length() > 0.9, "thrown dynamite not flying");

        ItemStack sticky = new ItemStack(IC2Items.DYNAMITE_STICKY);
        player.setStackInHand(Hand.MAIN_HAND, sticky);
        sticky.use(ctx.getWorld(), player, Hand.MAIN_HAND);
        ctx.assertTrue(ctx.getEntities(IC2Entities.STICKY_DYNAMITE).size() == 1, "no sticky dynamite thrown");
        discardAll(ctx, IC2Entities.DYNAMITE);
        discardAll(ctx, IC2Entities.STICKY_DYNAMITE);
        ctx.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void dispenserThrowsDynamite(TestContext ctx) {
        BlockPos pos = new BlockPos(2, 2, 2);
        ctx.setBlockState(pos, Blocks.DISPENSER.getDefaultState().with(DispenserBlock.FACING, Direction.EAST));
        DispenserBlockEntity dispenser = (DispenserBlockEntity) ctx.getBlockEntity(pos);
        dispenser.setStack(0, new ItemStack(IC2Items.DYNAMITE, 2));
        ctx.setBlockState(pos.west(), Blocks.REDSTONE_BLOCK);

        ctx.runAtTick(8, () -> {
            List<EntityDynamite> thrown = ctx.getEntities(IC2Entities.DYNAMITE);
            ctx.assertTrue(thrown.size() == 1, "dispenser did not throw dynamite");
            ctx.assertTrue(thrown.get(0).getX() > ctx.getAbsolute(new Vec3d(4.0, 0, 0)).x, "dynamite not thrown east");
            ctx.assertTrue(dispenser.getStack(0).getCount() == 1, "dispenser did not use a stick");
            discardAll(ctx, IC2Entities.DYNAMITE);
            ctx.complete();
        });
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void dynamiteIsPlacedLikeATorchAndFallsWithoutSupport(TestContext ctx) {
        ServerPlayerEntity player = player(ctx);
        BlockPos floor = new BlockPos(2, 1, 2);
        BlockPos wall = new BlockPos(5, 2, 2);
        ctx.setBlockState(floor, Blocks.STONE);
        ctx.setBlockState(wall, Blocks.STONE);

        useOn(ctx, player, new ItemStack(IC2Items.DYNAMITE, 2), floor, Direction.UP);
        BlockState standing = ctx.getBlockState(floor.up());
        ctx.assertTrue(standing.isOf(IC2ClassicBlocks.DYNAMITE) && standing.get(BlockDynamite.FACING) == Direction.UP,
                "dynamite not standing on the floor");
        ctx.assertTrue(player.getStackInHand(Hand.MAIN_HAND).getCount() == 1, "placing did not use a stick");

        useOn(ctx, player, player.getStackInHand(Hand.MAIN_HAND), wall, Direction.WEST);
        BlockState hanging = ctx.getBlockState(wall.west());
        ctx.assertTrue(hanging.isOf(IC2ClassicBlocks.DYNAMITE) && hanging.get(BlockDynamite.FACING) == Direction.WEST,
                "dynamite not hanging on the wall");

        // sticky dynamite is not placed
        ctx.assertFalse(useOn(ctx, player, new ItemStack(IC2Items.DYNAMITE_STICKY), floor, Direction.NORTH).isAccepted(),
                "sticky dynamite was placed");

        // without the wall the stick is gone, and nothing is lit
        ctx.setBlockState(wall, Blocks.AIR);
        ctx.expectBlock(Blocks.AIR, wall.west());
        ctx.assertTrue(ctx.getEntities(IC2Entities.STICKY_DYNAMITE).isEmpty(), "fallen dynamite was lit");
        ctx.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE, batchId = "dynamite_redstone")
    public void placedDynamiteGoesOffOnRedstone(TestContext ctx) {
        ServerPlayerEntity player = player(ctx);
        BlockPos floor = new BlockPos(4, 1, 4);
        ctx.setBlockState(floor, Blocks.STONE);
        useOn(ctx, player, new ItemStack(IC2Items.DYNAMITE), floor, Direction.UP);
        ctx.setBlockState(floor.up().east(), Blocks.REDSTONE_BLOCK);

        ctx.expectBlock(Blocks.AIR, floor.up());
        List<EntityStickyDynamite> lit = ctx.getEntities(IC2Entities.STICKY_DYNAMITE);
        ctx.assertTrue(lit.size() == 1 && lit.get(0).fuse == 40, "redstone did not light the dynamite with a 40 tick fuse");

        // sticky dynamite burns four ticks a tick on the ground
        ctx.runAtTick(20, () -> {
            ctx.assertTrue(ctx.getEntities(IC2Entities.STICKY_DYNAMITE).isEmpty(), "lit dynamite did not go off");
            ctx.expectBlock(Blocks.AIR, floor);
            ctx.complete();
        });
    }

    @GameTest(templateName = EMPTY_STRUCTURE, batchId = "dynamite_remote")
    public void remoteLinksUnlinksAndSetsOffDynamite(TestContext ctx) {
        ServerPlayerEntity player = player(ctx);
        BlockPos floor = new BlockPos(4, 1, 4);
        BlockPos stick = floor.up();
        ctx.setBlockState(floor, Blocks.STONE);
        useOn(ctx, player, new ItemStack(IC2Items.DYNAMITE), floor, Direction.UP);

        ItemStack remote = new ItemStack(net.minecraft.registry.Registries.ITEM.get(net.minecraft.util.Identifier.of("ic2", "remote")));
        useOn(ctx, player, remote, stick, Direction.UP);
        ctx.assertTrue(ctx.getBlockState(stick).get(BlockDynamite.LINKED), "remote did not link");
        useOn(ctx, player, remote, stick, Direction.UP);
        ctx.assertFalse(ctx.getBlockState(stick).get(BlockDynamite.LINKED), "remote did not unlink");

        // another remote cannot unlink what this one linked
        useOn(ctx, player, remote, stick, Direction.UP);
        ItemStack other = remote.getItem().getDefaultStack();
        useOn(ctx, player, other, stick, Direction.UP);
        ctx.assertTrue(ctx.getBlockState(stick).get(BlockDynamite.LINKED), "a foreign remote unlinked");

        player.setStackInHand(Hand.MAIN_HAND, remote);
        remote.use(ctx.getWorld(), player, Hand.MAIN_HAND);
        ctx.expectBlock(Blocks.AIR, stick);
        List<EntityStickyDynamite> lit = ctx.getEntities(IC2Entities.STICKY_DYNAMITE);
        ctx.assertTrue(lit.size() == 1 && lit.get(0).owner == player, "remote did not set the dynamite off");
        discardAll(ctx, IC2Entities.STICKY_DYNAMITE);
        ctx.complete();
    }

    // ---------------------------------------------------------------- industrial TNT and the nuke

    @GameTest(templateName = EMPTY_STRUCTURE, batchId = "itnt", tickLimit = 200)
    public void industrialTntGoesOffOnRedstone(TestContext ctx) {
        BlockPos pos = new BlockPos(4, 3, 4);
        ctx.setBlockState(pos, IC2Blocks.ITNT_BLOCK);
        ctx.setBlockState(new BlockPos(4, 3, 6), Blocks.STONE);
        ctx.setBlockState(pos.west(), Blocks.REDSTONE_BLOCK);

        ctx.expectBlock(Blocks.AIR, pos);
        ctx.assertTrue(ctx.getEntities(IC2Entities.ITNT).size() == 1, "redstone did not light the industrial TNT");

        ctx.runAtTick(70, () -> {
            ctx.assertTrue(ctx.getEntities(IC2Entities.ITNT).isEmpty(), "industrial TNT did not go off");
            ctx.expectBlock(Blocks.AIR, new BlockPos(4, 3, 6));
            ctx.complete();
        });
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void industrialTntIsLitByFlintAndByBreaking(TestContext ctx) {
        ServerPlayerEntity player = player(ctx);
        BlockPos a = new BlockPos(2, 2, 2);
        BlockPos b = new BlockPos(5, 2, 2);
        ctx.setBlockState(a, IC2Blocks.ITNT_BLOCK);
        ctx.setBlockState(b, IC2Blocks.ITNT_BLOCK);

        ItemStack flint = new ItemStack(Items.FLINT_AND_STEEL);
        player.setStackInHand(Hand.MAIN_HAND, flint);
        BlockPos absA = ctx.getAbsolutePos(a);
        player.interactionManager.interactBlock(player, ctx.getWorld(), flint, Hand.MAIN_HAND,
                new BlockHitResult(Vec3d.ofCenter(absA), Direction.UP, absA, false));
        ctx.expectBlock(Blocks.AIR, a);
        ctx.assertTrue(flint.getDamage() == 1, "flint and steel not worn");

        // breaking lights it too, and drops nothing
        BlockPos absB = ctx.getAbsolutePos(b);
        BlockState state = ctx.getWorld().getBlockState(absB);
        state.getBlock().onBreak(ctx.getWorld(), absB, state, player);
        ctx.expectBlock(Blocks.AIR, b);
        ctx.assertTrue(ctx.getEntities(IC2Entities.ITNT).size() == 2, "industrial TNT not lit twice");
        ctx.assertTrue(ctx.getWorld().getEntitiesByClass(ItemEntity.class, ctx.getTestBox(), e -> true).isEmpty(),
                "lit industrial TNT dropped an item");
        discardAll(ctx, IC2Entities.ITNT);
        ctx.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void nukeNeedsTntAndAWrenchDefusesIt(TestContext ctx) {
        ServerPlayerEntity player = player(ctx);
        BlockPos pos = new BlockPos(3, 2, 3);
        ctx.setBlockState(pos, IC2Blocks.NUKE_BLOCK);
        TileEntityNuke nuke = (TileEntityNuke) ctx.getBlockEntity(pos);

        // without industrial TNT around the core, nothing happens
        ctx.setBlockState(pos.west(), Blocks.REDSTONE_BLOCK);
        ctx.expectBlock(IC2Blocks.NUKE_BLOCK, pos);
        ctx.assertTrue(ctx.getEntities(IC2Entities.NUKE).isEmpty(), "a nuke without TNT was lit");
        ctx.setBlockState(pos.west(), Blocks.AIR);

        // power 5 * cbrt(8) = 10; uranium 238 gives a radiation range of one per TNT
        nuke.outsideSlot.put(new ItemStack(IC2Blocks.ITNT_BLOCK, 8));
        nuke.insideSlot.put(new ItemStack(IC2ItemVariants.get("nuclear", "uranium_238"), 4));
        ctx.assertTrue(Math.abs(nuke.getNukeExplosivePower() - 10.0F) < 1.0E-4F, "power " + nuke.getNukeExplosivePower());
        ctx.assertTrue(nuke.getRadiationRange() == 8, "radiation range " + nuke.getRadiationRange());

        ctx.setBlockState(pos.west(), Blocks.REDSTONE_BLOCK);
        ctx.expectBlock(Blocks.AIR, pos);
        List<EntityNuke> lit = ctx.getEntities(IC2Entities.NUKE);
        ctx.assertTrue(lit.size() == 1, "nuke not lit");
        ctx.assertTrue(lit.get(0).getFuse() == 300 && lit.get(0).radiationRange == 8, "lit nuke has the wrong fuse or range");
        // the contents went into the blast, not onto the ground
        ctx.assertTrue(ctx.getWorld().getEntitiesByClass(ItemEntity.class, ctx.getTestBox(), e -> true).isEmpty(),
                "lit nuke spilled its contents");

        ItemStack wrench = new ItemStack(IC2Items.WRENCH);
        player.setStackInHand(Hand.MAIN_HAND, wrench);
        lit.get(0).interact(player, Hand.MAIN_HAND);
        ctx.assertTrue(ctx.getEntities(IC2Entities.NUKE).isEmpty(), "the wrench did not defuse the nuke");
        ctx.assertTrue(wrench.getDamage() == 1, "defusing did not wear the wrench");
        ctx.complete();
    }
}
