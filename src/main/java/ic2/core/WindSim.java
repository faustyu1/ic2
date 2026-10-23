package ic2.core;

import net.minecraft.world.World;
import org.ejml.simple.SimpleMatrix;

/**
 * Fabric port of ic2.core.WindSim - the wind a world has.
 *
 * <p>The base strength drifts between about 10 and 20 every 128 ticks. How much of it reaches a
 * height is a cubic through three conditions solved once per world, exactly as 1.12.2 did: full
 * strength halfway between sea level and the top of the world, nothing at an eighth above the top,
 * and the peak flat. Rain adds a quarter, a thunderstorm a half.
 */
public class WindSim {
    private int windStrength = 5 + IC2.random.nextInt(20);
    private int windDirection = IC2.random.nextInt(360);
    public int windTicker;
    private final World world;
    private final SimpleMatrix windHeightCoefficients;

    public WindSim(World world) {
        this.world = world;
        this.windHeightCoefficients = calculateCoefficients(world.getTopY(), world.getSeaLevel());
    }

    static SimpleMatrix calculateCoefficients(int height, int seaLevel) {
        height = Math.max(1, height);
        seaLevel = Math.max(0, seaLevel);
        double baseHeight = seaLevel < height ? seaLevel : height * 0.5;

        double sh = baseHeight + (height - baseHeight) / 2.0;
        double fh = height * 1.125;
        SimpleMatrix a = new SimpleMatrix(3, 3);
        SimpleMatrix b = new SimpleMatrix(3, 1);
        a.setRow(0, 0, sh, sh * sh, sh * sh * sh);
        b.set(0, 1.0);
        a.setRow(1, 0, fh, fh * fh, fh * fh * fh);
        b.set(1, 0.0);
        a.setRow(2, 0, 1.0, 2.0 * sh, 3.0 * sh * sh);
        b.set(2, 0.0);
        return a.solve(b);
    }

    public void updateWind() {
        if (windTicker++ % 128 != 0) {
            return;
        }

        int upChance = 10;
        int downChance = 10;
        if (windStrength > 20) {
            upChance -= windStrength - 20;
        } else if (windStrength < 10) {
            downChance -= 10 - windStrength;
        }

        if (IC2.random.nextInt(100) < upChance) {
            windStrength++;
        } else if (IC2.random.nextInt(100) < downChance) {
            windStrength--;
        }

        switch (IC2.random.nextInt(3)) {
            case 0 -> windDirection = changeWindDirection(-18);
            case 2 -> windDirection = changeWindDirection(18);
            default -> { }
        }
    }

    /** Wind strength at a height, in the "MCW" the windmeter and the rotors speak of. */
    public double getWindAt(double height) {
        return windAt(windStrength, heightMultiplier(windHeightCoefficients, height),
                world.isThundering(), world.isRaining());
    }

    static double heightMultiplier(SimpleMatrix coefficients, double height) {
        SimpleMatrix x = new SimpleMatrix(1, 3);
        x.setRow(0, 0, height, height * height, height * height * height);
        return Math.max(0.0, x.mult(coefficients).get(0));
    }

    static double windAt(int strength, double heightMultiplier, boolean thunder, boolean rain) {
        double ret = strength * heightMultiplier;
        if (thunder) {
            ret *= 1.5;
        } else if (rain) {
            ret *= 1.25;
        }
        return ret * 2.4;
    }

    public double getMaxWind() {
        return 108.0;
    }

    public int getWindStrength() {
        return windStrength;
    }

    private int changeWindDirection(int amount) {
        windDirection += amount;
        if (windDirection < 0) {
            return 359 - windDirection;
        }
        return windDirection > 359 ? windDirection - 359 : windDirection;
    }
}
