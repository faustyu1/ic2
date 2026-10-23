package ic2.core.gametest;

import ic2.api.item.ITerraformingBP;
import ic2.core.ChunkLoaderLogic;
import ic2.core.block.machine.tileentity.TileEntityChunkloader;
import ic2.core.block.machine.tileentity.TileEntityTeleporter;
import ic2.core.block.machine.tileentity.TileEntityTerraformer;
import ic2.core.block.wiring.TileEntityElectricBlock;
import ic2.core.init.IC2Blocks;
import ic2.core.init.IC2ItemVariants;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.SnowBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;

/** Teleporter, terraformer and chunk loader in a running world. Run with {@code ./gradlew runGametest}. */
public class TeleporterGameTest implements FabricGameTest {

    /**
     * The transmitter links two teleporters four blocks apart; powered, the first sends a pig to
     * the second for 100 * 14^0.7 * 5 = 3171 EU taken from the batbox beside it.
     */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void teleporterSendsAPig(TestContext ctx) {
        BlockPos from = new BlockPos(1, 2, 1);
        BlockPos to = new BlockPos(5, 2, 1);
        ctx.setBlockState(from, IC2Blocks.TELEPORTER_BLOCK.getDefaultState());
        ctx.setBlockState(to, IC2Blocks.TELEPORTER_BLOCK.getDefaultState());
        ctx.setBlockState(from.north(), IC2Blocks.BATBOX_BLOCK.getDefaultState());
        TileEntityTeleporter source = ctx.getBlockEntity(from);
        TileEntityTeleporter target = ctx.getBlockEntity(to);
        TileEntityElectricBlock batbox = ctx.getBlockEntity(from.north());
        batbox.injectEnergy(Direction.UP, 10000, 32);

        ItemStack transmitter = new ItemStack(Registries.ITEM.get(Identifier.of("ic2", "frequency_transmitter")));
        var player = ctx.createMockPlayer(net.minecraft.world.GameMode.SURVIVAL);
        player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, transmitter);
        // the context clicks the block next to the given one, in the given direction
        ctx.useStackOnBlock(player, transmitter, from.down(), Direction.UP);
        ctx.assertTrue(!source.hasTarget(), "linked after the first click");
        ctx.useStackOnBlock(player, transmitter, to.down(), Direction.UP);
        ctx.assertTrue(ctx.getAbsolutePos(to).equals(source.getTarget()), "source target " + source.getTarget());
        ctx.assertTrue(ctx.getAbsolutePos(from).equals(target.getTarget()), "link is not both ways");
        ctx.assertTrue(source.getComparatorOutput() == 15, "comparator of a linked teleporter");

        PigEntity pig = ctx.spawnEntity(EntityType.PIG, from.up());
        ctx.assertTrue(source.getWeightOf(pig) == 100, "pig weight " + source.getWeightOf(pig));

        source.tick();
        ctx.assertTrue(pig.getBlockPos().getX() == ctx.getAbsolutePos(from).getX(), "sent without redstone");

