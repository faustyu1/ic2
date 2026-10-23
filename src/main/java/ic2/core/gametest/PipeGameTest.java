package ic2.core.gametest;

import ic2.core.block.machine.tileentity.TileEntityTank;
import ic2.core.block.transport.BlockPipe;
import ic2.core.block.transport.TileEntityFluidPipe;
import ic2.core.block.transport.items.PipeSize;
import ic2.core.block.transport.items.PipeType;
import ic2.core.init.IC2Blocks;
import ic2.core.util.LiquidUtil;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.registry.Registries;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

/**
 * The fluid pipes, their covers and the tools for them in a running world.
 * Run with {@code ./gradlew runGametest}.
 */
public class PipeGameTest implements FabricGameTest {
    private static final BlockPos POS = new BlockPos(2, 2, 2);

    private static FluidStack water(int amount) {
        return new FluidStack(FluidRegistry.getFluid("water"), amount);
    }

    private static Block pipeBlock(PipeType type, PipeSize size) {
        return Registries.BLOCK.get(Identifier.of("ic2", type.getName(size)));
    }

    /** Puts a pipe connected on the given sides. */
    private static TileEntityFluidPipe pipe(TestContext ctx, BlockPos at, PipeType type, PipeSize size, Direction... connected) {
        int mask = 0;
        for (Direction side : connected) {
            mask |= 1 << side.ordinal();
        }
        ctx.setBlockState(at, BlockPipe.withPipeState(pipeBlock(type, size).getDefaultState(), mask, 0));
        return ctx.getBlockEntity(at);
    }

    private static TileEntityTank tank(TestContext ctx, BlockPos at) {
        ctx.setBlockState(at, IC2Blocks.TANK_BLOCK.getDefaultState());
        return ctx.getBlockEntity(at);
    }

    private static ItemUsageContext click(TestContext ctx, PlayerEntity player, Item item, BlockPos at, Direction face) {
        ItemStack stack = new ItemStack(item);
        player.setStackInHand(Hand.MAIN_HAND, stack);
        BlockPos abs = ctx.getAbsolutePos(at);
        // the middle of the face, which picks that face itself
        Vec3d hit = Vec3d.ofCenter(abs).add(face.getOffsetX() * 0.5, face.getOffsetY() * 0.5, face.getOffsetZ() * 0.5);
        return new ItemUsageContext(player, Hand.MAIN_HAND, new BlockHitResult(hit, face, abs, false));
    }

