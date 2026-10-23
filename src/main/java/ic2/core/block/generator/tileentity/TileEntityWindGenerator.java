package ic2.core.block.generator.tileentity;

import ic2.core.IC2;
import ic2.core.WindSim;
import ic2.core.WorldData;
import ic2.core.init.IC2Blocks;
import ic2.core.init.MainConfig;
import ic2.core.ref.TeBlock;
import ic2.core.util.ConfigUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

/**
 * Fabric port of TileEntityWindGenerator - the windmill.
 *
 * <p>Every 128 ticks it reads the world's wind at its height ({@link WindSim}), takes away a
 * 567th for every block that is not air in the 9x7x9 around it (from two below to four above), and
 * makes a tenth of that as EU per tick ({@code balance/energy/generator/wind}). Above half the
 * maximum wind it overheats and the gauge in its window fills. 1.12.2 also meant it to tear
 * itself apart then (its block and up to four iron ingots), but the chance compared against the
 * output it had just reset to zero and so never came up; that is kept as it was.
 */
public class TileEntityWindGenerator extends TileEntityBaseRotorGenerator {
    private static final int TICK_RATE = 128;
    private static final double SAFE_WIND_RATIO = 0.5;

    private int ticker = IC2.random.nextInt(TICK_RATE);
    private int obstructedBlockCount;
    private double overheatRatio;

    public TileEntityWindGenerator(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.WIND_BE : type, pos, state, TeBlock.wind_generator, 4.0, 1, 32, 2);
    }

    public TileEntityWindGenerator(BlockPos pos, BlockState state) {
        this(IC2Blocks.WIND_BE, pos, state);
    }

    private static double windToEnergy() {
        return 0.1 * ConfigUtil.getDouble(MainConfig.get(), "balance/energy/generator/wind");
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (world != null && !world.isClient) {
            updateObscuratedBlockCount();
        }
    }

    @Override
    public boolean gainEnergy() {
        if (++ticker % TICK_RATE == 0) {
            if (ticker % 1024 == 0) {
                updateObscuratedBlockCount();
            }

            if (!updateProduction()) {
                return false;
            }
        }

        return super.gainEnergy();
    }

    /**
     * Reads the wind and sets the output for the next 128 ticks.
     *
     * @return false when the generator makes nothing, or broke apart
     */
    private boolean updateProduction() {
        production = 0.0;
        overheatRatio = 0.0;
        double windToEnergy = windToEnergy();
        if (windToEnergy <= 0.0) {
            return false;
        }

        WindSim windSim = WorldData.get(world).windSim;
        double wind = windSim.getWindAt(pos.getY()) * (1.0 - obstructedBlockCount / 567.0);
        if (wind <= 0.0) {
            return false;
        }

        double windRatio = wind / windSim.getMaxWind();
        overheatRatio = Math.max(0.0, (windRatio - SAFE_WIND_RATIO) / SAFE_WIND_RATIO);
        // production still holds 0 here, so the 1.12.2 check only ever fired once it had been
        // reset - it is kept as it was
        if (wind > windSim.getMaxWind() * SAFE_WIND_RATIO && world.random.nextInt(5000) <= production - 5.0) {
            breakApart();
            return false;
        }

        production = wind * windToEnergy;
        return true;
    }

    /** The generator is mined as with a diamond pickaxe and throws out up to four iron ingots. */
    private void breakApart() {
        BlockPos at = pos;
        if (world.breakBlock(at, true)) {
            for (int i = world.random.nextInt(5); i > 0; i--) {
                Block.dropStack(world, at, new ItemStack(Items.IRON_INGOT));
            }
        }
    }

    @Override
    public boolean gainFuel() {
        return false;
    }

    @Override
    public boolean needsFuel() {
        return false;
    }

    /** Counts the blocks that are not air in the 9x7x9 around the generator, itself excluded. */
    public void updateObscuratedBlockCount() {
        int count = -1;
        for (int x = -4; x < 5; x++) {
            for (int y = -2; y < 5; y++) {
                for (int z = -4; z < 5; z++) {
                    if (!world.getBlockState(pos.add(x, y, z)).isAir()) {
                        count++;
                    }
                }
            }
        }
        obstructedBlockCount = count;
    }

    public int getObstructions() {
        return obstructedBlockCount;
    }

    /** EU per tick the generator makes until the next wind check. */
    public double getProduction() {
        return production;
    }

    @Override
    public String getOperationSoundFile() {
        return "Generators/WindGenLoop.ogg";
    }

    @Override
    protected boolean delayActiveUpdate() {
        return true;
    }

    @Override
    protected boolean shouldRotorRotate() {
        return production > 0.0;
    }

    /** The overheat gauge: how far the wind is beyond the safe half of the maximum. */
    @Override
    public double getGuiValue(String name) {
        return "wind".equals(name) ? Math.max(overheatRatio, 0.0) : super.getGuiValue(name);
    }

    /** The gauge's background turns from grey to lit while the generator makes energy. */
    @Override
    public boolean getGuiCondition(String name) {
        return "active:wind".equals(name) ? production > 0.0 : super.getGuiCondition(name);
    }
}
