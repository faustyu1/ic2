package ic2.core.gametest;

import ic2.api.item.ElectricItem;
import ic2.core.audio.MachineSound;
import ic2.core.block.BlockWall;
import ic2.core.block.TileEntityWall;
import ic2.core.block.comp.Obscuration;
import ic2.core.block.wiring.BlockCable;
import ic2.core.block.wiring.CableFoam;
import ic2.core.block.wiring.TileEntityCable;
import ic2.core.init.IC2Blocks;
import ic2.core.init.IC2ClassicBlocks;
import ic2.core.init.IC2ItemVariants;
import ic2.core.init.IC2Sounds;
import ic2.core.item.tool.ItemObscurator;
import ic2.core.item.tool.ItemSprayer;
import ic2.core.network.ObscuratorScanPayload;
import ic2.core.ref.FluidName;
import ic2.core.ref.TeBlock;
import ic2.core.util.Ic2Color;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

/**
 * Construction foam on cables, the obscurator, and the table of machine sounds.
 * Run with {@code ./gradlew runGametest}.
 */
public class FoamCableGameTest implements FabricGameTest {
    private static final BlockPos A = new BlockPos(2, 2, 2);

    @SuppressWarnings("removal")
    private static ServerPlayerEntity player(TestContext ctx) {
        ServerPlayerEntity player = ctx.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);
        return player;
    }

    private static ActionResult use(TestContext ctx, ServerPlayerEntity player, ItemStack stack, BlockPos rel, Direction side) {
        player.setStackInHand(Hand.MAIN_HAND, stack);
        BlockPos abs = ctx.getAbsolutePos(rel);
        BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(abs).add(Vec3d.of(side.getVector()).multiply(0.5)), side, abs, false);
        return player.getStackInHand(Hand.MAIN_HAND).useOnBlock(new ItemUsageContext(player, Hand.MAIN_HAND, hit));
    }

    private static void placeCable(TestContext ctx, String name, BlockPos rel) {
        BlockCable cable = (BlockCable) Registries.BLOCK.get(Identifier.of("ic2", name));
        ctx.setBlockState(rel, cable.withConnections(cable.getDefaultState(), ctx.getWorld(), ctx.getAbsolutePos(rel)));
    }

    private static TileEntityCable cable(TestContext ctx, BlockPos rel) {
        return (TileEntityCable) ctx.getBlockEntity(rel);
    }

    private static CableFoam foam(TestContext ctx, BlockPos rel) {
        return ctx.getBlockState(rel).get(BlockCable.FOAM);
    }

    private static ItemStack charged(ItemStack stack) {
        ElectricItem.manager.charge(stack, Double.POSITIVE_INFINITY, Integer.MAX_VALUE, true, false);
        return stack;
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void sprayerFoamsCablesAndSandSetsThem(TestContext ctx) {
        ServerPlayerEntity player = player(ctx);
        for (int x = 1; x <= 3; x++) {
            placeCable(ctx, "copper_cable_1", new BlockPos(x, 2, 2));
        }

        // the spray follows the cable instead of filling the air in front of it
        ItemSprayer sprayer = (ItemSprayer) Registries.ITEM.get(Identifier.of("ic2", "foam_sprayer"));
        ItemStack stack = sprayer.filledCopy(new ItemStack(sprayer), FluidName.construction_foam.getInstance());
        ctx.assertTrue(use(ctx, player, stack, new BlockPos(1, 2, 2), Direction.UP) == ActionResult.SUCCESS, "spray failed");
        for (int x = 1; x <= 3; x++) {
            ctx.assertTrue(foam(ctx, new BlockPos(x, 2, 2)) == CableFoam.soft, "cable " + x + " not foamed");
        }
        ctx.expectBlock(Blocks.AIR, new BlockPos(1, 3, 2));
        ctx.assertTrue(sprayer.getAmount(player.getStackInHand(Hand.MAIN_HAND)) == 8000 - 3 * 100, "foam used");

        // soft foam fills the block to aim at but not to stand on, and lets light through
        BlockState soft = ctx.getBlockState(A);
        BlockPos abs = ctx.getAbsolutePos(A);
        ctx.assertTrue(soft.getOutlineShape(ctx.getWorld(), abs).getBoundingBox().getAverageSideLength() == 1.0, "soft outline");
        ctx.assertTrue(soft.getCollisionShape(ctx.getWorld(), abs, ShapeContext.absent()).getBoundingBox().getAverageSideLength() < 1.0, "soft collision");
        ctx.assertTrue(soft.getOpacity(ctx.getWorld(), abs) == 0, "soft opacity");
        // sand sets it at once and is used up
        ItemStack sand = new ItemStack(Items.SAND, 2);
        player.setStackInHand(Hand.MAIN_HAND, sand);
        player.interactionManager.interactBlock(player, ctx.getWorld(), sand, Hand.MAIN_HAND,
                new BlockHitResult(Vec3d.ofCenter(abs), Direction.UP, abs, false));
        ctx.assertTrue(foam(ctx, A) == CableFoam.hardened, "sand did not set the foam");
        ctx.assertTrue(player.getStackInHand(Hand.MAIN_HAND).getCount() == 1, "sand not used");
        BlockState hard = ctx.getBlockState(A);
        ctx.assertTrue(hard.getCollisionShape(ctx.getWorld(), abs, ShapeContext.absent()).getBoundingBox().getAverageSideLength() == 1.0, "hardened collision");
        ctx.assertTrue(hard.getOpacity(ctx.getWorld(), abs) == 15, "hardened opacity " + hard.getOpacity(ctx.getWorld(), abs));
        // still a cable: the three stay joined and joined
        ctx.assertTrue(hard.get(net.minecraft.state.property.Properties.EAST) && hard.get(net.minecraft.state.property.Properties.WEST), "cable lost " + hard);
        // mined like the foam wall
        float wall = BlockWall.get(Ic2Color.light_gray).getDefaultState().calcBlockBreakingDelta(player, ctx.getWorld(), abs);
        ctx.assertTrue(hard.calcBlockBreakingDelta(player, ctx.getWorld(), abs) == wall, "hardness");
        ctx.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void softFoamOnACableSetsOnItsOwn(TestContext ctx) {
        placeCable(ctx, "tin_cable_0", A);
        ctx.assertTrue(cable(ctx, A).foam(), "not foamed");
        ctx.assertTrue(!cable(ctx, A).foam(), "foamed twice");
        // 1 / (300 * (16 - light) * 20) a tick: 6000 ticks on average next to a glowstone
        ctx.setBlockState(A.up(), Blocks.GLOWSTONE);
        ctx.waitAndRun(10, () -> {
            float chance = ic2.core.block.BlockFoam.getHardenChance(ctx.getWorld(), ctx.getAbsolutePos(A), ctx.getBlockState(A));
            ctx.assertTrue(chance == 1.0F / 6000.0F, "harden chance " + chance);
            for (int i = 0; i < 600_000 && foam(ctx, A) == CableFoam.soft; i++) {
                cable(ctx, A).tick();
            }
            ctx.assertTrue(foam(ctx, A) == CableFoam.hardened, "foam did not set");
            ctx.complete();
        });
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void breakingAFoamedCableOnlyBreaksTheFoam(TestContext ctx) {
        ServerPlayerEntity player = player(ctx);
        placeCable(ctx, "copper_cable_1", A);
        ItemStack foamBlocks = new ItemStack(IC2ClassicBlocks.FOAM, 2);
        player.setStackInHand(Hand.MAIN_HAND, foamBlocks);
        BlockPos abs = ctx.getAbsolutePos(A);
        player.interactionManager.interactBlock(player, ctx.getWorld(), foamBlocks, Hand.MAIN_HAND,
                new BlockHitResult(Vec3d.ofCenter(abs), Direction.UP, abs, false));
        ctx.assertTrue(foam(ctx, A) == CableFoam.soft && foamBlocks.getCount() == 1, "foam block not used on the cable");
        cable(ctx, A).changeFoam(CableFoam.hardened);

        player.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
        ctx.assertFalse(player.interactionManager.tryBreakBlock(abs), "the cable was broken");
        ctx.assertTrue(ctx.getBlockState(A).getBlock() instanceof BlockCable && foam(ctx, A) == CableFoam.none, "foam not removed");
        ctx.assertTrue(ctx.getWorld().getEntitiesByClass(net.minecraft.entity.ItemEntity.class,
                new net.minecraft.util.math.Box(abs).expand(2), e -> true).isEmpty(), "something dropped");
        // bare again, it breaks for real
        ctx.assertTrue(player.interactionManager.tryBreakBlock(abs), "bare cable did not break");
        ctx.expectBlock(Blocks.AIR, A);
        ctx.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void painterAndObscuratorDressTheFoamWall(TestContext ctx) {
        ServerPlayerEntity player = player(ctx);
        placeCable(ctx, "copper_cable_1", A);
        TileEntityCable cable = cable(ctx, A);
        cable.foam();
        // soft foam takes no paint
        ctx.assertFalse(cable.recolor(Ic2Color.red), "soft foam painted");
        cable.changeFoam(CableFoam.hardened);

        ItemStack obscurator = charged(new ItemStack(Registries.ITEM.get(Identifier.of("ic2", "obscurator"))));
        double full = ElectricItem.manager.getCharge(obscurator);
        Obscuration.ObscurationData stone = new Obscuration.ObscurationData(Blocks.STONE.getDefaultState(), Direction.UP, new int[] {-1});

        // the scan arrives from the client: 20000 EU and the face is stored in the item
        player.setStackInHand(Hand.MAIN_HAND, obscurator);
        ItemObscurator.receiveScan(player, new ObscuratorScanPayload(player.getInventory().selectedSlot,
                Block.getRawIdFromState(stone.state), Direction.UP.ordinal(), new int[] {-1}));
        ctx.assertTrue(stone.equals(ItemObscurator.getData(obscurator)), "scan not stored: " + ItemObscurator.getData(obscurator));
        ctx.assertTrue(ElectricItem.manager.getCharge(obscurator) == full - 20000, "scan cost");

        // printed on the north face of the foam wall for 5000 EU; the same face again is free
        ctx.assertTrue(use(ctx, player, obscurator, A, Direction.NORTH) == ActionResult.SUCCESS, "not printed");
        ctx.assertTrue(stone.equals(cable.getObscuration().getRenderState()[Direction.NORTH.ordinal()]), "face not stored");
        ctx.assertTrue(ElectricItem.manager.getCharge(obscurator) == full - 25000, "print cost");
        ctx.assertTrue(use(ctx, player, obscurator, A, Direction.NORTH) == ActionResult.PASS, "printed twice");
        ctx.assertTrue(cable.getRenderData() instanceof TileEntityWall.WallRenderState s && s.color() == Ic2Color.light_gray
                && s.obscurations() != null, "render data");

        // painting the wall wipes the faces
        ItemStack painter = new ItemStack(IC2ItemVariants.get("painter", Ic2Color.red.getName()));
        ctx.assertTrue(use(ctx, player, painter, A, Direction.UP).isAccepted(), "not painted");
        ctx.assertTrue(cable.getFoamColor() == Ic2Color.red && !cable.getObscuration().hasObscuration(), "paint");
        // the paint and faces survive a save, and are gone with the foam
        TileEntityCable copy = (TileEntityCable) net.minecraft.block.entity.BlockEntity.createFromNbt(cable.getPos(), cable.getCachedState(),
                cable.createNbtWithIdentifyingData(ctx.getWorld().getRegistryManager()), ctx.getWorld().getRegistryManager());
        ctx.assertTrue(copy != null && copy.getFoamColor() == Ic2Color.red, "foam colour not saved");
        cable.changeFoam(CableFoam.none);
        ctx.assertTrue(cable.getFoamColor() == Ic2Color.light_gray, "foam colour kept");

        // a bare cable takes no face
        ctx.assertTrue(use(ctx, player, obscurator, A, Direction.NORTH) == ActionResult.PASS, "printed on a bare cable");
        ctx.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void obscuratorTurnsAPlainWallIntoAnObscuredOne(TestContext ctx) {
        ServerPlayerEntity player = player(ctx);
        ctx.setBlockState(A, BlockWall.get(Ic2Color.blue));
        ItemStack obscurator = charged(new ItemStack(Registries.ITEM.get(Identifier.of("ic2", "obscurator"))));
        Obscuration.ObscurationData grass = new Obscuration.ObscurationData(Blocks.GRASS_BLOCK.getDefaultState(), Direction.UP, new int[] {0xFF91BD59});
        ItemObscurator.setData(obscurator, grass);

        ctx.assertTrue(use(ctx, player, obscurator, A, Direction.UP) == ActionResult.SUCCESS, "not printed");
        ctx.expectBlock(IC2Blocks.WALL_BLOCK, A);
        TileEntityWall wall = (TileEntityWall) ctx.getBlockEntity(A);
        ctx.assertTrue(wall.getColor() == Ic2Color.blue, "colour lost: " + wall.getColor());
        ctx.assertTrue(grass.equals(wall.getObscuration().getRenderState()[Direction.UP.ordinal()]), "face not stored");
        // it drops the plain wall of its colour, and the painter recolours it keeping the face
        ctx.assertTrue(wall.getSelfDrops(false).get(0).isOf(BlockWall.get(Ic2Color.blue).asItem()), "drop");
        use(ctx, player, new ItemStack(IC2ItemVariants.get("painter", Ic2Color.white.getName())), A, Direction.UP);
        ctx.assertTrue(wall.getColor() == Ic2Color.white && wall.getObscuration().hasObscuration(), "painted wall");

        // with nothing scanned it does nothing
        ItemStack empty = charged(new ItemStack(Registries.ITEM.get(Identifier.of("ic2", "obscurator"))));
        ctx.setBlockState(A.east(), BlockWall.get(Ic2Color.red));
        ctx.assertTrue(use(ctx, player, empty, A.east(), Direction.UP) == ActionResult.PASS, "printed nothing");
        ctx.expectBlock(BlockWall.get(Ic2Color.red), A.east());
        ctx.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void everyRunningMachineHasItsSound(TestContext ctx) {
        ctx.assertTrue(MachineSound.get(TeBlock.macerator).loop() == IC2Sounds.MACHINES_MACERATOR_OP
                && MachineSound.get(TeBlock.macerator).perOperation()
                && MachineSound.get(TeBlock.macerator).stop() == IC2Sounds.MACHINES_INTERRUPT_ONE, "macerator");
        ctx.assertTrue(MachineSound.get(TeBlock.induction_furnace).start() == IC2Sounds.MACHINES_INDUCTION_FURNACE_INDUCTION_START
                && MachineSound.get(TeBlock.induction_furnace).loop() == IC2Sounds.MACHINES_INDUCTION_FURNACE_INDUCTION_LOOP
                && MachineSound.get(TeBlock.induction_furnace).stop() == IC2Sounds.MACHINES_INDUCTION_FURNACE_INDUCTION_STOP, "induction furnace");
        ctx.assertTrue(MachineSound.get(TeBlock.semifluid_generator).loop() == IC2Sounds.GENERATORS_GEOTHERMAL_LOOP, "semifluid generator");
        ctx.assertTrue(MachineSound.get(TeBlock.mass_fabricator).extraPeriod() == 41, "scrap period");
        ctx.assertTrue(MachineSound.get(TeBlock.canner) == null && MachineSound.get(TeBlock.batbox) == null, "silent blocks");
        TeBlock[] noisy = {TeBlock.generator, TeBlock.geo_generator, TeBlock.semifluid_generator, TeBlock.water_generator, TeBlock.wind_generator,
                TeBlock.electric_furnace, TeBlock.induction_furnace, TeBlock.macerator, TeBlock.extractor, TeBlock.compressor,
                TeBlock.recycler, TeBlock.iron_furnace, TeBlock.miner, TeBlock.pump, TeBlock.terraformer, TeBlock.teleporter,
                TeBlock.mass_fabricator, TeBlock.matter_generator, TeBlock.nuclear_reactor};
        for (TeBlock block : noisy) {
            MachineSound sound = MachineSound.get(block);
            ctx.assertTrue(sound != null && Registries.SOUND_EVENT.getId(sound.loop()) != null, "no sound for " + block);
            // the machine has the active state the client starts the sound from
            ctx.assertTrue(IC2Blocks.getBlock(block) != null && IC2Blocks.getBlock(block).getDefaultState().contains(ic2.core.block.BlockTileEntity.ACTIVE), block + " has no active state");
        }
        ctx.complete();
    }
}