        ctx.setBlockState(from.down(), Blocks.REDSTONE_BLOCK.getDefaultState());
        source.tick();
        BlockPos arrival = ctx.getAbsolutePos(to);
        ctx.assertTrue(Math.abs(pig.getX() - (arrival.getX() + 0.5)) < 1e-6 && Math.abs(pig.getZ() - (arrival.getZ() + 0.5)) < 1e-6
                && Math.abs(pig.getY() - (arrival.getY() + 1.5)) < 1e-6, "pig at " + pig.getPos());
        int cost = TileEntityTeleporter.getEnergyCost(100, 4.0);
        ctx.assertTrue(cost == 3171, "cost " + cost);
        ctx.assertTrue(batbox.getStored() == 10000 - cost, "batbox holds " + batbox.getStored());
        ctx.assertTrue(source.getActive(), "powered linked teleporter not active");
        ctx.complete();
    }

    /** Without enough energy in the storage blocks around, nothing is sent. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void teleporterNeedsEnergy(TestContext ctx) {
        BlockPos from = new BlockPos(1, 2, 1);
        BlockPos to = new BlockPos(1, 2, 5);
        ctx.setBlockState(from, IC2Blocks.TELEPORTER_BLOCK.getDefaultState());
        ctx.setBlockState(to, IC2Blocks.TELEPORTER_BLOCK.getDefaultState());
        ctx.setBlockState(from.down(), Blocks.REDSTONE_BLOCK.getDefaultState());
        ctx.setBlockState(from.west(), IC2Blocks.BATBOX_BLOCK.getDefaultState());
        TileEntityTeleporter source = ctx.getBlockEntity(from);
        TileEntityElectricBlock batbox = ctx.getBlockEntity(from.west());
        batbox.injectEnergy(Direction.UP, 3000, 32);
        source.setTarget(ctx.getAbsolutePos(to));

        PigEntity pig = ctx.spawnEntity(EntityType.PIG, from.up());
        source.tick();
        ctx.assertTrue(pig.getBlockPos().getZ() == ctx.getAbsolutePos(from).getZ(), "sent for 3000 EU");
        ctx.assertTrue(batbox.getStored() == 3000, "energy used " + batbox.getStored());
        ctx.complete();
    }

    /** Chilling freezes water, puts snow on the ice and piles the snow up. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void chillingBlueprintFreezes(TestContext ctx) {
        BlockPos water = new BlockPos(2, 1, 2);
        ctx.setBlockState(water.down(), Blocks.STONE.getDefaultState());
        ctx.setBlockState(water, Blocks.WATER.getDefaultState());
        ItemStack stack = new ItemStack(IC2ItemVariants.get("tfbp", "chilling"));
        ITerraformingBP tfbp = (ITerraformingBP) stack.getItem();
        // the terraformer stands three below, so the search from ten above it stays under the test area's barrier ceiling
        BlockPos column = ctx.getAbsolutePos(water).down(3);

        ctx.assertTrue(tfbp.terraform(stack, ctx.getWorld(), column), "water not changed");
        ctx.expectBlock(Blocks.ICE, water);
        ctx.assertTrue(tfbp.terraform(stack, ctx.getWorld(), column), "no snow on the ice");
        ctx.expectBlock(Blocks.SNOW, water.up());
        ctx.assertTrue(tfbp.terraform(stack, ctx.getWorld(), column), "snow not piled up");
        ctx.assertTrue(ctx.getBlockState(water.up()).get(SnowBlock.LAYERS) == 2, "layers " + ctx.getBlockState(water.up()));
        ctx.complete();
    }

    /** Desertification turns the dirt under grass into sand and leaves the grass on top. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void desertificationBlueprintMakesSand(TestContext ctx) {
        BlockPos ground = new BlockPos(1, 2, 1);
        ctx.setBlockState(ground.down(), Blocks.DIRT.getDefaultState());
        ctx.setBlockState(ground, Blocks.GRASS_BLOCK.getDefaultState());
        ItemStack stack = new ItemStack(IC2ItemVariants.get("tfbp", "desertification"));
        ITerraformingBP tfbp = (ITerraformingBP) stack.getItem();

        ctx.assertTrue(tfbp.terraform(stack, ctx.getWorld(), ctx.getAbsolutePos(ground).down(3)), "grass column unchanged");
        ctx.expectBlock(Blocks.SAND, ground.down());
        ctx.expectBlock(Blocks.GRASS_BLOCK, ground);
        ctx.complete();
    }

    /**
     * A terraformer with a desertification blueprint pays 2500 EU for a change and a tenth of
     * that for a miss; on the dirt of the test world most columns change. A right click takes the
     * blueprint out again. Its own batch, since it works on the land around.
     */
    @GameTest(templateName = EMPTY_STRUCTURE, batchId = "terraformer")
    public void terraformerDesertifies(TestContext ctx) {
        BlockPos pos = new BlockPos(2, 2, 2);
        ctx.setBlockState(pos, IC2Blocks.TERRAFORMER_BLOCK.getDefaultState());
        TileEntityTerraformer terraformer = ctx.getBlockEntity(pos);
        terraformer.injectEnergy(Direction.UP, 100000, 2048);
        terraformer.tick();
        ctx.assertTrue(terraformer.getEnergy() == 100000 && !terraformer.getActive(), "works without a blueprint");

        terraformer.insertBlueprint(new ItemStack(IC2ItemVariants.get("tfbp", "desertification")));
        for (int i = 0; i < 5; i++) {
            terraformer.tick();
        }
        double used = 100000 - terraformer.getEnergy();
        // every tick costs either 2500 or 250
        ctx.assertTrue(used >= 2500 && Math.round(used) % 250 == 0 && used <= 5 * 2500, "used " + used);
        ctx.assertTrue(terraformer.getActive(), "not active");

        terraformer.onActivated(ctx.createMockPlayer(net.minecraft.world.GameMode.CREATIVE), net.minecraft.util.Hand.MAIN_HAND);
        ctx.assertTrue(terraformer.tfbpSlot.isEmpty(), "blueprint not ejected");
        ctx.complete();
    }

    /**
     * Powered, the chunk loader forces its own chunk; a click on a map cell forces that chunk, a
     * second click lets it go. Breaking the loader releases everything it forced. The test
     * framework forces the chunks of the test area itself, so those stay forced.
     */
    @GameTest(templateName = EMPTY_STRUCTURE, batchId = "chunkloader")
    public void chunkLoaderForcesChunks(TestContext ctx) {
        BlockPos pos = new BlockPos(2, 2, 2);
        ctx.setBlockState(pos, IC2Blocks.CHUNK_LOADER_BLOCK.getDefaultState());
        TileEntityChunkloader loader = ctx.getBlockEntity(pos);
        loader.onLoad();
        ServerWorld world = ctx.getWorld();
        BlockPos abs = ctx.getAbsolutePos(pos);
        ChunkPos main = ChunkLoaderLogic.getChunkCoords(abs);
        var forced = world.getForcedChunks();

        loader.tick();
        ctx.assertTrue(!loader.getActive() && !ChunkLoaderLogic.isLoading(world, abs, main), "loading without energy");

        loader.injectEnergy(Direction.UP, 100, 32);
        loader.tick();
        ctx.assertTrue(loader.getActive() && ChunkLoaderLogic.isLoading(world, abs, main), "own chunk not forced");
        ctx.assertTrue(loader.getEnergy() == 99, "energy " + loader.getEnergy());
        ctx.assertTrue(loader.getGuiCondition("chunk_4_4") && !loader.getGuiCondition("chunk_5_4"), "map cells");

        // a map cell whose chunk nothing else forces
        int column = -1;
        int row = -1;
        for (int c = 0; c < 9 && column < 0; c++) {
            for (int r = 0; r < 9; r++) {
                if (!forced.contains(ChunkPos.toLong(main.x + c - 4, main.z + r - 4))) {
                    column = c;
                    row = r;
                    break;
                }
            }
        }
        ChunkPos other = new ChunkPos(main.x + column - 4, main.z + row - 4);
        String cell = "chunk:" + column + ":" + row;

        loader.onGuiEvent(null, cell);
        ctx.assertTrue(forced.contains(other.toLong()) && loader.getGuiCondition("chunk_" + column + "_" + row), other + " not forced");
        loader.tick();
        ctx.assertTrue(loader.getEnergy() == 97, "two chunks cost " + (99 - loader.getEnergy()));

        loader.onGuiEvent(null, cell);
        ctx.assertTrue(!forced.contains(other.toLong()), other + " still forced");
        loader.onGuiEvent(null, "chunk:4:4");
        ctx.assertTrue(ChunkLoaderLogic.isLoading(world, abs, main), "own chunk let go");

        loader.onGuiEvent(null, cell);
        ctx.assertTrue(forced.contains(other.toLong()), other + " not forced again");
        ctx.setBlockState(pos, Blocks.AIR.getDefaultState());
        ctx.assertTrue(!forced.contains(other.toLong()) && !ChunkLoaderLogic.isLoading(world, abs, main),
                "chunks still forced after the loader was broken");
        ctx.assertTrue(forced.contains(main.toLong()), "let go of the test area's own chunk");
        ctx.complete();
    }
}
