package ic2.core.gametest;

import ic2.core.block.BlockTileEntity;
import ic2.core.block.machine.tileentity.TileEntityStandardMachine;
import ic2.core.block.reactor.tileentity.TileEntityNuclearReactorElectric;
import ic2.core.block.reactor.tileentity.TileEntityRCI_LZH;
import ic2.core.block.reactor.tileentity.TileEntityReactorRedstonePort;
import ic2.core.block.wiring.TileEntityCableDetector;
import ic2.core.block.wiring.TileEntityCableSplitter;
import ic2.core.block.wiring.TileEntityElectricBlock;
import ic2.core.block.wiring.TileEntityTransformer;
import ic2.core.init.IC2Blocks;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Transformers, the detector and splitter cables, and the reactor's redstone and coolant
 * injector blocks in a running world. Run with {@code ./gradlew runGametest}.
 */
public class WiringGameTest implements FabricGameTest {
    private static final BlockPos POS = new BlockPos(2, 2, 2);

    private static Block cable(String name) {
        return Registries.BLOCK.get(Identifier.of("ic2", name));
    }

    /**
     * Unpowered an LV transformer steps down: 128 EU/t in through the face, four 32 EU packets out
     * of the other sides. Powered it steps up: one 128 EU packet out of the face. The window's
     * buttons fix either mode.
     */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void transformerStepsDownAndUp(TestContext ctx) {
        ctx.setBlockState(POS, IC2Blocks.LV_TRANSFORMER_BLOCK.getDefaultState());
        TileEntityTransformer transformer = ctx.getBlockEntity(POS);
        transformer.setFacing(Direction.EAST);
        transformer.ensureLoaded();
        transformer.tick();

        ctx.assertTrue(!transformer.isStepUp() && !transformer.getActive(), "unpowered transformer steps up");
        ctx.assertTrue(transformer.getSinkTier() == 2 && transformer.getSourceTier() == 1,
                "tiers " + transformer.getSinkTier() + "/" + transformer.getSourceTier());
        ctx.assertTrue(transformer.acceptsEnergyFrom(null, Direction.EAST) && !transformer.acceptsEnergyFrom(null, Direction.WEST),
                "stepping down it must take energy through its face only");
        ctx.assertTrue(transformer.emitsEnergyTo(null, Direction.WEST) && !transformer.emitsEnergyTo(null, Direction.EAST),
                "stepping down it must give energy out of the other sides");
        ctx.assertTrue(transformer.getInputFlow() == 128 && transformer.getOutputFlow() == 32,
                "flow " + transformer.getInputFlow() + " -> " + transformer.getOutputFlow());
        ctx.assertTrue(transformer.getDemandedEnergy() == 256, "buffer of eight LV packets: " + transformer.getDemandedEnergy());

        // only whole packets leave
        transformer.injectEnergy(Direction.EAST, 20, 128);
        ctx.assertTrue(transformer.getOfferedEnergy() == 0 && transformer.getMultipleEnergyPacketAmount() == 0, "a part packet is offered");
        transformer.injectEnergy(Direction.EAST, 236, 128);
        ctx.assertTrue(transformer.getOfferedEnergy() == 256 && transformer.getMultipleEnergyPacketAmount() == 4,
                "offered " + transformer.getOfferedEnergy() + " in " + transformer.getMultipleEnergyPacketAmount());

        ctx.setBlockState(POS.up(), Blocks.REDSTONE_BLOCK.getDefaultState());
        transformer.tick();
        ctx.assertTrue(transformer.isStepUp() && transformer.getActive(), "powered transformer does not step up");
        ctx.assertTrue(transformer.getSinkTier() == 1 && transformer.getSourceTier() == 2, "tiers stepping up");
        ctx.assertTrue(transformer.emitsEnergyTo(null, Direction.EAST) && !transformer.emitsEnergyTo(null, Direction.WEST), "output side stepping up");
        ctx.assertTrue(transformer.getMultipleEnergyPacketAmount() == 1, "packets " + transformer.getMultipleEnergyPacketAmount());
        // the 1.12.2 window shows the high voltage as input either way
        ctx.assertTrue(transformer.getInputFlow() == 128 && transformer.getOutputFlow() == 32, "flow stepping up");

        // "fixed step-down" overrides the redstone, "fixed step-up" works without it
        transformer.onGuiEvent(null, "mode1");
        ctx.assertTrue(transformer.getMode() == TileEntityTransformer.Mode.stepdown && !transformer.isStepUp(), "fixed step-down ignored");
        ctx.setBlockState(POS.up(), Blocks.AIR.getDefaultState());
        transformer.onGuiEvent(null, "mode2");
        transformer.tick();
        ctx.assertTrue(transformer.isStepUp() && transformer.getActive(), "fixed step-up ignored");
        ctx.assertTrue(!transformer.getGuiItem("mode2").isEmpty() && transformer.getGuiItem("mode0").isEmpty(), "wrench marks the wrong mode");
        ctx.complete();
    }

