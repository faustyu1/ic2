package ic2.core.block.kineticgenerator.tileentity;

import ic2.api.item.IKineticRotor;
import ic2.core.WorldData;
import ic2.core.init.IC2Blocks;
import ic2.core.init.MainConfig;
import ic2.core.ref.TeBlock;
import ic2.core.util.ConfigUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

/**
 * Fabric port of TileEntityWindKineticGenerator - a wind turbine.
 *
 * <p>Every 32 ticks it measures the wind at its height and how much of the area in front of the
 * rotor is blocked; a blocked area weakens the wind by the square of the blocked share, another
 * turbine in it stops the rotor. Between the rotor's minimum and maximum wind it makes
 * {@code wind * 10 * efficiency} kU a tick and wears the rotor by one; above the maximum it wears
 * four times as fast.
 */
public class TileEntityWindKineticGenerator extends TileEntityRotorKineticGenerator {
    private double windStrength;
    private int obstructedCrossSection;
    private int crossSection;

    public TileEntityWindKineticGenerator(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.WIND_KINETIC_BE : type, pos, state, TeBlock.wind_kinetic_generator, IKineticRotor.GearboxType.WIND);
    }

    public TileEntityWindKineticGenerator(BlockPos pos, BlockState state) {
        this(IC2Blocks.WIND_KINETIC_BE, pos, state);
    }

    public static float outputModifier() {
        return 10.0F * (float) ConfigUtil.getDouble(MainConfig.get(), "balance/energy/kineticgenerator/wind");
    }

    @Override
    protected int getTickRate() {
        return 32;
    }

    @Override
    public void tick() {
        if (world == null || world.isClient || updateTicker++ % getTickRate() != 0) {
            return;
        }

        boolean active = hasRotor() && rotorHasSpace();
        setActive(active);

        if (!active) {
            rotationSpeed = 0.0F;
            return;
        }

        crossSection = square(getRotorDiameter() / 2 * 2 * 2 + 1);
        obstructedCrossSection = checkSpace(getRotorDiameter() * 3, false);
        if (obstructedCrossSection > 0 && obstructedCrossSection <= (getRotorDiameter() + 1) / 2) {
            obstructedCrossSection = 0;
        }

        if (obstructedCrossSection < 0) {
            windStrength = 0.0;
            rotationSpeed = 0.0F;
            return;
        }

        windStrength = calcWindStrength();
        rotationSpeed = (float) Math.max(0.0, Math.min(2.0, (windStrength - getMinWindStrength()) / getMaxWindStrength()));
        if (windStrength >= getMinWindStrength()) {
            rotorSlot.damage(windStrength <= getMaxWindStrength() ? 1 : 4, false);
            markDirty();
        }
    }

    private static int square(int x) {
        return x * x;
    }

    public double calcWindStrength() {
        WorldData data = WorldData.get(world);
        double wind = data == null || data.windSim == null ? 0.0 : data.windSim.getWindAt(pos.getY());
        wind *= 1.0 - Math.pow((double) obstructedCrossSection / crossSection, 2.0);
        return Math.max(0.0, wind);
    }

    @Override
    protected boolean blocksRotor(BlockPos at) {
        return !world.getBlockState(at).isAir();
    }

    public int getMinWindStrength() {
        IKineticRotor rotor = rotor();
        return rotor == null ? 0 : rotor.getMinWindStrength(rotorSlot.get());
    }

    public int getMaxWindStrength() {
        IKineticRotor rotor = rotor();
        return rotor == null ? 0 : rotor.getMaxWindStrength(rotorSlot.get());
    }

    public boolean isWindStrongEnough() {
        return windStrength >= getMinWindStrength();
    }

    public boolean isRotorOverloaded() {
        return hasRotor() && rotorHasSpace() && isWindStrongEnough() && windStrength > getMaxWindStrength();
    }

    @Override
    public int getKuOutput() {
        return windStrength >= getMinWindStrength() && getActive()
                ? (int) (windStrength * outputModifier() * getEfficiency())
                : 0;
    }

    public int getWindStrength() {
        return (int) windStrength;
    }

    public int getObstructions() {
        return obstructedCrossSection;
    }

    @Override
    public boolean getGuiCondition(String name) {
        return "windStrong".equals(name) ? isWindStrongEnough() : super.getGuiCondition(name);
    }
}
