package ic2.core.gametest;

import ic2.core.WindSim;
import ic2.core.WorldData;
import ic2.core.block.generator.tileentity.TileEntityRTGenerator;
import ic2.core.block.generator.tileentity.TileEntitySolarGenerator;
import ic2.core.block.generator.tileentity.TileEntityWaterGenerator;
import ic2.core.block.generator.tileentity.TileEntityWindGenerator;
import ic2.core.init.IC2Blocks;
import ic2.core.init.IC2ItemVariants;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * The generators that run on the world around them - sun, wind and water - and the RTG.
 * Run with {@code ./gradlew runGametest}.
 */
public class GeneratorGameTest implements FabricGameTest {
    private static final BlockPos POS = new BlockPos(2, 2, 2);

    private static void tick(ic2.core.block.TileEntityBlock tile, int times) {
        for (int i = 0; i < times; i++) {
            tile.tick();
        }
    }

    /** Under a stone the panel makes nothing; in the open it makes its sky light, at most 2 EU. */
    @GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 100)
    public void solarGeneratorNeedsTheSky(TestContext ctx) {
        ctx.setBlockState(POS, IC2Blocks.SOLAR_BLOCK.getDefaultState());
        ctx.setBlockState(POS.up(), Blocks.STONE.getDefaultState());
        TileEntitySolarGenerator solar = ctx.getBlockEntity(POS);

        // the light engine catches up with the stone on the next ticks
        ctx.waitAndRun(5, () -> {
            solar.updateSunVisibility();
            ctx.assertTrue(solar.skyLight == 0.0F, "light under stone: " + solar.skyLight);
            tick(solar, 300);
            ctx.assertTrue(solar.getEnergy() == 0.0, "energy under stone: " + solar.getEnergy());
            ctx.assertTrue(!solar.getActive(), "active under stone");

            ctx.setBlockState(POS.up(), Blocks.AIR.getDefaultState());
            ctx.waitAndRun(5, () -> {
                solar.updateSunVisibility();
                float sky = TileEntitySolarGenerator.getSkyLight(ctx.getWorld(), ctx.getAbsolutePos(POS).up());
                ctx.assertTrue(solar.skyLight == sky, "light " + solar.skyLight + " instead of " + sky);
                ctx.assertTrue(sky > 0.0F && sky <= 1.0F, "no sky light in the open: " + sky);
                // the world ticks the panel too, and may already have filled it during the wait
                solar.drawEnergy(solar.getEnergy());
                solar.tick();
                ctx.assertTrue(Math.abs(solar.getEnergy() - Math.min(2.0, sky)) < 1e-6, "one tick gave " + solar.getEnergy());

                // the panel shows what it mostly did in the last 256 ticks; 600 hold a whole such window
                tick(solar, 600);
                ctx.assertTrue(solar.getEnergy() == 2.0, "buffer holds " + solar.getEnergy());
                ctx.assertTrue(solar.getActive(), "a panel in the sun does not show it");
                ctx.complete();
            });
        });
    }

    /** Every block in the 9x7x9 counts against the wind, which becomes a tenth of it in EU/t. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void windGeneratorReadsTheWindAndItsObstructions(TestContext ctx) {
        ctx.setBlockState(POS, IC2Blocks.WIND_BLOCK.getDefaultState());
        TileEntityWindGenerator wind = ctx.getBlockEntity(POS);
        wind.updateObscuratedBlockCount();
        int before = wind.getObstructions();

        ctx.setBlockState(POS.east(), Blocks.STONE.getDefaultState());
        ctx.setBlockState(POS.up(3), Blocks.STONE.getDefaultState());
        ctx.setBlockState(POS.north(2).west(2), Blocks.STONE.getDefaultState());
        wind.updateObscuratedBlockCount();
        ctx.assertTrue(wind.getObstructions() == before + 3, "obstructions " + wind.getObstructions() + " after " + before);

        // within 128 ticks the generator reads the wind once, all in this one game tick
        tick(wind, 128);
        WindSim sim = WorldData.get(ctx.getWorld()).windSim;
        double expected = sim.getWindAt(ctx.getAbsolutePos(POS).getY()) * (1.0 - wind.getObstructions() / 567.0);
        expected = expected <= 0.0 ? 0.0 : expected * 0.1;
        ctx.assertTrue(Math.abs(wind.getProduction() - expected) < 1e-9, "production " + wind.getProduction() + " instead of " + expected);
        ctx.complete();
    }

    /** Eight water blocks put 8 a tick in the store; the 13th tick has 104, which is 1 EU with 4 left. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void waterGeneratorRunsOnTheWaterAroundIt(TestContext ctx) {
        ctx.setBlockState(POS, IC2Blocks.WATER_BLOCK.getDefaultState());
        for (Direction dir : Direction.Type.HORIZONTAL) {
            ctx.setBlockState(POS.offset(dir), Blocks.WATER.getDefaultState());
            ctx.setBlockState(POS.offset(dir).offset(dir.rotateYClockwise()), Blocks.WATER.getDefaultState());
        }
        TileEntityWaterGenerator mill = ctx.getBlockEntity(POS);
        mill.updateWaterCount();
        ctx.assertTrue(mill.water == 8, "water " + mill.water);

        tick(mill, 12);
        ctx.assertTrue(mill.microStorage == 96 && mill.getEnergy() == 0.0, "store " + mill.microStorage + ", energy " + mill.getEnergy());
        mill.tick();
        ctx.assertTrue(mill.getEnergy() == 1.0, "energy " + mill.getEnergy());
        ctx.assertTrue(mill.microStorage == 4, "store " + mill.microStorage);
        ctx.complete();
    }

    /** A bucket of water is 500 ticks at 1 EU/t and leaves the empty bucket behind. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void waterGeneratorDrinksABucket(TestContext ctx) {
        ctx.setBlockState(POS, IC2Blocks.WATER_BLOCK.getDefaultState());
        TileEntityWaterGenerator mill = ctx.getBlockEntity(POS);
        mill.fuelSlot.put(new ItemStack(Items.WATER_BUCKET));
        mill.tick();
        ctx.assertTrue(mill.getFuel() == 499, "fuel " + mill.getFuel());
        ctx.assertTrue(mill.getEnergy() == 1.0, "energy " + mill.getEnergy());
        ctx.assertTrue(mill.fuelSlot.get().isOf(Items.BUCKET), "slot holds " + mill.fuelSlot.get());
        ctx.assertTrue(Math.abs(mill.getGuiValue("water") - 499.0 / 2000.0) < 1e-9, "gauge " + mill.getGuiValue("water"));
        ctx.complete();
    }

    /** Three pellets make 4 EU/t, six make 32. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void rtgDoublesWithEveryPellet(TestContext ctx) {
        ctx.setBlockState(POS, IC2Blocks.RT_BLOCK.getDefaultState());
        TileEntityRTGenerator rtg = ctx.getBlockEntity(POS);
        ItemStack pellet = new ItemStack(IC2ItemVariants.get("nuclear", "rtg_pellet"));
        rtg.tick();
        ctx.assertTrue(rtg.getEnergy() == 0.0, "energy without pellets " + rtg.getEnergy());

        for (int i = 0; i < 3; i++) {
            rtg.fuelSlot.put(i, pellet.copy());
        }
        rtg.tick();
        ctx.assertTrue(rtg.getEnergy() == 4.0, "three pellets gave " + rtg.getEnergy());

        for (int i = 3; i < 6; i++) {
            rtg.fuelSlot.put(i, pellet.copy());
        }
        rtg.tick();
        ctx.assertTrue(rtg.getEnergy() == 36.0, "six pellets gave " + (rtg.getEnergy() - 4.0));
        ctx.assertTrue(rtg.fuelSlot.get(0).getCount() == 1, "a pellet was used up");
        ctx.complete();
    }
}