    /** A BatBox filling another through a detector cable: lit, and a full redstone signal. */
    @GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 200)
    public void detectorCableSignalsEnergyFlow(TestContext ctx) {
        ctx.setBlockState(POS.west(), IC2Blocks.BATBOX_BLOCK.getDefaultState());
        ctx.setBlockState(POS, cable("detector_cable").getDefaultState());
        ctx.setBlockState(POS.east(), IC2Blocks.BATBOX_BLOCK.getDefaultState());
        TileEntityElectricBlock source = ctx.getBlockEntity(POS.west());
        TileEntityElectricBlock sink = ctx.getBlockEntity(POS.east());
        source.setFacing(Direction.EAST);
        // the receiving BatBox faces away, so it takes the energy on its back
        sink.setFacing(Direction.EAST);
        source.injectEnergy(Direction.UP, 40000, 32);
        ctx.assertTrue(ctx.getBlockEntity(POS) instanceof TileEntityCableDetector, "the detector cable has a plain cable entity");
        TileEntityCableDetector detector = ctx.getBlockEntity(POS);

        ctx.waitAndRun(100, () -> {
            ctx.assertTrue(sink.getStored() > 0, "no energy went through the detector");
            ctx.assertTrue(detector.getRedstoneOutput() == 15, "signal " + detector.getRedstoneOutput());
            ctx.assertTrue(ctx.getBlockState(POS).get(BlockTileEntity.ACTIVE), "detector not lit");
            ctx.assertTrue(ctx.getWorld().getEmittedRedstonePower(ctx.getAbsolutePos(POS), Direction.UP) == 15, "the world sees no signal");
            // 32 EU/t of the cable's 8192 is less than a comparator step
            ctx.assertTrue(detector.getComparatorOutput() == 0, "comparator " + detector.getComparatorOutput());
            ctx.complete();
        });
    }

    /** A powered splitter cable cuts the line; without the signal it joins it again. */
    @GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 300)
    public void splitterCableCutsTheLineWhilePowered(TestContext ctx) {
        ctx.setBlockState(POS.west(), IC2Blocks.BATBOX_BLOCK.getDefaultState());
        ctx.setBlockState(POS, cable("splitter_cable").getDefaultState());
        ctx.setBlockState(POS.east(), IC2Blocks.MACERATOR_BLOCK.getDefaultState());
        ctx.setBlockState(POS.up(), Blocks.REDSTONE_BLOCK.getDefaultState());
        TileEntityElectricBlock batbox = ctx.getBlockEntity(POS.west());
        batbox.setFacing(Direction.EAST);
        batbox.injectEnergy(Direction.UP, 40000, 32);
        TileEntityCableSplitter splitter = ctx.getBlockEntity(POS);
        TileEntityStandardMachine<?, ?, ?> macerator = ctx.getBlockEntity(POS.east());

        ctx.waitAndRun(60, () -> {
            ctx.assertTrue(!splitter.isAddedToEnet(), "a powered splitter stays in the net");
            ctx.assertTrue(!ctx.getBlockState(POS).get(BlockTileEntity.ACTIVE), "a cut splitter is lit");
            ctx.assertTrue(macerator.getEnergy() == 0, "energy went through a powered splitter: " + macerator.getEnergy());

            ctx.setBlockState(POS.up(), Blocks.AIR.getDefaultState());
            ctx.waitAndRun(100, () -> {
                ctx.assertTrue(splitter.isAddedToEnet(), "the splitter did not rejoin the net");
                ctx.assertTrue(ctx.getBlockState(POS).get(BlockTileEntity.ACTIVE), "a joined splitter is not lit");
                ctx.assertTrue(macerator.getEnergy() > 0, "no energy once the signal is gone");
                ctx.complete();
            });
        });
    }

    /** A redstone signal on a reactor chamber switches the reactor on, as one on the reactor would. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void reactorChamberPassesRedstoneOn(TestContext ctx) {
        ctx.setBlockState(POS, IC2Blocks.NUCLEAR_REACTOR_BLOCK.getDefaultState());
        ctx.setBlockState(POS.east(), IC2Blocks.REACTOR_CHAMBER_BLOCK.getDefaultState());
        TileEntityNuclearReactorElectric reactor = ctx.getBlockEntity(POS);
        ctx.assertTrue(!reactor.hasRedstoneInput(), "reactor powered by nothing");

        ctx.setBlockState(POS.east(2), Blocks.REDSTONE_BLOCK.getDefaultState());
        ctx.assertTrue(!ctx.getWorld().isReceivingRedstonePower(ctx.getAbsolutePos(POS)), "the reactor itself is powered");
        ctx.assertTrue(reactor.hasRedstoneInput() && reactor.produceEnergy(), "a powered chamber does not switch the reactor on");
        ctx.complete();
    }

    /** In a fluid reactor's 5x5x5 hull a powered redstone port switches the reactor on. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void redstonePortSwitchesTheFluidReactor(TestContext ctx) {
        BlockPos core = new BlockPos(3, 3, 3);
        BlockPos port = core.east(2);
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    if (Math.abs(x) == 2 || Math.abs(y) == 2 || Math.abs(z) == 2) {
                        ctx.setBlockState(core.add(x, y, z), IC2Blocks.REACTOR_VESSEL_BLOCK.getDefaultState());
                    }
                }
            }
        }
        ctx.setBlockState(port, IC2Blocks.REACTOR_REDSTONE_PORT_BLOCK.getDefaultState());
        ctx.setBlockState(core, IC2Blocks.NUCLEAR_REACTOR_BLOCK.getDefaultState());
        for (Direction dir : Direction.values()) {
            ctx.setBlockState(core.offset(dir), IC2Blocks.REACTOR_CHAMBER_BLOCK.getDefaultState());
        }

        TileEntityNuclearReactorElectric reactor = ctx.getBlockEntity(core);
        reactor.tick();
        ctx.assertTrue(reactor.isFluidCooled(), "the hull does not make a fluid reactor");
        TileEntityReactorRedstonePort redstonePort = ctx.getBlockEntity(port);
        ctx.assertTrue(redstonePort.getReactorInstance() == reactor, "the port does not find its reactor");
        ctx.assertTrue(!reactor.hasRedstoneInput(), "reactor powered by nothing");

        ctx.setBlockState(port.east(), Blocks.REDSTONE_BLOCK.getDefaultState());
        ctx.assertTrue(redstonePort.hasRedstone(), "port not powered");
        ctx.assertTrue(reactor.hasRedstoneInput(), "the powered port does not reach the reactor");
        ctx.complete();
    }

    /** The coolant injector takes no energy from the side it shares with the reactor. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void coolantInjectorTakesNoEnergyFromTheReactorSide(TestContext ctx) {
        ctx.setBlockState(POS.west(), IC2Blocks.NUCLEAR_REACTOR_BLOCK.getDefaultState());
        ctx.setBlockState(POS, IC2Blocks.RCI_LZH_BLOCK.getDefaultState());
        TileEntityRCI_LZH rci = ctx.getBlockEntity(POS);
        rci.setFacing(Direction.EAST);
        rci.updateEnergyFacings();
        ctx.assertTrue(!rci.acceptsEnergyFrom(null, Direction.WEST), "takes energy from the reactor side");
        ctx.assertTrue(rci.acceptsEnergyFrom(null, Direction.EAST) && rci.acceptsEnergyFrom(null, Direction.UP), "refuses energy on a free side");
        ctx.complete();
    }
}