    /** A pipe holds what it moves in a second: transferRate * size multiplier, 400 to 9600 mB. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void pipeCapacities(TestContext ctx) {
        int[][] expected = {{400, 800, 2400, 4800}, {800, 1600, 4800, 9600}};
        int[][] perTick = {{20, 40, 120, 240}, {40, 80, 240, 480}};
        for (PipeType type : PipeType.values()) {
            for (PipeSize size : PipeSize.values()) {
                int want = expected[type.ordinal()][size.ordinal()];
                ctx.assertTrue(TileEntityFluidPipe.getCapacity(type, size) == want, type.getName(size) + " capacity");
                ctx.assertTrue(TileEntityFluidPipe.getTransferPerTick(type, size) == perTick[type.ordinal()][size.ordinal()],
                        type.getName(size) + " per tick");
            }
        }
        TileEntityFluidPipe pipe = pipe(ctx, POS, PipeType.steel, PipeSize.medium);
        ctx.assertTrue(pipe.getMaxInnerCapacity() == 4800, "placed capacity " + pipe.getMaxInnerCapacity());
        ctx.assertTrue(pipe.getPipeType() == PipeType.steel && pipe.getPipeSize() == PipeSize.medium, "variant");
        ctx.complete();
    }

    /** A bronze small pipe hands 40 mB a tick to the tank it is connected to, nothing to an unconnected one. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void pipeFeedsConnectedTank(TestContext ctx) {
        TileEntityFluidPipe pipe = pipe(ctx, POS, PipeType.bronze, PipeSize.small, Direction.NORTH);
        TileEntityTank north = tank(ctx, POS.north());
        TileEntityTank south = tank(ctx, POS.south());
        ctx.assertTrue(LiquidUtil.fillTile(pipe, Direction.NORTH, water(1000), false) == 800, "pipe takes its capacity");
        ctx.assertTrue(LiquidUtil.fillTile(pipe, Direction.SOUTH, water(100), true) == 0, "unconnected side takes fluid");

        pipe.tick();
        ctx.assertTrue(north.getTank().getFluidAmount() == 40, "north " + north.getTank().getFluidAmount());
        ctx.assertTrue(south.getTank().getFluidAmount() == 0, "south got fluid");
        ctx.assertTrue(pipe.getCurrentInnerCapacity() == 760, "pipe " + pipe.getCurrentInnerCapacity());
        for (int i = 0; i < 19; i++) {
            pipe.tick();
        }
        ctx.assertTrue(north.getTank().getFluidAmount() == 800 && pipe.getCurrentInnerCapacity() == 0,
                "a second moved " + north.getTank().getFluidAmount());
        ctx.complete();
    }

    /** Joined pipes even out at once, the odd mB going to the emptier one; then the outputs share the rate. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void pipesEvenOut(TestContext ctx) {
        TileEntityFluidPipe a = pipe(ctx, POS, PipeType.bronze, PipeSize.small, Direction.EAST);
        TileEntityFluidPipe b = pipe(ctx, POS.east(), PipeType.bronze, PipeSize.small, Direction.WEST, Direction.NORTH, Direction.SOUTH);
        TileEntityTank north = tank(ctx, POS.east().north());
        TileEntityTank south = tank(ctx, POS.east().south());
        LiquidUtil.fillTile(a, Direction.EAST, water(701), false);

        a.tick();
        ctx.assertTrue(a.getCurrentInnerCapacity() == 350 && b.getCurrentInnerCapacity() == 351,
                "split " + a.getCurrentInnerCapacity() + "/" + b.getCurrentInnerCapacity());

        // b holds more than a, so only the two tanks get from it: 40 mB shared, 20 each
        b.tick();
        ctx.assertTrue(north.getTank().getFluidAmount() == 20 && south.getTank().getFluidAmount() == 20,
                "tanks " + north.getTank().getFluidAmount() + "/" + south.getTank().getFluidAmount());
        ctx.assertTrue(a.getCurrentInnerCapacity() == 350 && b.getCurrentInnerCapacity() == 311,
                "after " + a.getCurrentInnerCapacity() + "/" + b.getCurrentInnerCapacity());
        ctx.complete();
    }

    /**
     * An LV pump cover pulls 32 mB a tick out of the tank beside it and lets nothing back out; the
     * crowbar takes it off again as an item.
     */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void pumpCoverPullsAndCrowbarRemovesIt(TestContext ctx) {
        TileEntityFluidPipe pipe = pipe(ctx, POS, PipeType.bronze, PipeSize.small, Direction.NORTH);
        TileEntityTank source = tank(ctx, POS.north());
        LiquidUtil.fillTile(source, Direction.SOUTH, water(1000), false);

        PlayerEntity player = ctx.createMockPlayer(net.minecraft.world.GameMode.SURVIVAL);
        Item cover = Registries.ITEM.get(Identifier.of("ic2", "cover_pump_lv"));
        cover.useOnBlock(click(ctx, player, cover, POS, Direction.NORTH));
        ctx.assertTrue(pipe.hasCover(Direction.NORTH), "cover not placed");
        ctx.assertTrue(ctx.getBlockState(POS).get(BlockPipe.COVERS.get(Direction.NORTH)), "cover not in the block state");

        pipe.tick();
        ctx.assertTrue(pipe.getCurrentInnerCapacity() == 32, "pumped " + pipe.getCurrentInnerCapacity());
        ctx.assertTrue(source.getTank().getFluidAmount() == 968, "source " + source.getTank().getFluidAmount());
        pipe.tick();
        ctx.assertTrue(pipe.getCurrentInnerCapacity() == 64, "the cover let fluid back out: " + pipe.getCurrentInnerCapacity());

        Item crowbar = Registries.ITEM.get(Identifier.of("ic2", "crowbar"));
        ItemUsageContext use = click(ctx, player, crowbar, POS, Direction.NORTH);
        crowbar.useOnBlock(use);
        ctx.assertTrue(!pipe.hasCover(Direction.NORTH), "cover not removed");
        ctx.assertTrue(!ctx.getBlockState(POS).get(BlockPipe.COVERS.get(Direction.NORTH)), "cover still drawn");
        ctx.assertTrue(use.getStack().getDamage() == 1, "crowbar damage " + use.getStack().getDamage());
        List<ItemEntity> drops = ctx.getWorld().getEntitiesByClass(ItemEntity.class,
                new Box(ctx.getAbsolutePos(POS)).expand(2), e -> e.getStack().isOf(cover));
        ctx.assertTrue(drops.size() == 1, "dropped covers " + drops.size());

        // without the cover the pipe pushes its 64 mB straight back
        pipe.tick();
        ctx.assertTrue(pipe.getCurrentInnerCapacity() == 24, "pipe " + pipe.getCurrentInnerCapacity());
        ctx.complete();
    }

    /** The new wrench connects two pipes on both ends at once, and disconnects them again. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void wrenchJoinsPipes(TestContext ctx) {
        TileEntityFluidPipe a = pipe(ctx, POS, PipeType.steel, PipeSize.tiny);
        TileEntityFluidPipe b = pipe(ctx, POS.east(), PipeType.steel, PipeSize.tiny);
        PlayerEntity player = ctx.createMockPlayer(net.minecraft.world.GameMode.SURVIVAL);
        Item wrench = Registries.ITEM.get(Identifier.of("ic2", "wrench_new"));

        wrench.useOnBlock(click(ctx, player, wrench, POS, Direction.EAST));
        ctx.assertTrue(a.isConnected(Direction.EAST) && b.isConnected(Direction.WEST), "not joined");
        BlockState state = ctx.getBlockState(POS);
        ctx.assertTrue(state.get(BlockPipe.CONNECTIONS.get(Direction.EAST)), "arm not drawn");
        ctx.assertTrue(ctx.getBlockState(POS.east()).get(BlockPipe.CONNECTIONS.get(Direction.WEST)), "other arm not drawn");

        LiquidUtil.fillTile(a, Direction.EAST, water(800), false);
        a.tick();
        ctx.assertTrue(a.getCurrentInnerCapacity() == 400 && b.getCurrentInnerCapacity() == 400, "not shared");

        wrench.useOnBlock(click(ctx, player, wrench, POS.east(), Direction.WEST));
        ctx.assertTrue(!a.isConnected(Direction.EAST) && !b.isConnected(Direction.WEST), "not parted");
        ctx.complete();
    }

    /** A pipe broken with more than a bucket in it leaves a source block of the fluid behind. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void brokenPipeSpillsItsFluid(TestContext ctx) {
        TileEntityFluidPipe pipe = pipe(ctx, POS, PipeType.steel, PipeSize.large);
        pipe.getTank().fill(water(2000), true);
        ctx.getWorld().breakBlock(ctx.getAbsolutePos(POS), false);
        ctx.waitAndRun(2, () -> {
            ctx.assertTrue(ctx.getBlockState(POS).isOf(Blocks.WATER), "no water: " + ctx.getBlockState(POS));
            ctx.complete();
        });
    }
}
